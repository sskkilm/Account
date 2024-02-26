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

import static org.junit.jupiter.api.Assertions.*;
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
    void lockAndUnLock() throws Throwable {
        //given
        ArgumentCaptor<String> lockArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unLockArgumentCaptor = ArgumentCaptor.forClass(String.class);
        UseBalance.Request request = UseBalance.Request.builder()
                .userId(123L)
                .accountNumber("1234")
                .amount(1000L)
                .build();

        //when
        lockAopAspect.aroundMethod(proceedingJoinPoint, request);

        //then
        verify(lockService, times(1))
                .lock(lockArgumentCaptor.capture());
        verify(lockService, times(1))
                .unLock(unLockArgumentCaptor.capture());
        assertEquals("1234", lockArgumentCaptor.getValue());
        assertEquals("1234", unLockArgumentCaptor.getValue());
    }

    @Test
    void lockAndUnLock_evenIfThrow() throws Throwable {
        //given
        ArgumentCaptor<String> lockArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unLockArgumentCaptor = ArgumentCaptor.forClass(String.class);
        UseBalance.Request request = UseBalance.Request.builder()
                .userId(123L)
                .accountNumber("54321")
                .amount(1000L)
                .build();
        given(proceedingJoinPoint.proceed())
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        //when
        assertThrows(AccountException.class,
                () -> lockAopAspect.aroundMethod(proceedingJoinPoint, request));



        //then
        verify(lockService, times(1))
                .lock(lockArgumentCaptor.capture());
        verify(lockService, times(1))
                .unLock(unLockArgumentCaptor.capture());
        assertEquals("54321", lockArgumentCaptor.getValue());
        assertEquals("54321", unLockArgumentCaptor.getValue());
    }

}