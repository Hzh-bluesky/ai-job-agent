package com.example.jobagent.controller;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.common.Result;
import com.example.jobagent.dto.MatchCreateDTO;
import com.example.jobagent.dto.MatchPageQueryDTO;
import com.example.jobagent.security.UserContext;
import com.example.jobagent.service.MatchService;
import com.example.jobagent.vo.MatchReportVO;
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

@Tag(name = "简历匹配")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/match")
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "生成简历与岗位匹配报告")
    @PostMapping
    public Result<MatchReportVO> create(@Valid @RequestBody MatchCreateDTO createDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(matchService.create(currentUserId, createDTO));
    }

    @Operation(summary = "分页查询当前用户的匹配报告")
    @GetMapping
    public Result<PageResult<MatchReportVO>> page(@Valid MatchPageQueryDTO queryDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(matchService.page(currentUserId, queryDTO));
    }

    @Operation(summary = "查询匹配报告详情")
    @GetMapping("/{id}")
    public Result<MatchReportVO> getDetail(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(matchService.getDetail(currentUserId, id));
    }

    @Operation(summary = "删除当前用户的匹配报告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        matchService.delete(currentUserId, id);
        return Result.success();
    }
}
