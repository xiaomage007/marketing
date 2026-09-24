package com.charlie.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: 商品购物车请求对象
 * @author: Charlie
 * @date: 2026/9/21 9:25
 */
@Data
public class SkuProductShopCartRequestDTO implements Serializable {

    /**
     * 用户ID
     */
    private String userId;
    /**
     * sku 商品
     */
    private Long sku;

}
