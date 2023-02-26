package com.example.account.service;

import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {
    private final RedissonClient redissonClient;

    public void lock(String accountNumber) {
        RLock lock = redissonClient.getLock(getLockKey(accountNumber));
        log.debug("Trying lock for accountNumber : {}", accountNumber);

        try {
            boolean isLock = lock.tryLock(1, 15, TimeUnit.SECONDS);
            if (!isLock) {
                log.error("======Lock acquisition failed=====");
                throw new AccountException(ErrorCode.ACCOUNT_TRANSACTION_LOCK);
            }
        }catch (AccountException e){ //AccountException(ErrorCode.ACCOUNT_TRANSACTION_LOCK) 은 다시 상위로 올려 GlobalExceptionHandler가 받는다
            throw e;
        }catch (Exception e) { // 그 외에 다른 에러는 에러만 찍는다
            log.error("Redis lock failed", e);
        }
    }

    public void unlock(String accountNumber) {
        log.debug("Unlock for accountNumber : {}", accountNumber);
        redissonClient.getLock(getLockKey(accountNumber)).unlock();
    }

    // 계좌 번호를 Lock의 키로 삼는다 private 메서드로 빼서 Lock key를 만들어준다는 것을 명시적으로 나타냄
    private String getLockKey(String accountNumber) {
        return "ACLK:" + accountNumber;
    }
}
