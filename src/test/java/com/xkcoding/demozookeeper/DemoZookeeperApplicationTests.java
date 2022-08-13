package com.xkcoding.demozookeeper;

import com.xkcoding.demozookeeper.annotation.ZooLock;
import com.xkcoding.demozookeeper.aspectj.ZooLockAspect;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import sun.reflect.generics.tree.VoidDescriptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@SpringBootTest
@Slf4j
class DemoZookeeperApplicationTests {


    @Autowired
    private CuratorFramework zkClient;

    private Integer count = 100;
    ExecutorService executorService = Executors.newFixedThreadPool(100);

    public Integer getCount() {
        return count;
    }

    /**
     * 不使用分布式锁
     */
    @Test
    public void test() throws InterruptedException {
        IntStream.range(0, 10000).forEach(i -> executorService.execute(this::toBuy));
        TimeUnit.MILLISECONDS.sleep(100);
        System.out.println(count);
    }


    /**
     * 测试AOP分布式锁
     */
    @Test
    public void testAopLock() throws InterruptedException {
        // 测试类中使用AOP需要手动代理
        AspectJProxyFactory factory = new AspectJProxyFactory();
        //设置被代理的目标对象
        factory.setTarget(new DemoZookeeperApplicationTests());
        //设置标注了@Aspect注解的类
        ZooLockAspect zooLockAspect = new ZooLockAspect(zkClient);
        factory.addAspect(zooLockAspect);
        //生成代理对象
        DemoZookeeperApplicationTests proxy = factory.getProxy();
        IntStream.range(0, 100).forEach(i -> executorService.execute(() -> proxy.aopBuy(i)));
        TimeUnit.MINUTES.sleep(1);
        System.out.println(count);
        log.error("count值为{}", proxy.getCount());

    }

    @ZooLock(key = "buy", timeout = 1, timeUnit = TimeUnit.MINUTES)
    public void aopBuy(int userId) {
        log.info("{} 正在出库。。。", userId);
        toBuy();
        log.info("{} 扣库存成功。。。", userId);
    }

    public void toBuy() {
        count--;
    }

    /**
     * 测试手动加锁
     */
    @Test
    public void testManualLock() throws InterruptedException {
        IntStream.range(0, 100).forEach(i -> executorService.execute(this::manualBuy));
        TimeUnit.MINUTES.sleep(1);
        log.error("count值为{}", count);
    }

    private void manualBuy() {
        String lockPath = "/buy";
        log.info("try to buy sth.");
        try {
            InterProcessMutex lock = new InterProcessMutex(zkClient, lockPath);
            try {
                if (lock.acquire(1, TimeUnit.MINUTES)) {
                    toBuy();
                    log.info("buy successfully!");
                }
            } finally {
                lock.release();
            }
        } catch (Exception e) {
            log.error("zk error");
        }
    }
}



