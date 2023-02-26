package com.example.account.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // 어노테이션을 붙일 수 있는 타겟
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited // 상속 가능한 구조로 쓰겠다
public @interface AccountLock { //단순히 어노테이션만 만듦, 어노테이션이 붙었을 때 동작하는 부분은 따로 만들어줘야 함
    long tryLockTime() default 5000L; // 이 시간동안 기다리겠다
}
