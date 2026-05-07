package com.lyc.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lyc.usercenter.mapper.SensorMapper;
import com.lyc.usercenter.model.domain.SourceData;
import com.lyc.usercenter.service.SensorService;
import com.lyc.usercenter.service.mqtt.MqttSubscribeService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 传感器数据写入服务
 * 应用启动时自动订阅 MQTT 主题接收传感器数据
 * 通过外部调用 startWrite()/stopWrite() 控制是否写入数据库
 * 数据来源：订阅 MQTT 主题 testtopic/1
 */
@Component
@Slf4j
public class SensorServicelmpl extends ServiceImpl<SensorMapper, SourceData> implements SensorService {

    // ====================== 注入5个数据源的 Mapper ======================
    @Resource
    private SensorMapper dataSource1Mapper;
    @Resource
    private SensorMapper dataSource2Mapper;
    @Resource
    private SensorMapper dataSource3Mapper;
    @Resource
    private SensorMapper dataSource4Mapper;
    @Resource
    private SensorMapper dataSource5Mapper;

    // 定时任务线程池（5个数据源）
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    // MQTT 订阅服务（抽离后的模块）
    @Autowired
    private MqttSubscribeService mqttSubscribeService;

    // 传感器当前值（从 MQTT 接收）
    private volatile double currentGps = 100.0;       // 海拔，单位m
    private volatile double currentTemp = 25.0;       // 温度，单位°C
    private volatile double currentHumid = 60.0;      // 湿度，单位%RH
    private volatile double currentLight = 40.0;        // 光照，单位kLux
    private volatile double currentPress = 1013.25;   // 气压，单位hPa

    @Getter
    public final boolean[] isRunning = new boolean[5];

    // 保存定时任务的 Future，以便停止时取消
    private final ScheduledFuture<?>[] futures = new ScheduledFuture<?>[5];

    // 所有 mapper 和表名按 index 对应
    private final SensorMapper[] mappers = new SensorMapper[5];
    private final String[] tableNames = new String[5];

    // 应用启动时设置消息处理器
    @javax.annotation.PostConstruct
    public void init() {
        // 设置 MQTT 消息处理器
        mqttSubscribeService.setMessageHandler(this::parseSensorData);
        initMappings();
    }

    // 初始化映射关系
    private void initMappings() {
        mappers[0] = dataSource1Mapper;
        mappers[1] = dataSource2Mapper;
        mappers[2] = dataSource3Mapper;
        mappers[3] = dataSource4Mapper;
        mappers[4] = dataSource5Mapper;

        // 与 writeSource1~5 及常量注释保持一致
        tableNames[0] = TABLE_GPS;      // index 0: GPS
        tableNames[1] = TABLE_HUMID;   // index 1: 湿度
        tableNames[2] = TABLE_LIGHT;   // index 2: 光照
        tableNames[3] = TABLE_PRESS;   // index 3: 气压
        tableNames[4] = TABLE_TEMP;    // index 4: 温度
    }

    // 解析传感器数据并更新当前值
    private void parseSensorData(String json) {
        try {
            // 简单解析 JSON（假设格式包含传感器数据）
            // 示例: {"gps": 100.5, "temp": 25.3, "humid": 60.0, "light": 40.0, "press": 1013.25}
            if (json.contains("\"gps\"")) {
                currentGps = extractValue(json, "\"gps\"");
            }
            if (json.contains("\"temp\"")) {
                currentTemp = extractValue(json, "\"temp\"");
            }
            if (json.contains("\"humid\"")) {
                currentHumid = extractValue(json, "\"humid\"");
            }
            if (json.contains("\"light\"")) {
                currentLight = extractValue(json, "\"light\"");
            }
            if (json.contains("\"press\"")) {
                currentPress = extractValue(json, "\"press\"");
            }
        } catch (Exception e) {
            log.warn("解析传感器数据失败: {}", e.getMessage());
        }
    }

