package com.lyc.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lyc.usercenter.mapper.SensorMapper;
import com.lyc.usercenter.model.domain.SourceData;

 public interface SensorService extends IService<SourceData> {

     void startWrite();

     void stopWrite();

     void writeSource1();

     void writeSource2();

     void writeSource3();

     void writeSource4();

     void writeSource5();

     void insert(SensorMapper mapper, String sourceName, String tableName);


}