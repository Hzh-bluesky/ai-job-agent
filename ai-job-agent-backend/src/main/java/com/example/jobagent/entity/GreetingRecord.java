package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("greeting_record")
@EqualsAndHashCode(callSuper = true)
public class GreetingRecord extends BaseEntity {

    private Long userId;

    private Long resumeId;

    private Long jobPostId;

    private String greetingText;

    private String rawResult;
}
