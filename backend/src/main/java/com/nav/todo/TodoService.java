package com.nav.todo;

import com.nav.common.error.ApiException;
import com.nav.notification.NotificationService;
import com.nav.todo.dto.CreateTodoRequest;
import com.nav.todo.dto.TodoResponse;
import com.nav.todo.dto.UpdateTodoRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 待办 / 日程业务:按用户隔离的首页内联列表、新增、更新、完成与删除。
 */
@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final NotificationService notificationService;

    public TodoService(TodoRepository todoRepository, NotificationService notificationService) {
        this.todoRepository = todoRepository;
        this.notificationService = notificationService;
    }

    /**
     * 列出当前用户待办。
     *
     * @param userId 用户 id
     * @param limit  最大返回数量,可空
     * @return 待办列表
     */
    @Transactional(readOnly = true)
    public List<TodoResponse> list(UUID userId, Integer limit) {
        // 1. 按首页顺序排序:未完成优先、计划时间靠前、排序值靠前
        Sort sort = homeSort();
        List<Todo> todos = limit == null
                ? todoRepository.findByUserId(userId, sort)
                : todoRepository.findByUserId(userId, PageRequest.of(0, Math.max(1, Math.min(limit, 50)), sort));
        // 2. 转响应
        return todos.stream().map(TodoResponse::from).toList();
    }

    /**
     * 新建待办,排到当前用户末尾。
     *
     * @param userId  用户 id
     * @param request 新建请求
     * @return 新待办
     */
    @Transactional
    public TodoResponse create(UUID userId, CreateTodoRequest request) {
        // 1. 建待办
        Todo todo = new Todo();
        todo.setUserId(userId);
        todo.setTitle(request.title());
        todo.setTag(request.tag());
        todo.setScheduledAt(request.scheduledAt());
        todo.setSortOrder((int) todoRepository.countByUserId(userId));
        // 2. 保存并返回
        return TodoResponse.from(todoRepository.save(todo));
    }

    /**
     * 更新待办。
     *
     * @param userId  用户 id
     * @param id      待办 id
     * @param request 更新请求
     * @return 更新后待办
     */
    @Transactional
    public TodoResponse update(UUID userId, UUID id, UpdateTodoRequest request) {
        // 1. 取本人待办
        Todo todo = requireOwned(userId, id);
        // 2. 更新字段
        todo.setTitle(request.title());
        todo.setTag(request.tag());
        todo.setScheduledAt(request.scheduledAt());
        if (request.done() != null) {
            todo.setDone(request.done());
        }
        // 3. 完成 / 重新排期到未来 / 清空时间后,同步解决旧逾期通知;仍逾期则后续通知列表会重新同步
        if (todo.isDone() || todo.getScheduledAt() == null || !todo.getScheduledAt().isBefore(Instant.now())) {
            notificationService.resolveTodoOverdue(userId, todo.getId());
        }
        // 4. 保存并返回
        return TodoResponse.from(todoRepository.save(todo));
    }

    /**
     * 删除待办。
     *
     * @param userId 用户 id
     * @param id     待办 id
     */
    @Transactional
    public void delete(UUID userId, UUID id) {
        // 1. 取本人待办
        Todo todo = requireOwned(userId, id);
        // 2. 先解决关联逾期通知,再删除待办
        notificationService.resolveTodoOverdue(userId, todo.getId());
        todoRepository.delete(todo);
    }

    /**
     * 取本人待办,不存在或越权均 404。
     */
    private Todo requireOwned(UUID userId, UUID id) {
        return todoRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "待办不存在"));
    }

    /**
     * 首页待办排序。
     */
    private Sort homeSort() {
        return Sort.by(
                Sort.Order.asc("done"),
                Sort.Order.asc("scheduledAt"),
                Sort.Order.asc("sortOrder"),
                Sort.Order.desc("createdAt"));
    }
}
