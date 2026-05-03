package com.lyc.usercenter.constant;

/**
 * 用户常量
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "userLoginState";

    //  ------- 权限 --------

    /**
     * 默认权限
     */
    int DEFAULT_ROLE = 0;

    /**
     * 管理员权限
     */
    int ADMIN_ROLE = 1;

    //  ------- 用户状态 --------

    /**
     * 离线
     */
    int STATUS_OFFLINE = 0;

    /**
     * 在线
     */
    int STATUS_ONLINE = 1;

    /**
     * 隐身
     */
    int STATUS_INVISIBLE = 2;

    /**
     * 忙碌
     */
    int STATUS_BUSY = 3;

}
