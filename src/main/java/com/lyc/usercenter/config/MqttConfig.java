package com.lyc.usercenter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttConfig {
    private String host;
    private String username;
    private String password;
    private String clientId;
    private String defaultTopic;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDefaultTopic() {
        return defaultTopic;
    }

    public void setDefaultTopic(String defaultTopic) {
        this.defaultTopic = defaultTopic;
    }
}
