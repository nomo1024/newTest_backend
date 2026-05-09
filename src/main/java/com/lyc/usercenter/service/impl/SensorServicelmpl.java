package com.lyc.usercenter.service.impl;

import com.lyc.usercenter.mapper.SensorMapper;
import com.lyc.usercenter.model.domain.SourceData;
import com.lyc.usercenter.service.SensorService;
import com.lyc.usercenter.service.mqtt.MqttSubscribeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 传感器数据写入服务（方案A：事件驱动）
 * "开始监测"：启动MQTT订阅，接收消息后立即写入数据库
 * "停止监测"：停止MQTT订阅
 * 订阅5个主题：testtopic/gps, testtopic/humid, testtopic/light, testtopic/press, testtopic/temp
 */
@Component
@Slf4j
public class SensorServicelmpl implements SensorService {

    // ====================== 注入5个数据源的Mapper ======================
    @Autowired
    private SensorMapper dataSource1Mapper; // GPS
    @Autowired
    private SensorMapper dataSource2Mapper; // 湿度
    @Autowired
    private SensorMapper dataSource3Mapper; // 光照
    @Autowired
    private SensorMapper dataSource4Mapper; // 气压
    @Autowired
    private SensorMapper dataSource5Mapper; // 温度

    // MQTT订阅服务
    @Autowired
    private MqttSubscribeService mqttSubscribeService;

    // 5个数据源对应的表名和主题
    private static final String TABLE_GPS = "gps_data";
    private static final String TABLE_HUMID = "humidity_data";
    private static final String TABLE_LIGHT = "light_data";
    private static final String TABLE_PRESS = "pressure_data";
    private static final String TABLE_TEMP = "temperature_data";

    // 传感器索引到主题的映射（source 1-5 对应 index 0-4）
    private static final String[] TOPICS = {
        "testtopic/gps",     // index 0: GPS (source 1)
        "testtopic/humid",   // index 1: 湿度 (source 2)
        "testtopic/light",   // index 2: 光照 (source 3)
        "testtopic/press",   // index 3: 气压 (source 4)
        "testtopic/temp"     // index 4: 温度 (source 5)
    };

    // 传感器索引到表名的映射
    private static final String[] TABLES = {
        TABLE_GPS, TABLE_HUMID, TABLE_LIGHT, TABLE_PRESS, TABLE_TEMP
    };

    // 传感器索引到Mapper的映射
    private SensorMapper[] mappers = new SensorMapper[5];

    // 运行状态（每个传感器独立控制）
    private volatile boolean[] isRunning = {false, false, false, false, false};

    // ====================== 初始化 ======================
    @PostConstruct
    public void init() {
        // 初始化 mappers 映射
        mappers[0] = dataSource1Mapper;
        mappers[1] = dataSource2Mapper;
        mappers[2] = dataSource3Mapper;
        mappers[3] = dataSource4Mapper;
        mappers[4] = dataSource5Mapper;

        // 设置MQTT消息处理器：根据主题判断传感器类型，立即写入数据库
        mqttSubscribeService.setMessageHandler(new MqttSubscribeService.MessageHandler() {
            public void handle(String topic, String payload) {
                try {
                    double value = Double.parseDouble(payload.trim());
                    log.info("收到传感器数据 [{}]: {}", topic, value);

                    // 根据主题找到对应的索引
                    for (int i = 0; i < TOPICS.length; i++) {
                        if (topic.equals(TOPICS[i])) {
                            writeToDatabase(i, value);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn("处理传感器数据失败: {}", e.getMessage());
                }
            }
        });
    }

    // ====================== 开始/停止监测（控制MQTT启停，只订阅/取消当前传感器主题） ======================
    public synchronized void startWrite() {
        // 不传index则全部启动
        startWrite(0);
        startWrite(1);
        startWrite(2);
        startWrite(3);
        startWrite(4);
    }

    public synchronized void startWrite(int index) {
        if (index < 0 || index >= 5) return;
        if (isRunning[index]) return;
        
        String topic = TOPICS[index];
        mqttSubscribeService.start(topic);
        isRunning[index] = true;
        log.info("✅ 已开始监测传感器{}（订阅主题：{}）", index + 1, topic);
    }

    public synchronized void stopWrite() {
        // 停止所有
        for (int i = 0; i < 5; i++) {
            stopWrite(i);
        }
    }

    public synchronized void stopWrite(int index) {
        if (index < 0 || index >= 5) return;
        if (!isRunning[index]) return;
        
        String topic = TOPICS[index];
        mqttSubscribeService.stop(topic);
        isRunning[index] = false;
        log.info("⏸️ 已停止监测传感器{}（取消订阅：{}）", index + 1, topic);
    }

    // ====================== 写入数据库 ======================
    /**
     * 将传感器数据写入对应表
     * @param index 传感器索引（0=GPS, 1=湿度, 2=光照, 3=气压, 4=温度）
     * @param value 传感器值
     */
    private void writeToDatabase(int index, double value) {
        try {
            String tableName = TABLES[index];
            SensorMapper mapper = mappers[index];
            
            SourceData data = new SourceData();
            data.setCol1(Math.round(value * 100.0) / 100.0);
            data.setCollect_time(LocalDateTime.now());
            
            int rows = mapper.insertToTable(tableName, data);
            if (rows > 0) {
                log.debug("传感器{}数据已写入表{}: {}", index + 1, tableName, value);
            } else {
                log.warn("传感器{}写入表{}失败", index + 1, tableName);
            }
        } catch (Exception e) {
            log.error("传感器{}写入数据库失败: {}", index + 1, e.getMessage(), e);
        }
    }



    // ====================== 接口兼容方法（空实现） ======================
    public void writeSource1() {}
    public void writeSource2() {}
    public void writeSource3() {}
    public void writeSource4() {}
    public void writeSource5() {}
    public void insert(SensorMapper mapper, String sourceName, String tableName) {}

    /**
     * 查询数据源是否正在运行
     */
    public boolean isRunning(int index) {
        if (index < 0 || index >= 5) return false;
        return isRunning[index];
    }

    /**
     * 获取所有数据源运行状态
     */
    public boolean[] getRunningStates() {
        return java.util.Arrays.copyOf(isRunning, isRunning.length);
    }

    /**
     * 读取指定数据源最近 limit 条数据（index 0..4）
     */
    public List<SourceData> readLatest(int index, int limit) {
        if (index < 0 || index >= 5) {
            throw new IllegalArgumentException("index 必须在 0..4");
        }
        SensorMapper mapper = mappers[index];
        String table = TABLES[index];
        return mapper.selectRecentFromTable(table, limit);
    }
}