    private double extractValue(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return 0;
            int start = json.indexOf(':', idx) + 1;
            int end = json.indexOf(',', start);
            if (end == -1) end = json.indexOf('}', start);
            if (end == -1) end = json.length();
            String val = json.substring(start, end).trim();
            return Double.parseDouble(val);
        } catch (Exception e) {
            return 0;
        }
    }

    // ====================== 开始写入（单个/全部） ======================
    public synchronized void startWrite() {
        // 启动全部
        initMappings();
        boolean anyStarted = false;
        for (int i = 0; i < 5; i++) {
            if (!isRunning[i]) {
                startWrite(i);
                anyStarted = true;
            }
        }
        if (anyStarted) {
            log.info("✅ 已启动所有数据源写入");
        }
    }

    public synchronized void startWrite(int index) {
        if (index < 0 || index >= 5) return;
        initMappings();
        if (isRunning[index]) return; // 已经运行

        // 从数据库读取最后一条数据作为初始值
        try {

            List<SourceData> lastList = readLatest(index,1);
            if (lastList != null && !lastList.isEmpty()) {
                double lastValue = lastList.get(0).getCol1();
                switch (index) {
                    case 0: currentGps = lastValue; break;
                    case 1: currentHumid = lastValue; break;
                    case 2: currentLight = lastValue; break;
                    case 3: currentPress = lastValue; break;
                    case 4: currentTemp = lastValue; break;
                }
                log.info("数据源 {} 从数据库恢复初始值: {}", index + 1, lastValue);
            } else {
                setDefaultInitialValue(index);
            }
        } catch (Exception e) {
            log.warn("数据源 {} 读取数据库初始值失败，使用默认值: {}", index + 1, e.getMessage());
            setDefaultInitialValue(index);
        }

        // schedule任务，task 会调用对应的 writeSourceX
        ScheduledFuture<?> future = null;
        switch (index) {
            case 0:
                future = scheduler.scheduleAtFixedRate(this::writeSource1, 0, 2, TimeUnit.SECONDS);
                break;
            case 1:
                future = scheduler.scheduleAtFixedRate(this::writeSource2, 0, 2, TimeUnit.SECONDS);
                break;
            case 2:
                future = scheduler.scheduleAtFixedRate(this::writeSource3, 0, 2, TimeUnit.SECONDS);
                break;
            case 3:
                future = scheduler.scheduleAtFixedRate(this::writeSource4, 0, 2, TimeUnit.SECONDS);
                break;
            case 4:
                future = scheduler.scheduleAtFixedRate(this::writeSource5, 0, 2, TimeUnit.SECONDS);
                break;
        }
        futures[index] = future;
        isRunning[index] = true;
        log.info("✅ 数据源 {} 已开始写入", index + 1);
    }

    private void setDefaultInitialValue(int index) {
        switch (index) {
            case 0: currentGps = 100.0; break;
            case 1: currentHumid = 60.0; break;
            case 2: currentLight = 0.5; break;
            case 3: currentPress = 1013.25; break;
            case 4: currentTemp = 25.0; break;
        }
    }

    // ====================== 停止写入（单个/全部） ======================
    public synchronized void stopWrite() {
        // 停止全部
        for (int i = 0; i < 5; i++) {
            stopWrite(i);
        }
        log.info("⏸️ 已停止所有数据源写入");
    }

    public synchronized void stopWrite(int index) {
        if (index < 0 || index >= 5) return;
        if (!isRunning[index]) return;
        ScheduledFuture<?> f = futures[index];
        if (f != null) {
            f.cancel(false);
            futures[index] = null;
        }
        isRunning[index] = false;
        log.info("⏸️ 数据源 {} 已停止写入", index + 1);
    }

    // ====================== 写入方法 ======================
    public void writeSource1() {
        if (!isRunning[0]) return;
        insert(dataSource1Mapper, "gps", TABLE_GPS);
    }

    public void writeSource2() {
        if (!isRunning[1]) return;
        insert(dataSource2Mapper, "湿度", TABLE_HUMID);
    }

    public void writeSource3() {
        if (!isRunning[2]) return;
        insert(dataSource3Mapper, "光照", TABLE_LIGHT);
    }

    public void writeSource4() {
        if (!isRunning[3]) return;
        insert(dataSource4Mapper, "气压", TABLE_PRESS);
    }

    public void writeSource5() {
        if (!isRunning[4]) return;
        insert(dataSource5Mapper, "温度", TABLE_TEMP);
    }

    /**
     *  不同数据源对应的表名
     */

    /**
     * gps  (index=0 | source=1)
     */
    private static final String TABLE_GPS = "gps_data";
    /**
     * 湿度  (index=1 | source=2)
     */
    private static final String TABLE_HUMID = "humidity_data";
    /**
     * 光照  (index=2 | source=3)
     */
    private static final String TABLE_LIGHT = "light_data";
    /**
     * 气压  (index=3 | source=4)
     */
    private static final String TABLE_PRESS = "pressure_data";
    /**
     * 温度  (index=4 | source=5)
     */
    private static final String TABLE_TEMP = "temperature_data";

    //@todo faker假数据改用MQTT协议生成数据
    // 统一插入数据（从 MQTT 接收的传感器数据）
    public void insert(SensorMapper mapper, String sourceName, String tableName) {
        SourceData data = new SourceData();

        // 使用从 MQTT 接收的当前值
        switch (tableName) {
            case TABLE_GPS:
                data.setCol1(Math.round(currentGps * 100.0) / 100.0);
                break;
            case TABLE_TEMP:
                data.setCol1(Math.round(currentTemp * 100.0) / 100.0);
                break;
            case TABLE_HUMID:
                data.setCol1(Math.round(currentHumid * 100.0) / 100.0);
                break;
            case TABLE_LIGHT:
                data.setCol1(Math.round(currentLight * 100.0) / 100.0);
                break;
            case TABLE_PRESS:
                data.setCol1(Math.round(currentPress * 100.0) / 100.0);
                break;
            default:
                data.setCol1(0.0);
        }

        data.setCollect_time(LocalDateTime.now());

        // 使用自定义的动态表插入方法，捕获并记录异常以便排查
        try {
            int rows = mapper.insertToTable(tableName, data);
            if (rows <= 0) {
                log.warn("插入到表 {} 未产生任何行：{}", tableName, data);
            } else {
                log.debug("插入到表 {} 成功，id={}, data={}", tableName, data.getId(), data);
            }
        } catch (Exception e) {
            log.error("向表 {} 插入数据失败，data={}，异常：{}", tableName, data, e.getMessage(), e);
        }
    }

    /**
     * 读取指定数据源最近 limit 条数据（index 0..4）
     */
    public List<SourceData> readLatest(int index, int limit) {
        initMappings();
        if (index < 0 || index >= mappers.length) {
            throw new IllegalArgumentException("index 必须在 0..4");
        }
        if (limit <= 0) {
            limit = 1;
        }
        SensorMapper mapper = mappers[index];
        String table = tableNames[index];
        return mapper.selectRecentFromTable(table, limit);
    }

    /**
     * 查询某个数据源是否正在运行
     */
    public boolean isRunning(int index) {
        if (index < 0 || index >= isRunning.length) return false;
        return isRunning[index];
    }

    /**
     * 获取所有数据源运行状态
     */
    public boolean[] getRunningStates() {
        return java.util.Arrays.copyOf(isRunning, isRunning.length);
    }

}
