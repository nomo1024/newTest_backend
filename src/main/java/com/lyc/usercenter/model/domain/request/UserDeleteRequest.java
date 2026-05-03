package com.lyc.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户删除请求体
 */
@Data
public class UserDeleteRequest implements Serializable {
    private long id;
}
