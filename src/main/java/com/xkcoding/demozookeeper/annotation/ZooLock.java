package com.xkcoding.demozookeeper.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author weepppp 2022/8/12 20:30
 *
 * 需要加锁的方法的Aop代理注解
 **/

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ZooLock {

    /**
     * 分布式锁的键
     * @return
     */
    String key();

    /**
     * 锁释放时间：默认5秒
     * @return
     */
    long timeout() default 5*1000;

    /**
     * 时间格式：默认为毫秒
     * @return
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
