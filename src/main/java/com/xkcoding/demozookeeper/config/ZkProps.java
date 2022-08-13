package com.xkcoding.demozookeeper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author weepppp 2022/8/13 10:34
 *
 * Zookeeper配置类
 **/
@Data
@ConfigurationProperties(prefix = "zk")
public class ZkProps {
    private String url;
    private int timeout = 1000;
    private int retry = 3;
}
