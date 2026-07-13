package com.example.jobagent.controller;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.common.Result;
import com.example.jobagent.dto.ResumeRewriteDTO;
import com.example.jobagent.dto.ResumeRewritePageQueryDTO;
import com.example.jobagent.security.UserContext;
import com.example.jobagent.service.ResumeRewriteService;
import com.example.jobagent.vo.ResumeRewriteVO;
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

@Tag(name = "简历优化")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resume-rewrite")
public class ResumeRewriteController {

    private final ResumeRewriteService resumeRewriteService;

    @Operation(summary = "生成简历项目优化结果")
    @PostMapping
    public Result<ResumeRewriteVO> create(@Valid @RequestBody ResumeRewriteDTO rewriteDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(resumeRewriteService.create(currentUserId, rewriteDTO));
    }

    @Operation(summary = "分页查询当前用户的简历优化记录")
    @GetMapping
    public Result<PageResult<ResumeRewriteVO>> page(@Valid ResumeRewritePageQueryDTO queryDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(resumeRewriteService.page(currentUserId, queryDTO));
    }

    @Operation(summary = "查询简历优化详情")
    @GetMapping("/{id}")
    public Result<ResumeRewriteVO> getDetail(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(resumeRewriteService.getDetail(currentUserId, id));
    }

    @Operation(summary = "删除当前用户的简历优化记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        resumeRewriteService.delete(currentUserId, id);
        return Result.success();
    }
}
