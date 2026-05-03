package com.lyc.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体
 */
@TableName(value = "user")
@Data
public class User implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableLogic
    private Integer isDelete;

    private String userAccount;
    private String userPassword;
    private String phone;
    private String email;
    private Integer userStatus;
    private Date createTime;
    private Integer userRole;


}