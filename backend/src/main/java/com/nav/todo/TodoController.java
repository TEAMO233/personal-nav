package com.nav.todo;

import com.nav.security.SecurityUtils;
import com.nav.todo.dto.CreateTodoRequest;
import com.nav.todo.dto.TodoResponse;
import com.nav.todo.dto.UpdateTodoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 待办 / 日程接口:首页内联管理。
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * 列出当前用户待办。
     *
     * @param limit 最大返回数量,可空
     * @return 待办列表
     */
    @GetMapping
    public List<TodoResponse> list(@RequestParam(required = false) Integer limit) {
        // 1. 取当前用户待办
        return todoService.list(SecurityUtils.currentUserId(), limit);
    }

    /**
     * 新建待办。
     *
     * @param request 新建请求
     * @return 新待办
     */
    @PostMapping
    public TodoResponse create(@Valid @RequestBody CreateTodoRequest request) {
        // 1. 为当前用户创建待办
        return todoService.create(SecurityUtils.currentUserId(), request);
    }

    /**
     * 更新待办。
     *
     * @param id      待办 id
     * @param request 更新请求
     * @return 更新后待办
     */
    @PutMapping("/{id}")
    public TodoResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTodoRequest request) {
        // 1. 更新当前用户待办
        return todoService.update(SecurityUtils.currentUserId(), id, request);
    }

    /**
     * 删除待办。
     *
     * @param id 待办 id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        // 1. 删除当前用户待办
        todoService.delete(SecurityUtils.currentUserId(), id);
    }
}
