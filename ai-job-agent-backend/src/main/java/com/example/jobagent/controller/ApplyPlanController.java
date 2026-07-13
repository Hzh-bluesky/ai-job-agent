package com.example.jobagent.controller;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.common.Result;
import com.example.jobagent.dto.ApplyPlanCreateDTO;
import com.example.jobagent.dto.ApplyPlanPageQueryDTO;
import com.example.jobagent.security.UserContext;
import com.example.jobagent.service.ApplyPlanService;
import com.example.jobagent.vo.ApplyPlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "One click apply plan")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/apply-plans")
public class ApplyPlanController {

    private final ApplyPlanService applyPlanService;

    @Operation(summary = "Generate one click apply plan")
    @PostMapping("/generate")
    public Result<ApplyPlanVO> generate(@Valid @RequestBody ApplyPlanCreateDTO createDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(applyPlanService.generate(currentUserId, createDTO));
    }

    @Operation(summary = "Page query apply plans")
    @GetMapping
    public Result<PageResult<ApplyPlanVO>> page(@Valid ApplyPlanPageQueryDTO queryDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(applyPlanService.page(currentUserId, queryDTO));
    }

    @Operation(summary = "Get apply plan detail")
    @GetMapping("/{id}")
    public Result<ApplyPlanVO> getDetail(@Positive(message = "id must be positive") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(applyPlanService.getDetail(currentUserId, id));
    }
}
