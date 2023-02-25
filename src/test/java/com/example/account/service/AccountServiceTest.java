package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("계좌 생성 성공")
    void successCreateAccount() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(
                        Account.builder()
                                .accountNumber("1000000012").build() //의미 있는 필드인 accountNumber만
                ));

        given(accountRepository.save(any()))
                .willReturn(
                        Account.builder()
                                .accountUser(user)
                                .accountNumber("임의의계좌번호").build()
                );

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 100L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("기존에 생성된 계좌 없음 - 계좌 생성 성공")
    void successCreateFirstAccount() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());

        given(accountRepository.save(any()))
                .willReturn(
                        Account.builder()
                                .accountUser(user)
                                .accountNumber("임의의계좌번호").build()
                );

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class); //실제로 저장되는 Account가 들어감

        //when
        AccountDto accountDto = accountService.createAccount(1L, 100L);
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void failedCreateAccount_UserNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 100L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("유저가 10개 이상의 계좌를 갖고 있음 - 계좌 생성 실패")
    void failedCreateAccount_maxAccountIs10() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any())).willReturn(10);
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 100L));
        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 성공")
    void successDeleteAccount() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(user)
                                .balance(0L)
                                .accountStatus(AccountStatus.IN_USE)
                                .accountNumber("1000000012").build() //의미 있는 필드인 accountNumber만
                ));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1000000012");
        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void faileddeleteAccount_UserNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "임의의계좌번호"));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void failedDeleteAccount_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "임의의계좌번호"));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }


    @Test
    @DisplayName("계좌 소유주가 다름 - 계좌 해지 실패")
    void failedDeleteAccount_userUnmatch() {
        //given
        AccountUser pobi = AccountUser.builder().id(12L).name("Pobi").build();

        AccountUser harry = AccountUser.builder().id(13L).name("Harry").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(harry)
                                .balance(0L)
                                .accountStatus(AccountStatus.IN_USE)
                                .accountNumber("1000000012").build() //의미 있는 필드인 accountNumber만
                ));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "임의의계좌번호"));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UNMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌가 이미 해지된 상태인 경우- 계좌 해지 실패")
    void failedDeleteAccount_aleadyUnregistered() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(user)
                                .balance(100L)
                                .accountStatus(AccountStatus.UNREGISTERED)
                                .accountNumber("1000000012").build()
                ));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "임의의계좌번호"));
        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 잔액이 없어야 함 - 계좌 해지 실패")
    void failedDeleteAccount_balanceNotEmpty() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(user)
                                .balance(100L)
                                .accountStatus(AccountStatus.IN_USE)
                                .accountNumber("1000000012").build()
                ));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "임의의계좌번호"));
        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    void successGetAccountsByUserId() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountUser(user)
                        .balance(100L)
                        .accountNumber("1000000012")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build(),
                Account.builder()
                        .accountUser(user)
                        .balance(1000L)
                        .accountNumber("1000000013")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build(),
                Account.builder()
                        .accountUser(user)
                        .balance(2000L)
                        .accountNumber("1000000014")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build()
        );
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);
        //when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(12L);
        //then
        assertEquals(3, accountDtos.size());
        assertEquals("1000000012", accountDtos.get(0).getAccountNumber());
        assertEquals(100L, accountDtos.get(0).getBalance());
        assertEquals("1000000013", accountDtos.get(1).getAccountNumber());
        assertEquals(1000L, accountDtos.get(1).getBalance());
        assertEquals("1000000014", accountDtos.get(2).getAccountNumber());
        assertEquals(2000L, accountDtos.get(2).getBalance());
    }

    @Test
    void failedGetAccountsByUserId() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(12L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

}