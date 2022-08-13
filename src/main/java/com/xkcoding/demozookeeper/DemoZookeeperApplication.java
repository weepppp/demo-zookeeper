package com.xkcoding.demozookeeper;

import com.xkcoding.demozookeeper.aspectj.ZooLockAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoZookeeperApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoZookeeperApplication.class, args);
    }

}
