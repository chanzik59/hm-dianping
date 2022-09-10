package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Objects;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            Result.fail("手机格式不正确");
        }
        String code = RandomUtil.randomNumbers(6);
        session.setAttribute("code", code);
        log.info("短信验证码{}", code);


        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            Result.fail("手机格式不正确");
        }
        Object cacheCode = session.getAttribute("code");
        if (Objects.isNull(cacheCode) || !cacheCode.toString().equalsIgnoreCase(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (Objects.isNull(user)) {
            user = createUserWithPhone(loginForm.getPhone());
        }
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        String userName = "user" + RandomUtil.randomString(5);
        user.setNickName(userName);
        save(user);
        return user;
    }
}
