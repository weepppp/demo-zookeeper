package com.xkcoding.demozookeeper.annotation;

import java.lang.annotation.*;

/**
 * @author weepppp 2022/8/12 20:22
 * <p>
 * 分布式锁动态key注解，配置之后key的值会动态获取参数内容？
 **/

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LockKeyParam {

    /**
     * ex:如果动态key在user对象中，那么就需要设置fields的值为user对象中的属性名可以为可能的所有值，基本类型则不需要设置该值
     * public void count(@LockKeyParam{"id"} USer user)
     * public void count(@LockKeyParam{{"id","name"}} USer user)
     */
    String[] field() default {};
}
