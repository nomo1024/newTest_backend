package com.lyc.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyc.usercenter.common.BaseResponse;
import com.lyc.usercenter.common.ErrorCode;
import com.lyc.usercenter.common.ResultUtils;
import com.lyc.usercenter.exception.BusinessException;
import com.lyc.usercenter.model.domain.User;
import com.lyc.usercenter.model.domain.request.UserDeleteRequest;
import com.lyc.usercenter.model.domain.request.UserLoginRequest;
import com.lyc.usercenter.model.domain.request.UserQueryRequest;
import com.lyc.usercenter.model.domain.request.UserRegisterRequest;
import com.lyc.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.lyc.usercenter.constant.UserConstant.USER_LOGIN_STATE;


/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    @PostMapping("/search")
    public BaseResponse<List<User>> searchUsers(@RequestBody UserQueryRequest userQueryRequest, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (userQueryRequest != null) {
            if (userQueryRequest.getId() != null) {
                if (userQueryRequest.getId() < 1) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID必须大于1");
                }
                queryWrapper.eq("id", userQueryRequest.getId());
            }
            if (StringUtils.isNotBlank(userQueryRequest.getUserAccount())) {
                queryWrapper.like("userAccount", userQueryRequest.getUserAccount());
            }
            if (StringUtils.isNotBlank(userQueryRequest.getPhone())) {
                queryWrapper.like("phone", userQueryRequest.getPhone());
            }
            if (StringUtils.isNotBlank(userQueryRequest.getEmail())) {
                queryWrapper.like("email", userQueryRequest.getEmail());
            }
            if (userQueryRequest.getUserRole() != null) {
                if (userQueryRequest.getUserRole() < 0 || userQueryRequest.getUserRole() > 1) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户权限只能为0或1");
                }
                queryWrapper.eq("userRole", userQueryRequest.getUserRole());
            }
            if (userQueryRequest.getUserStatus() != null) {
                if (userQueryRequest.getUserStatus() <0 || userQueryRequest.getUserStatus() >1){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户状态只能为0或1");
                }
                queryWrapper.eq("userStatus", userQueryRequest.getUserStatus());
            }

            Date now = new Date();
            if (userQueryRequest.getCreateTimeTo() != null) {
                if (userQueryRequest.getCreateTimeFrom().after(now)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间不能晚于当前时间");
                }
                queryWrapper.ge("createTime", userQueryRequest.getCreateTimeFrom());
            }
            if (userQueryRequest.getCreateTimeTo() != null) {
                if (userQueryRequest.getCreateTimeTo().after(now)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间不能晚于当前时间");
                }
                queryWrapper.le("createTime", userQueryRequest.getCreateTimeTo());
            }
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        // 校验参数是否为空
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        int result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody UserDeleteRequest deleteRequest, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }


}

