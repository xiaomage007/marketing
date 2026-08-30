package com.charlie.domain.task.repository;

import com.charlie.domain.task.model.entity.TaskEntity;

import java.util.List;

/**
 * @description: 任务服务仓储接口
 * @author: Charlie
 * @date: 2026/8/30 9:29
 */
public interface ITaskRepository {

    List<TaskEntity> queryNoSendMessageTaskList();

    void sendMessage(TaskEntity taskEntity);

    void updateTaskSendMessageCompleted(String userId, String messageId);

    void updateTaskSendMessageFail(String userId, String messageId);


}
