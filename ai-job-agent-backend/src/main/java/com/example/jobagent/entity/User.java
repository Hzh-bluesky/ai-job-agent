package com.example.jobagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.jobagent.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sys_user")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    private String username;

    private String password;

    private String nickname;

    private String email;

    private String role;

    private Integer status;
}
