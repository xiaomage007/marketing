package com.charlie.infrastructure.persistent.dao;

import cn.bugstack.middleware.db.router.annotation.DBRouterStrategy;
import com.charlie.infrastructure.persistent.po.UserAwardRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: 用户中奖记录表
 * @author: Charlie
 * @date: 2026/8/27 7:53
 */
@Mapper
@DBRouterStrategy(splitTable = true)
public interface IUserAwardRecordDao {

    void insert(UserAwardRecord userAwardRecord);

}
