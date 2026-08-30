package com.charlie.domain.task.service;

import com.charlie.domain.task.model.entity.TaskEntity;
import com.charlie.domain.task.repository.ITaskRepository;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @description: 消息任务服务
 * @author: Charlie
 * @date: 2026/8/30 9:32
 */
public class TaskService implements ITaskService {

    @Resource
    private ITaskRepository taskRepository;

    @Override
    public List<TaskEntity> queryNoSendMessageTaskList() {
        return Collections.emptyList();
    }

    @Override
    public void sendMessage(TaskEntity taskEntity) {

    }

    @Override
    public void updateTaskSendMessageCompleted(String userId, String messageId) {

    }

    @Override
    public void updateTaskSendMessageFail(String userId, String messageId) {

    }
}
