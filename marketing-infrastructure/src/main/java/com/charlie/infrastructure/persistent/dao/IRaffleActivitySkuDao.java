package com.charlie.infrastructure.persistent.dao;

import com.charlie.infrastructure.persistent.po.RaffleActivitySku;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: 商品sku
 * @author: Charlie
 * @date: 2026/8/13 10:06
 */
@Mapper
public interface IRaffleActivitySkuDao {

    RaffleActivitySku queryActivitySku(Long sku);

}
