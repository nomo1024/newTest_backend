package com.lyc.usercenter.controller;

import com.lyc.usercenter.service.impl.SensorServicelmpl;
import com.lyc.usercenter.model.domain.SourceData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/data")
public class DataController {

    @Resource
    private SensorServicelmpl taskManager;

    // 开始写入（可传 source=1..5 指定单个数据源；不传则启动全部）
    @GetMapping("/start")
    public Map<String, Object> start(Integer source) {
        if (source == null) {
            taskManager.startWrite();
            return result("success", "已开始自动写入全部数据源");
        }
        if (source < 1 || source > 5) {
            return result("error", "source 必须为 1..5");
        }
        taskManager.startWrite(source - 1);
        return result("success", "已开始自动写入数据源 " + source);
    }

    // 停止写入（可传 source=1..5 指定单个数据源；不传则停止全部）
    @GetMapping("/stop")
    public Map<String, Object> stop(Integer source) {
        if (source == null) {
            taskManager.stopWrite();
            return result("success", "已停止全部数据源写入");
        }
        if (source < 1 || source > 5) {
            return result("error", "source 必须为 1..5");
        }
        taskManager.stopWrite(source - 1);
        return result("success", "已停止数据源 " + source + " 的写入");
    }

    // 查看当前状态（不传返回所有；传 source=1..5 返回指定）
    @GetMapping("/status")
    public Map<String, Object> status(Integer source) {
        if (taskManager == null) return result("error", "service unavailable");
        if (source == null) {
            Map<String, Object> map = new HashMap<>();
            boolean[] states = taskManager.getRunningStates();
            if (states == null) {
                for (int i = 1; i <= 5; i++) {
                    map.put("source_" + i, "未知");
                }
                return result("success", map);
            }
            for (int i = 1; i <= states.length; i++) {
                map.put("source_" + i, states[i - 1] ? "运行中" : "已停止");
            }
            return result("success", map);
        }
        if (source < 1 || source > 5) {
            return result("error", "source 必须为 1..5");
        }
        boolean running = taskManager.isRunning(source - 1);
        Map<String, Object> single = new HashMap<>();
        single.put("source_" + source, running ? "运行中" : "已停止");
        return result("success", single);
    }

    // 读取指定表的数据（source 1..5），不传 source 返回全部表的数据；limit 可选，默认 100
    @GetMapping("/read")
    public Map<String, Object> read(Integer source, Integer limit) {
        if (limit == null || limit <= 0) limit = 1;
        if (source == null) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                List<SourceData> list = taskManager.readLatest(i - 1, limit);
                map.put("source_" + i, list);
            }
            return result("success", map);
        }
        if (source < 1 || source > 5) {
            return result("error", "source 必须为 1..5");
        }
        List<SourceData> list = taskManager.readLatest(source - 1, limit);
        return result("success", list);
    }

    private Map<String, Object> result(String status, Object msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("msg", msg);
        return map;
    }
}