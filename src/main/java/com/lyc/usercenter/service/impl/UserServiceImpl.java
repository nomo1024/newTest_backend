package com.lyc.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lyc.usercenter.common.ErrorCode;
import com.lyc.usercenter.constant.UserConstant;
import com.lyc.usercenter.exception.BusinessException;
import com.lyc.usercenter.mapper.UserMapper;
import com.lyc.usercenter.model.domain.User;
import com.lyc.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lyc.usercenter.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "lyc";

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 3. 更新用户状态为在线(1)
        user.setUserStatus(1);
        userMapper.updateById(user);
        // 4. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 5. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setPhone(originUser.getPhone());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 获取当前登录用户
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj != null) {
            User user = (User) userObj;
            // 更新用户状态为离线(0)
            user.setUserStatus(0);
            userMapper.updateById(user);
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    @Override
    public int updateUser(User user, User loginUser) {
        long userId = user.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 如果是管理员，允许更新任意用户
        // 如果不是管理员，只允许更新当前（自己的）信息
        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        //处理数据,如果没传,则不更新;如果传了..
        // 检查传递的值是否和数据库中的值一样
        boolean allSame = true;

        if (StringUtils.isNotBlank(user.getUserAccount())) {
            if (!user.getUserAccount().equals(oldUser.getUserAccount())) allSame = false;
        } else user.setUserAccount(oldUser.getUserAccount());

        if (StringUtils.isNotBlank(user.getPhone())) {
            if (!user.getPhone().equals(oldUser.getPhone())) allSame = false;
        } else user.setPhone(oldUser.getPhone());

        if (StringUtils.isNotBlank(user.getEmail())) {
            if (!user.getEmail().equals(oldUser.getEmail())) allSame = false;
        } else user.setEmail(oldUser.getEmail());


        /**
         *  处理密码：如果传了新密码，则加密后更新；如果没传，则保留旧密码，避免将 null 或明文写入数据库
         */
        if (StringUtils.isNotBlank(user.getUserPassword())) {
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + user.getUserPassword()).getBytes());
            user.setUserPassword(encryptPassword);
        } else user.setUserPassword(oldUser.getUserPassword());


        if (user.getUserStatus() != null) {
            if (!user.getUserStatus().equals(oldUser.getUserStatus())) allSame = false;
        } else user.setUserStatus(oldUser.getUserStatus());


        if (user.getUserRole() != null) {
            if (!user.getUserRole().equals(oldUser.getUserRole())) allSame = false;
        } else user.setUserRole(oldUser.getUserRole());

        if (allSame) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "传递的值和当前值相同，无需更新");
        }
        return userMapper.updateById(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User) userObj;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    /**
     * 是否为管理员
     *
     * @param loginUser
     * @return
     */
    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    /**
     * 更新用户状态
     *
     * @param userId    用户ID
     * @param status    状态：0-离线 1-在线 2-隐身 3-忙碌
     * @param loginUser 当前登录用户
     * @return 更新结果
     */
    @Override
    public int updateUserStatus(Long userId, Integer status, User loginUser) {
        // 校验参数
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        if (status == null || status < 0 || status > 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户状态只能为0(离线)、1(在线)、2(隐身)或3(忙碌)");
        }
        // 权限校验：只能修改自己的状态，除非是管理员
        if (!isAdmin(loginUser) && !userId.equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权修改其他用户的状态");
        }
        // 查询用户是否存在
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        // 状态未变，无需更新
        if (status.equals(oldUser.getUserStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户状态未改变");
        }
        // 更新状态
        oldUser.setUserStatus(status);
        return userMapper.updateById(oldUser);
    }
}