package com.example.jobagent.controller;

import com.example.jobagent.common.PageResult;
import com.example.jobagent.common.Result;
import com.example.jobagent.dto.GreetingGenerateDTO;
import com.example.jobagent.dto.GreetingPageQueryDTO;
import com.example.jobagent.security.UserContext;
import com.example.jobagent.service.GreetingService;
import com.example.jobagent.vo.GreetingVO;
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

@Tag(name = "打招呼话术")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/greetings")
public class GreetingController {

    private final GreetingService greetingService;

    @Operation(summary = "生成Boss直聘打招呼话术")
    @PostMapping("/generate")
    public Result<GreetingVO> generate(@Valid @RequestBody GreetingGenerateDTO generateDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(greetingService.generate(currentUserId, generateDTO));
    }

    @Operation(summary = "分页查询当前用户的话术记录")
    @GetMapping
    public Result<PageResult<GreetingVO>> page(@Valid GreetingPageQueryDTO queryDTO) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(greetingService.page(currentUserId, queryDTO));
    }

    @Operation(summary = "查询话术详情")
    @GetMapping("/{id}")
    public Result<GreetingVO> getDetail(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(greetingService.getDetail(currentUserId, id));
    }

    @Operation(summary = "删除当前用户的话术记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Positive(message = "必须大于0") @PathVariable Long id) {
        Long currentUserId = UserContext.getUserId();
        greetingService.delete(currentUserId, id);
        return Result.success();
    }
}
