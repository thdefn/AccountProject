package com.example.account.service;

import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {
    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock; // 우리가 만든 bean은 아니지만 RLock의 동작에 따라 로직이 변화하기 때문에 mocking해서 동작을 우리가 원하는대로 바꿔주자

    @InjectMocks
    private LockService lockService;

    @Test
    void successGetLock() throws InterruptedException {
        //given
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock); // 이제 RLock 이 우리가 원하는 mocking 동작을 할 수 있다
        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(true);
        //when
        //then
        assertDoesNotThrow(()->lockService.lock("123")); // lock을 획득하면 아무것도 안하기 때문에 성공할것
    }

    @Test
    void failedGetLock() throws InterruptedException {
        //given
        given(redissonClient.getLock(anyString()))
                .willReturn(rLock); // 이제 RLock 이 우리가 원하는 mocking 동작을 할 수 있다
        given(rLock.tryLock(anyLong(), anyLong(), any()))
                .willReturn(false);
        //when
        AccountException exception =
                assertThrows(AccountException.class, ()->lockService.lock("123")); // lock을 획득실패하면 AccountException이 던져진다
        //then
        assertEquals(ErrorCode.ACCOUNT_TRANSACTION_LOCK,exception.getErrorCode());
    }
}