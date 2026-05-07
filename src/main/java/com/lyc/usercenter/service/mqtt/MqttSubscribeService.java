package com.lyc.usercenter.service.mqtt;

import com.lyc.usercenter.config.MqttConfig;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

/**
 * MQTT 订阅服务 - 独立模块
 * 负责 MQTT 连接、订阅、消息接收
 */
@Component
public class MqttSubscribeService {

    @Autowired
    private MqttConfig mqttConfig;

    private MqttClient mqttClient;

    // 消息处理器（由外部设置）
    private Consumer<String> messageHandler;

    @PostConstruct
    public void init() {
        connectAndSubscribe();
    }

    /**
     * 连接 MQTT 并订阅主题
     */
    private void connectAndSubscribe() {
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
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("MQTT 连接丢失: " + cause.getMessage());
                    // 尝试重连
                    try {
                        Thread.sleep(3000);
                        mqttClient.reconnect();
                    } catch (Exception e) {
                        System.err.println("MQTT 重连失败: " + e.getMessage());
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("收到 MQTT 消息 [" + topic + "]: " + payload);
                    if (messageHandler != null) {
                        messageHandler.accept(payload);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            mqttClient.connect(options);

            String topic = mqttConfig.getDefaultTopic();
            if (topic == null || topic.isEmpty()) {
                topic = "testtopic/1";
            }
            mqttClient.subscribe(topic, 1);
            System.out.println("✅ MQTT 订阅成功: " + topic);

        } catch (Exception e) {
            System.err.println("MQTT 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置消息处理器
     */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    /**
     * 发布消息
     */
    public void publish(String topic, String message, int qos) {
        try {
            if (!mqttClient.isConnected()) {
                mqttClient.reconnect();
            }
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(qos);
            mqttClient.publish(topic, mqttMessage);
            System.out.println("消息已发布到 " + topic + ": " + message);
        } catch (MqttException e) {
            System.err.println("发布消息失败: " + e.getMessage());
        }
    }

    /**
     * 订阅主题
     */
    public void subscribe(String topic, int qos) {
        try {
            mqttClient.subscribe(topic, qos);
            System.out.println("已订阅主题: " + topic);
        } catch (MqttException e) {
            System.err.println("订阅主题失败: " + e.getMessage());
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                System.out.println("MQTT 已断开连接");
            }
        } catch (MqttException e) {
            System.err.println("断开 MQTT 连接失败: " + e.getMessage());
        }
    }
}
