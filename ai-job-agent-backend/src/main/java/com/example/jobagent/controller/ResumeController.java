package com.example.jobagent.controller;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.common.Result;
import com.example.jobagent.dto.ResumeCreateDTO;
import com.example.jobagent.dto.ResumePageQueryDTO;
import com.example.jobagent.dto.ResumeUpdateDTO;
import com.example.jobagent.security.UserContext;
import com.example.jobagent.service.ResumeService;
import com.example.jobagent.vo.ResumeVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "简历管理")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "新增简历")
    @PostMapping
    public Result<ResumeVO> create(@Valid @RequestBody ResumeCreateDTO createDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(resumeService.create(currentUserId, createDTO));
    }

    @Operation(summary = "分页查询当前登录用户的简历")
    @GetMapping
    public Result<PageResult<ResumeVO>> page(@Valid ResumePageQueryDTO queryDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(resumeService.page(currentUserId, queryDTO));
    }

    @Operation(summary = "查询当前登录用户的简历详情")
    @GetMapping("/{id}")
    public Result<ResumeVO> getDetail(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(resumeService.getDetail(currentUserId, id));
    }

    @Operation(summary = "修改当前登录用户的简历")
    @PutMapping("/{id}")
    public Result<ResumeVO> update(@Positive(message = "必须大于0") @PathVariable Long id,
                                   @Valid @RequestBody ResumeUpdateDTO updateDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(resumeService.update(currentUserId, id, updateDTO));
    }

    @Operation(summary = "设置默认简历")
    @PutMapping("/{id}/default")
    public Result<ResumeVO> setDefault(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(resumeService.setDefault(currentUserId, id));
    }

    @Operation(summary = "删除当前登录用户的简历")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        resumeService.delete(currentUserId, id);
        return Result.success();
    }
}
