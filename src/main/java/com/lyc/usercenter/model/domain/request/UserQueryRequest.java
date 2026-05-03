package com.lyc.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户查询请求
 */
@Data
public class UserQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String userAccount;

    private String phone;

    private String email;

    private Integer userRole;

    private Integer userStatus;

    private Date createTimeFrom;

    private Date createTimeTo;
}
