package com.lyc.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SourceData implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private double col1;
    private double col2;
    private double col3;
    private double col4;
    private double col5;
    private LocalDateTime collect_time;
}