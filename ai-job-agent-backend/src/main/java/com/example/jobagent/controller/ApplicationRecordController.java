package com.example.jobagent.controller;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.common.Result;
import com.example.jobagent.dto.ApplicationCreateDTO;
import com.example.jobagent.dto.ApplicationPageQueryDTO;
import com.example.jobagent.dto.ApplicationStatusUpdateDTO;
import com.example.jobagent.dto.ApplicationUpdateDTO;
import com.example.jobagent.security.UserContext;
import com.example.jobagent.service.ApplicationRecordService;
import com.example.jobagent.vo.ApplicationRecordVO;
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

@Tag(name = "投递记录")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applications")
public class ApplicationRecordController {

    private final ApplicationRecordService applicationRecordService;

    @Operation(summary = "新增投递记录")
    @PostMapping
    public Result<ApplicationRecordVO> create(@Valid @RequestBody ApplicationCreateDTO createDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(applicationRecordService.create(currentUserId, createDTO));
    }

    @Operation(summary = "分页查询当前用户投递记录")
    @GetMapping
    public Result<PageResult<ApplicationRecordVO>> page(@Valid ApplicationPageQueryDTO queryDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(applicationRecordService.page(currentUserId, queryDTO));
    }

    @Operation(summary = "查询投递记录详情")
    @GetMapping("/{id}")
    public Result<ApplicationRecordVO> getDetail(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(applicationRecordService.getDetail(currentUserId, id));
    }

    @Operation(summary = "修改投递记录")
    @PutMapping("/{id}")
    public Result<ApplicationRecordVO> update(@Positive(message = "必须大于0") @PathVariable Long id,
                                              @Valid @RequestBody ApplicationUpdateDTO updateDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(applicationRecordService.update(currentUserId, id, updateDTO));
    }

    @Operation(summary = "修改投递状态")
    @PutMapping("/{id}/status")
    public Result<ApplicationRecordVO> updateStatus(@Positive(message = "必须大于0") @PathVariable Long id,
                                                    @Valid @RequestBody ApplicationStatusUpdateDTO statusUpdateDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(applicationRecordService.updateStatus(currentUserId, id, statusUpdateDTO));
    }

    @Operation(summary = "删除当前用户投递记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        applicationRecordService.delete(currentUserId, id);
        return Result.success();
    }
}
