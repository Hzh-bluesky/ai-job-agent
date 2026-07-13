package com.example.jobagent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "面试题生成记录")
public class InterviewQuestionVO {

    private Long id;

    private Long userId;

    private Long jobPostId;

    private String technicalQuestions;

    private String projectQuestions;

    private String hrQuestions;

    private String rawResult;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
