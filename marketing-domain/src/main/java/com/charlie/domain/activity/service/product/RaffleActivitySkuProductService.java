package com.charlie.domain.activity.service.product;

import com.charlie.domain.activity.model.entity.SkuProductEntity;
import com.charlie.domain.activity.repository.IActivityRepository;
import com.charlie.domain.activity.service.IRaffleActivitySkuProductService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: sku商品服务
 * @author: Charlie
 * @date: 2026/9/21 8:29
 */
@Service
public class RaffleActivitySkuProductService implements IRaffleActivitySkuProductService {

    @Resource
    private IActivityRepository repository;

    @Override
    public List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId) {
        return repository.querySkuProductEntityListByActivityId(activityId);
    }

}
