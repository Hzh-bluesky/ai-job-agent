package com.example.jobagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.jobagent.common.ErrorCode;
import com.example.jobagent.dto.LoginDTO;
import com.example.jobagent.dto.RegisterDTO;
import com.example.jobagent.entity.User;
import com.example.jobagent.exception.BusinessException;
import com.example.jobagent.mapper.UserMapper;
import com.example.jobagent.security.JwtTokenUtil;
import com.example.jobagent.service.AuthService;
import com.example.jobagent.vo.LoginVO;
import com.example.jobagent.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ROLE_USER = "USER";
    private static final int STATUS_NORMAL = 1;

    private final UserMapper userMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO registerDTO) {
        User existsUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (existsUser != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname());
        user.setEmail(registerDTO.getEmail());
        user.setRole(ROLE_USER);
        user.setStatus(STATUS_NORMAL);
        userMapper.insert(user);

        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (!Integer.valueOf(STATUS_NORMAL).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        return buildLoginVO(user);
    }

    @Override
    public UserProfileVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        if (!Integer.valueOf(STATUS_NORMAL).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return toUserProfileVO(user);
    }

    private LoginVO buildLoginVO(User user) {
        String token = jwtTokenUtil.generateToken(user);
        return LoginVO.builder()
                .token(token)
                .user(toUserProfileVO(user))
                .build();
    }

    private UserProfileVO toUserProfileVO(User user) {
        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build();
    }
}
