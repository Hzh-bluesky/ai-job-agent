package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("interview_question_record")
@EqualsAndHashCode(callSuper = true)
public class InterviewQuestionRecord extends BaseEntity {

    private Long userId;

    private Long jobPostId;

    private String technicalQuestions;

    private String projectQuestions;

    private String hrQuestions;

    private String rawResult;
}
