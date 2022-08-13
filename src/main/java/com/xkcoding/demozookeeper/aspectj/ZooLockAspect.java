package com.xkcoding.demozookeeper.aspectj;

import com.xkcoding.demozookeeper.annotation.LockKeyParam;
import com.xkcoding.demozookeeper.annotation.ZooLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author weepppp 2022/8/12 20:38
 *
 * 使用Aop切面注解加分布式锁
 *
 **/

@Aspect
@Slf4j
@Component
public class ZooLockAspect {

    private final CuratorFramework zkClient;

    private static final String KEY_PREFIX = "DISTRIBUTED_LOCK_";

    private static final String KEY_SEPARATOR = "/";

    @Autowired
    public ZooLockAspect(CuratorFramework zkClient) {
        this.zkClient = zkClient;
    }

    /**
     * 切点
     */
    @Pointcut("@annotation(com.xkcoding.demozookeeper.annotation.ZooLock)")
    public void doLock() {

    }

    @Around("doLock()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        //getSignature()表示获取封装了署名信息的对象,在该对象中可以获取到目标方法名,所属类的Class等信息（被注解所定义过的）
        MethodSignature signature = (MethodSignature) point.getSignature();
        //signature.getMethod()获得当前被加了注解的所有方法
        Method method = signature.getMethod();
        //point.getArgs()获取带参方法的参数
        Object[] args = point.getArgs();
        ZooLock zooLock = method.getAnnotation(ZooLock.class);
        if (!StringUtils.hasText(zooLock.key())) {
            throw new RuntimeException("分布式锁键值不能为空");
        }
        String lockKey = buildLockKey(zooLock, method, args);
        /**
         * InterProcessMutex(CuratorFramework client, String path):
         * client : curator中zk客户端对象
         *  path  : 抢锁路径，同一个锁path需一致
         */
        InterProcessMutex lock = new InterProcessMutex(zkClient, lockKey);
        try {
            if (lock.acquire(zooLock.timeout(),zooLock.timeUnit())){
                return point.proceed();
            } else {
                throw new RuntimeException(("请勿重复提交"));
            }
        } finally {
            lock.release();
        }
    }

    /**
     * 通过方法参数是对象还是非对象 动态构建分布式锁的键
     * @param lock
     * @param method
     * @param args
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private String buildLockKey(ZooLock lock, Method method, Object[] args) throws NoSuchFieldException, IllegalAccessException {
        StringBuilder key = new StringBuilder(KEY_SEPARATOR + KEY_PREFIX + lock.key());
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (!annotation.annotationType().isInstance(LockKeyParam.class)) {
                    continue;
                }
                String[] fields = ((LockKeyParam) annotation).field();
                if (StringUtils.hasText(Arrays.toString(fields))) {
                    // @LockKeyParam的fields值为null，说明动态key不在参数对象中 ，也就是说，当前参数不是对象类型
                    if (ObjectUtils.isEmpty(args[i])) {
                        throw new RuntimeException(("动态参数不能为null"));
                    }
                    key.append(KEY_SEPARATOR).append(args[i]);
                } else {
                    // @LockKeyParam的fields值不为null，所以当前参数应该是对象类型
                    for (String field : fields) {
                        Class<? extends Object[]> aClass = args.getClass();
                        Field declaredField = aClass.getDeclaredField(field);
                        declaredField.setAccessible(true);
                        //得到该注解的值 在对象类中对应属性字段的value值
                        Object value = declaredField.get(aClass);
                        key.append(KEY_SEPARATOR).append(value);
                    }
                }
            }
        }
        return key.toString();
    }


}
