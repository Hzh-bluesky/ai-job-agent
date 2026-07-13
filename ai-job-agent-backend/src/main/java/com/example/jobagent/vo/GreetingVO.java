package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Boss直聘打招呼话术记录")
public class GreetingVO {

    private Long id;

    private Long userId;

    private Long resumeId;

    private Long jobPostId;

    private String greetingText;

    private String rawResult;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
