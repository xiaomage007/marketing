package com.charlie.trigger.http;

import com.alibaba.nacos.api.config.ConfigService;
import com.charlie.api.IDCCService;
import com.charlie.api.response.Response;
import com.charlie.types.common.Constants;
import com.charlie.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 动态配置管理。生产上建议直接通过 Nacos 控制台修改(自带发布历史、回滚、权限)，
 * 本接口作为无控制台权限场景下的兜底工具
 * @author: Charlie
 * @date: 2026/9/28 14:04
 */
@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/dcc/")
public class DCCController implements IDCCService {

    @Resource
    private ConfigService configService;

    /**
     * 更新配置
     * <p>
     * curl --request GET --url 'http://localhost:8091/api/v1/raffle/dcc/update_config?key=degradeSwitch&value=close'
     */
    @RequestMapping(value = "update_config", method = RequestMethod.GET)
    @Override
    public Response<Boolean> updateConfig(@RequestParam String key, @RequestParam String value) {
        try {
            log.info("DCC 动态配置值变更开始 key:{} value:{}", key, value);
            boolean publishResult = configService.publishConfig(key, Constants.DCC.CONFIG_GROUP, value);
            if (!publishResult) {
                log.error("DCC 动态配置值变更失败，Nacos 返回 false key:{} value:{}", key, value);
                return Response.<Boolean>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info(ResponseCode.UN_ERROR.getInfo())
                        .build();
            }
            log.info("DCC 动态配置值变更完成 key:{} value:{}", key, value);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("DCC 动态配置值变更失败 key:{} value:{}", key, value, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
}
