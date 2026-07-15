package com.charlie.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Redis 客户端，使用 Redisson <a href="https://github.com/redisson/redisson">Redisson</a>
 *
 * @author Charlie
 */
@Configuration
@EnableConfigurationProperties(RedisClientConfigProperties.class)
public class RedisClientConfig {

    @Bean("redissonClient")
    public RedissonClient redissonClient(ConfigurableApplicationContext applicationContext, RedisClientConfigProperties properties) {
        Config config = new Config();
        // ===== 设置 Redisson 的编解码器（Codec）=====
        // 作用：Redis 只能存储字节数组（byte[]），Codec 负责 Java 对象与字节流之间的双向转换。
        //       写入时：encoder 把 Java 对象序列化为字节流存入 Redis；
        //       读取时：decoder 把 Redis 返回的字节流反序列化为 Java 对象。
        //
        // JsonJacksonCodec：Redisson 内置的 JSON 编解码器，底层基于 Jackson（ObjectMapper）。
        //   - 序列化结果是人类可读的 JSON 字符串，便于通过 redis-cli 直接排查问题；
        //   - 存储体积比 JDK 二进制序列化小，跨语言兼容性好；
        //   - INSTANCE：单例实例，全局共享一份 ObjectMapper，避免重复创建开销。
        //
        // 显式 setCodec 的意义：
        //   1. Redisson 默认就是 JsonJacksonCodec，这里显式设置是为了语义清晰、防止未来默认值变更带来隐患；
        //   2. 与本类下方的自定义 RedisCodec（基于 FastJSON，带 WriteClassName）形成对照——
        //      当需要存储子类类型信息（多态反序列化）时才会切换到 FastJSON 方案，当前装配策略数据用 Jackson 足够。
        //
        // 参考文档：https://github.com/redisson/redisson/wiki/4.-数据序列化
        config.setCodec(JsonJacksonCodec.INSTANCE);
        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
//                .setPassword(properties.getPassword())
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive())
        ;

        return Redisson.create(config);
    }

    static class RedisCodec extends BaseCodec {

        private final Encoder encoder = in -> {
            ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
            try {
                ByteBufOutputStream os = new ByteBufOutputStream(out);
                JSON.writeJSONString(os, in, SerializerFeature.WriteClassName);
                return os.buffer();
            } catch (IOException e) {
                out.release();
                throw e;
            } catch (Exception e) {
                out.release();
                throw new IOException(e);
            }
        };

        private final Decoder<Object> decoder = (buf, state) -> JSON.parseObject(new ByteBufInputStream(buf), Object.class);

        @Override
        public Decoder<Object> getValueDecoder() {
            return decoder;
        }

        @Override
        public Encoder getValueEncoder() {
            return encoder;
        }

    }

}
