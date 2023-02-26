package com.example.account.service;

import com.example.account.aop.AccountLockIdInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {

    private final LockService lockService;

    /**
     * Around는 진행하고 있던 ProceedingJoinPoint를 그대로 가져와서 point before / after 을 모두 둘러싸면서 동작한다
     * aspectj 문법 중 args(request)는 특정 어노테이션을 붙인 메서드에서 request라는 파라미터를 가져오는 것
     */
    @Around("@annotation(com.example.account.aop.AccountLock) && args(request)") // 어떤 경우에 Aspect를 적용할건지
    public Object aroundMethod(
            ProceedingJoinPoint pjp,
            AccountLockIdInterface request // 공통화된 인터페이스로 가져올 수 있음
            //UseBalance.Request request //CancleBalance 에서 동작이 안된다
    ) throws Throwable {
        // lock 취득 시도
        lockService.lock(request.getAccountNumber()); // 1. 락을 취득해서
        try {
            return pjp.proceed(); // 이 어노테이션이 달린 메서드가 실행되는 부분, 2. 로직을 돌리고
        } finally {
            // lock 해제, 메서드가 정상적으로 진행되던 exception 발생하던 락을 해제
            lockService.unlock(request.getAccountNumber()); // 3. 락을 해제한다
        }
    }

}
