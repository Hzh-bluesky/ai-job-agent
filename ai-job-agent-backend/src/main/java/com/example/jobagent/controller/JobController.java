package com.example.jobagent.controller;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.common.Result;
import com.example.jobagent.dto.JobAnalyzeDTO;
import com.example.jobagent.dto.JobPageQueryDTO;
import com.example.jobagent.security.UserContext;
import com.example.jobagent.service.JobService;
import com.example.jobagent.vo.JobAnalysisVO;
import com.example.jobagent.vo.JobImportResultVO;
import com.example.jobagent.vo.JobPostVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "岗位分析")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    @Operation(summary = "粘贴JD并解析岗位")
    @PostMapping("/analyze")
    public Result<JobAnalysisVO> analyze(@Valid @RequestBody JobAnalyzeDTO analyzeDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(jobService.analyze(currentUserId, analyzeDTO));
    }

    @Operation(summary = "批量导入岗位并自动解析")
    @PostMapping("/import")
    public Result<JobImportResultVO> importJobs(@RequestParam("file") MultipartFile file) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(jobService.importJobs(currentUserId, file));
    }

    @Operation(summary = "分页查询当前用户保存过的岗位")
    @GetMapping
    public Result<PageResult<JobPostVO>> page(@Valid JobPageQueryDTO queryDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(jobService.page(currentUserId, queryDTO));
    }

    @Operation(summary = "查询岗位详情和分析结果")
    @GetMapping("/{id}")
    public Result<JobPostVO> getDetail(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(jobService.getDetail(currentUserId, id));
    }

    @Operation(summary = "删除当前用户的岗位记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        jobService.delete(currentUserId, id);
        return Result.success();
    }
}
