package com.example.account.service;

import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.type.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {
    @Mock
    private LockService lockService;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    private LockAopAspect lockAopAspect;

    @Test
    void lockAndUnlock() throws Throwable {
        //given
        ArgumentCaptor<String> lockArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unlockArgumentCaptor = ArgumentCaptor.forClass(String.class);
        UseBalance.Request request =
                new UseBalance.Request(123L, "12345", 1000L);
        //when
        lockAopAspect.aroundMethod(proceedingJoinPoint, request);
        //then
        verify(lockService, times(1)).lock(lockArgumentCaptor.capture()); // lockService.lock() 이 호출이 잘되는지 확인
        verify(lockService, times(1)).unlock(unlockArgumentCaptor.capture()); // lockService.unlock() 이 호출이 잘되는지 확인
        assertEquals("12345",lockArgumentCaptor.getValue());
        assertEquals("12345",unlockArgumentCaptor.getValue());
    }

    @Test
    void lockAndUnlock_evenIfThrow() throws Throwable { // 원래 메소드에서 AccountException이 던져질때도 unlock이 이뤄지는지
        //given
        ArgumentCaptor<String> unlockArgumentCaptor = ArgumentCaptor.forClass(String.class);
        UseBalance.Request request =
                new UseBalance.Request(123L, "54322", 1000L);
        given(proceedingJoinPoint.proceed())
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        //when
        assertThrows(AccountException.class, //lockAopAspect에서 발생한 익셉션을 잡아줌
                ()->lockAopAspect.aroundMethod(proceedingJoinPoint, request));
        //then
        verify(lockService, times(1)).unlock(unlockArgumentCaptor.capture()); // lockService.unlock() 이 호출이 잘되는지 확인
        assertEquals("54322",unlockArgumentCaptor.getValue());
    }
}