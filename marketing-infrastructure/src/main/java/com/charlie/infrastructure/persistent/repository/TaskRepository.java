package com.charlie.infrastructure.persistent.repository;

import com.charlie.domain.task.model.entity.TaskEntity;
import com.charlie.domain.task.repository.ITaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * @description: 任务服务仓储实现
 * @author: Charlie
 * @date: 2026/8/30 9:23
 */
@Slf4j
@Repository
public class TaskRepository implements ITaskRepository {

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
