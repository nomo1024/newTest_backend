package com.lyc.usercenter.service.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MqttPublishTest {
    public static void main(String[] args) {
        // 默认配置
        String broker = "ssl://ncabc26b.ala.cn-hangzhou.emqxsl.cn:8883";
        String topic = "testtopic/1";
        String message = "Hello MQTT " + System.currentTimeMillis();
        int qos = 1;
        String username = "admin";
        String password = "li885201.";

        // 解析命令行参数
        if (args.length > 0) topic = args[0];
        if (args.length > 1) message = args[1];
        if (args.length > 2) qos = Integer.parseInt(args[2]);

        try {
            String clientId = MqttClient.generateClientId();
            MemoryPersistence persistence = new MemoryPersistence();
            
            MqttClient client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            
            if (username != null) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }

            // SSL 配置
            if (broker.startsWith("ssl://")) {
                try {
                    InputStream caCrtStream = MqttPublishTest.class.getResourceAsStream("/ssl/broker.emqx.io-ca.crt");
                    if (caCrtStream != null) {
                        Path tempCaCrt = Files.createTempFile("ca", ".crt");
                        Files.copy(caCrtStream, tempCaCrt, StandardCopyOption.REPLACE_EXISTING);
                        options.setSocketFactory(com.lyc.usercenter.service.mqtt.SSLUtils.getSingleSocketFactory(tempCaCrt.toString()));
                        System.out.println("SSL 配置成功");
                    }
                } catch (Exception e) {
                    System.err.println("SSL 配置失败: " + e.getMessage());
                }
            }

            System.out.println("Connecting to broker: " + broker);
            client.connect(options);
            System.out.println("Connected!");

            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(qos);
            
            client.publish(topic, mqttMessage);
            System.out.println("Message published:");
            System.out.println("  Topic: " + topic);
            System.out.println("  Message: " + message);
            System.out.println("  QoS: " + qos);

            client.disconnect();
            System.out.println("Disconnected");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
