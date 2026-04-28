package com.lyc.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.javafaker.Faker;
import com.lyc.usercenter.mapper.SensorMapper;
import com.lyc.usercenter.model.domain.SourceData;
import com.lyc.usercenter.service.SensorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
 * 默认不自动写入。通过外部调用 startWrite()/stopWrite() 控制写入。
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
    private final Faker faker = new Faker();

    // 每个数据源的运行状态（index 0..4）
    @Getter
    public final boolean[] isRunning = new boolean[5];

    // 保存定时任务的 Future，以便停止时取消
    private final ScheduledFuture<?>[] futures = new ScheduledFuture<?>[5];

    // 所有 mapper 和表名按 index 对应
    private final SensorMapper[] mappers = new SensorMapper[5];
    private final String[] tableNames = new String[5];

    // 初始化映射关系
    private void initMappings() {
        mappers[0] = dataSource1Mapper;
        mappers[1] = dataSource2Mapper;
        mappers[2] = dataSource3Mapper;
        mappers[3] = dataSource4Mapper;
        mappers[4] = dataSource5Mapper;

        tableNames[0] = TABLE_GPS;
        tableNames[1] = TABLE_TEMP;
        tableNames[2] = TABLE_HUMID;
        tableNames[3] = TABLE_LIGHT;
        tableNames[4] = TABLE_PRESS;
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
        insert(dataSource2Mapper, "温度", TABLE_TEMP);
    }

    public void writeSource3() {
        if (!isRunning[2]) return;
        insert(dataSource3Mapper, "湿度", TABLE_HUMID);
    }

    public void writeSource4() {
        if (!isRunning[3]) return;
        insert(dataSource4Mapper, "光照", TABLE_LIGHT);
    }

    public void writeSource5() {
        if (!isRunning[4]) return;
        insert(dataSource5Mapper, "气压", TABLE_PRESS);
    }

    /**
     *  不同数据源对应的表名
     */

    /**
     * gps  (index=0)
     */
    private static final String TABLE_GPS = "gps_data";
    /**
     * 温度  (index=1)
     */
    private static final String TABLE_TEMP = "temperature_data";
    /**
     * 湿度  (index=2)
     */
    private static final String TABLE_HUMID = "humidity_data";
    /**
     * 光照  (index=3)
     */
    private static final String TABLE_LIGHT = "light_data";
    /**
     * 气压  (index=4)
     */
    private static final String TABLE_PRESS = "pressure_data";

    // 统一插入假数据，并将数据写入指定表
    public void insert(SensorMapper mapper, String sourceName, String tableName) {
        SourceData data = new SourceData();
        // 生成 2 位小数的随机值，范围可根据 sourceName 调整
        double v1 = faker.number().randomDouble(2, 0, 100);
        double v2 = faker.number().randomDouble(2, 0, 100);
        double v3 = faker.number().randomDouble(2, 0, 100);
        double v4 = faker.number().randomDouble(2, 0, 100);
        double v5 = faker.number().randomDouble(2, 0, 100);
        data.setCol1(v1);
        data.setCol2(v2);
        data.setCol3(v3);
        data.setCol4(v4);
        data.setCol5(v5);
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
     * 读取指定数据源最近 N 条数据（index 0..4）
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
