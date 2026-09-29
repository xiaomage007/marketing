package com.charlie.api;

import com.charlie.api.response.Response;

/**
 * @description: DCC 动态配置中心
 * @author: Charlie
 * @date: 2026/9/28 14:03
 */
public interface IDCCService {

    Response<Boolean> updateConfig(String key, String value);

}
