package com.example.jobagent.service;

import com.example.jobagent.dto.LoginDTO;
import com.example.jobagent.dto.RegisterDTO;
import com.example.jobagent.vo.LoginVO;
import com.example.jobagent.vo.UserProfileVO;

public interface AuthService {

    LoginVO register(RegisterDTO registerDTO);

    LoginVO login(LoginDTO loginDTO);

    UserProfileVO getCurrentUser(Long userId);
}
