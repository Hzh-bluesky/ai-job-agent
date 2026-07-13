package com.example.jobagent.controller;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.common.Result;
import com.example.jobagent.dto.InterviewGenerateDTO;
import com.example.jobagent.dto.InterviewPageQueryDTO;
import com.example.jobagent.security.UserContext;
import com.example.jobagent.service.InterviewService;
import com.example.jobagent.vo.InterviewQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "面试准备")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "生成面试题和回答思路")
    @PostMapping("/generate")
    public Result<InterviewQuestionVO> generate(@Valid @RequestBody InterviewGenerateDTO generateDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(interviewService.generate(currentUserId, generateDTO));
    }

    @Operation(summary = "分页查询当前用户的面试题记录")
    @GetMapping
    public Result<PageResult<InterviewQuestionVO>> page(@Valid InterviewPageQueryDTO queryDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(interviewService.page(currentUserId, queryDTO));
    }

    @Operation(summary = "查询面试题详情")
    @GetMapping("/{id}")
    public Result<InterviewQuestionVO> getDetail(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(interviewService.getDetail(currentUserId, id));
    }

    @Operation(summary = "删除当前用户的面试题记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        interviewService.delete(currentUserId, id);
        return Result.success();
    }
}
