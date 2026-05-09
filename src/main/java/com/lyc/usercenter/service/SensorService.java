package com.lyc.usercenter.service;

import com.lyc.usercenter.mapper.SensorMapper;
import com.lyc.usercenter.model.domain.SourceData;

public interface SensorService {

    void startWrite();

    void stopWrite();

    void startWrite(int index);

    void stopWrite(int index);



    void insert(SensorMapper mapper, String sourceName, String tableName);
}
