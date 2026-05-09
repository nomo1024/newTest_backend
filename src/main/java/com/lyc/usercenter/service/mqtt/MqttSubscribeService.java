package com.lyc.usercenter.service.mqtt;

import com.lyc.usercenter.config.MqttConfig;
import com.lyc.usercenter.service.mqtt.SSLUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * MQTT 订阅服务 - 独立模块
 * 负责 MQTT 连接、订阅、消息接收
 * 支持启停控制，订阅5个传感器主题
 */
@Component
public class MqttSubscribeService {

    @Autowired
    private MqttConfig mqttConfig;

    private MqttClient mqttClient;
    
    // 消息处理器接口（接收主题和消息内容）
    public interface MessageHandler {
        void handle(String topic, String payload);
    }
    
    private MessageHandler messageHandler;
    
    // 已订阅的主题集合
    private java.util.Set<String> subscribedTopics = new java.util.HashSet<>();

    /**
     * 启动 MQTT 订阅（连接并订阅指定主题）
     * @param topic 要订阅的主题，为null则订阅默认主题
     */
    public void start(String topic) {
        if (mqttClient != null && mqttClient.isConnected()) {
            // 已连接，只订阅新主题
            if (topic != null && !subscribedTopics.contains(topic)) {
                subscribeTopic(topic);
            }
            return;
        }
        // 未连接，先连接
        connectAndSubscribe(topic);
    }

    /**
     * 停止 MQTT 订阅（取消订阅指定主题，如果所有主题都取消则断开连接）
     * @param topic 要取消订阅的主题，为null则取消所有订阅并断开连接
     */
    public void stop(String topic) {
        try {
            if (topic == null) {
                // 取消所有订阅并断开连接
                if (mqttClient != null && mqttClient.isConnected()) {
                    mqttClient.disconnect();
                    subscribedTopics.clear();
                    System.out.println("✅ MQTT 已停止所有订阅");
                }
            } else {
                // 只取消订阅指定主题
                if (mqttClient != null && mqttClient.isConnected()) {
                    mqttClient.unsubscribe(topic);
                    subscribedTopics.remove(topic);
                    System.out.println("✅ 已取消订阅: " + topic);
                    
                    // 如果没有订阅的主题了，断开连接
                    if (subscribedTopics.isEmpty()) {
                        mqttClient.disconnect();
                        System.out.println("✅ MQTT 已断开连接（无订阅主题）");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("停止 MQTT 失败: " + e.getMessage());
        }
    }

    /**
     * 订阅单个主题
     */
    private void subscribeTopic(String topic) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topic, 1);
                subscribedTopics.add(topic);
                System.out.println("✅ 已订阅主题: " + topic);
            }
        } catch (Exception e) {
            System.err.println("订阅主题失败 " + topic + ": " + e.getMessage());
        }
    }

    /**
     * 连接 MQTT 并订阅指定主题
     * @param topic 要订阅的主题，为null则不订阅
     */
    private void connectAndSubscribe(String topic) {
        try {
            String broker = mqttConfig.getHost();
            String clientId = mqttConfig.getClientId();
            if (clientId == null || clientId.isEmpty()) {
                clientId = MqttClient.generateClientId();
            }
            
            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);

            // 设置用户名和密码
            if (mqttConfig.getUsername() != null && !mqttConfig.getUsername().isEmpty()) {
                options.setUserName(mqttConfig.getUsername());
                options.setPassword(mqttConfig.getPassword().toCharArray());
            }

            // SSL 配置
            if (broker.startsWith("ssl://")) {
                try {
                    InputStream caCrtStream = MqttSubscribeService.class.getResourceAsStream("/ssl/broker.emqx.io-ca.crt");
                    if (caCrtStream != null) {
                        Path tempCaCrt = Files.createTempFile("ca", ".crt");
                        Files.copy(caCrtStream, tempCaCrt, StandardCopyOption.REPLACE_EXISTING);
                        options.setSocketFactory(SSLUtils.getSingleSocketFactory(tempCaCrt.toString()));
                        System.out.println("SSL 配置成功");
                    } else {
                        System.err.println("SSL 证书文件未找到: /ssl/broker.emqx.io-ca.crt");
                    }
                } catch (Exception e) {
                    System.err.println("SSL 配置失败: " + e.getMessage());
                }
            }

            // 设置回调
            mqttClient.setCallback(new MqttCallback() {
                public void connectionLost(Throwable cause) {
                    System.err.println("MQTT 连接丢失: " + cause.getMessage());
                }

                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("收到 MQTT 消息 [" + topic + "]: " + payload);
                    if (messageHandler != null) {
                        messageHandler.handle(topic, payload);
                    }
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            mqttClient.connect(options);
            
            // 只订阅指定主题
            if (topic != null && !topic.isEmpty()) {
                mqttClient.subscribe(topic, 1);
                subscribedTopics.add(topic);
                System.out.println("✅ MQTT 已订阅主题: " + topic);
            }
            
        } catch (Exception e) {
            System.err.println("MQTT 启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置消息处理器（接收主题和消息内容）
     */
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }
}
