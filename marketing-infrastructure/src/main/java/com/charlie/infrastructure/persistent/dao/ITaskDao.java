package com.charlie.infrastructure.persistent.dao;

import cn.bugstack.middleware.db.router.annotation.DBRouter;
import com.charlie.infrastructure.persistent.po.Task;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @description: 任务表，发送MQ
 * @author: Charlie
 * @date: 2026/8/27 7:53
 */
@Mapper
public interface ITaskDao {

    void insert(Task task);

    @DBRouter
    void updateTaskSendMessageCompleted(Task task);

    @DBRouter
    void updateTaskSendMessageFail(Task task);

    List<Task> queryNoSendMessageTaskList();

}
