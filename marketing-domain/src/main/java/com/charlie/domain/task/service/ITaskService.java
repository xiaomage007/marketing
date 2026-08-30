package com.charlie.domain.task.service;

import com.charlie.domain.task.model.entity.TaskEntity;

import java.util.List;

/**
 * @description: 消息任务服务接口
 * @author: Charlie
 * @date: 2026/8/30 9:31
 */
public interface ITaskService {

    /**
     * 查询发送MQ失败和超时1分钟未发送的MQ
     *
     * @return 未发送的任务消息列表10条
     */
    List<TaskEntity> queryNoSendMessageTaskList();

    void sendMessage(TaskEntity taskEntity);

    void updateTaskSendMessageCompleted(String userId, String messageId);

    void updateTaskSendMessageFail(String userId, String messageId);

}
