package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.exception.TransactionException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
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
import java.util.Optional;

import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(
                        Transaction.builder()
                                .transactionType(USE)
                                .transactionResultType(S)
                                .account(account)
                                .amount(2000L)
                                .balanceSnapshot(18000L)
                                .transactionId("abcdefghikl")
                                .transactedAt(LocalDateTime.now())
                                .build()
                );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        TransactionDto transactionDto = transactionService
                .useBalance(1L, "111111111", 10000L);
        //then
        verify(transactionRepository, times(1)).save(captor.capture()); //captor야 캡쳐해
        assertEquals("1234567890", transactionDto.getAccountNumber());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(2000L, transactionDto.getAmount()); //레파지토리에 mocking한 값이 들어간다
        assertEquals(18000L, transactionDto.getBalanceSnapshot()); //레파지토리에 mocking한 값이 들어간다
        assertEquals(190000L, captor.getValue().getBalanceSnapshot()); //account 엔티티에 들어있던 값에서 amount를 뺀 만큼 나머지 잔액이 잘 들어 있는지
        assertEquals(10000L, captor.getValue().getAmount()); // 실제로 서비스에서 저장되는 시점의 값이 들어감
    }

    @Test
    @DisplayName("해당 유저 없음 - 잔액 사용 실패")
    void failedUseBalance_UserNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "임의의계좌번호", 1000L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
    void failedUseBalance_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "임의의계좌번호", 1000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주가 다름 - 잔액 사용 실패")
    void failedUseBalance_userUnmatch() {
        //given
        AccountUser pobi = AccountUser.builder().id(12L).name("Pobi").build();

        AccountUser harry = AccountUser.builder().id(13L).name("Harry").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(harry)
                                .balance(200000L)
                                .accountStatus(AccountStatus.IN_USE)
                                .accountNumber("1000000012").build() //의미 있는 필드인 accountNumber만
                ));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "임의의계좌번호", 1000L));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UNMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌가 이미 해지된 상태인 경우 - 잔액 사용 실패")
    void failedUseBalance_aleadyUnregistered() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(user)
                                .balance(200000L)
                                .accountStatus(AccountStatus.UNREGISTERED)
                                .accountNumber("1000000012").build()
                ));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "임의의계좌번호", 1000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우 - 잔액 사용 실패")
    void failedUseBalance_amountExceedBalance() {
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
                () -> transactionService.useBalance(1L, "임의의계좌번호", 1000L));
        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("사용 실패 트랜잭션 저장 성공")
    void successSaveFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(
                        Transaction.builder()
                                .transactionType(USE)
                                .transactionResultType(S)
                                .account(account)
                                .amount(2000L)
                                .balanceSnapshot(18000L)
                                .transactionId("abcdefghikl")
                                .transactedAt(LocalDateTime.now())
                                .build()
                );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        transactionService.saveFailedUseTransaction("1000000000", 10000L);
        //then
        verify(transactionRepository, times(1)).save(captor.capture()); //captor야 캡쳐해
        assertEquals(F, captor.getValue().getTransactionResultType());
        assertEquals(USE, captor.getValue().getTransactionType());
        assertEquals(200000L, captor.getValue().getBalanceSnapshot()); //account 엔티티에 들어있던 값에서 amount를 뺀 만큼 나머지 잔액이 잘 들어 있는지
        assertEquals(10000L, captor.getValue().getAmount()); // 실제로 서비스에서 저장되는 시점의 값이 들어감
    }

    @Test
    @DisplayName("사용 실패 트랜잭션 저장 성공")
    void failedSaveFailedUseTransaction_accountNotFound() {
        //given
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.saveFailedUseTransaction("1000000000", 10000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND,exception.getErrorCode());
    }

    @Test
    void successCancleBalance() {
        //given
        Account account = Account.builder()
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();

        //과거에 썼던 내역
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                        Transaction.builder()
                                .transactionType(USE)
                                .transactionResultType(S)
                                .account(account)
                                .amount(20000L)
                                .balanceSnapshot(18000L)
                                .transactionId("usedtransaction")
                                .transactedAt(LocalDateTime.now())
                                .build()
                ));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any())).willReturn(
                Transaction.builder()
                        .transactionType(CANCEL)
                        .transactionResultType(S)
                        .account(account)
                        .amount(1234L)
                        .balanceSnapshot(123456L)
                        .transactionId("transactionIdforCancle")
                        .transactedAt(LocalDateTime.now())
                        .build()
        );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService
                .cancleBalance("임의의uuid", "열자리계좌번호입니다", 20000L);
        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(220000L, captor.getValue().getBalanceSnapshot());
        assertEquals(20000L, captor.getValue().getAmount());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(123456L, transactionDto.getBalanceSnapshot());
        assertEquals("transactionIdforCancle", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("원 사용 거래 없음 - 잔액 사용 취소 실패")
    void failedCancleBalance_trasactionNotFound() {
        //given

        //과거에 썼던 내역
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        TransactionException exception = assertThrows(TransactionException.class,
                ()-> transactionService.cancleBalance(
                        "임의의uuid",
                        "열자리계좌번호입니다",
                        20000L));
        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당하는 계좌가 없는 경우 - 잔액 사용 취소 실패")
    void failedCancleBalance_accountNotFound() {
        //given
        //과거에 썼던 내역
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                        Transaction.builder()
                                .transactionType(USE)
                                .transactionResultType(S)
                                .amount(20000L)
                                .balanceSnapshot(18000L)
                                .transactionId("usedtransaction")
                                .transactedAt(LocalDateTime.now())
                                .build()
                ));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                ()-> transactionService.cancleBalance("임의의uuid", "열자리계좌번호입니다", 20000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌 매칭 실패 - 잔액 사용 취소 실패")
    void failedCancleBalance_transactionAccountUnmatch() {
        //given
        Account account = Account.builder()
                .accountNumber("123456778")
                .id(12L)
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();

        Account transactionAccount = Account.builder()
                .accountNumber("1234567890")
                .id(15L)
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();
        //과거에 썼던 내역
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                        Transaction.builder()
                                .transactionType(USE)
                                .transactionResultType(S)
                                .account(transactionAccount)
                                .amount(20000L)
                                .balanceSnapshot(18000L)
                                .transactionId("usedtransaction")
                                .transactedAt(LocalDateTime.now())
                                .build()
                ));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                ()-> transactionService.cancleBalance("임의의uuid", "열자리계좌번호입니다", 20000L));
        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UNMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액과 취소 금액이 다름 - 잔액 사용 취소 실패")
    void failedCancleBalance_cancleMustFully() {
        //given
        Account account = Account.builder()
                .accountNumber("123456778")
                .id(12L)
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();

        //과거에 썼던 내역
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                        Transaction.builder()
                                .transactionType(USE)
                                .transactionResultType(S)
                                .account(account)
                                .amount(15000L)
                                .balanceSnapshot(18000L)
                                .transactionId("usedtransaction")
                                .transactedAt(LocalDateTime.now())
                                .build()
                ));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        TransactionException exception = assertThrows(TransactionException.class,
                ()-> transactionService.cancleBalance(
                        "임의의uuid",
                        "열자리계좌번호입니다",
                        20000L));
        //then
        assertEquals(ErrorCode.CANCLE_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("취소는 1년까지만 가능 - 잔액 사용 취소 실패")
    void failedCancleBalance_tooOldOrdertoCancle() {
        //given
        Account account = Account.builder()
                .accountNumber("123456778")
                .id(12L)
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();

        //과거에 썼던 내역
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                        Transaction.builder()
                                .transactionType(USE)
                                .transactionResultType(S)
                                .account(account)
                                .amount(15000L)
                                .balanceSnapshot(18000L)
                                .transactionId("usedtransaction")
                                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                                .build()
                ));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        TransactionException exception = assertThrows(TransactionException.class,
                ()-> transactionService.cancleBalance(
                        "임의의uuid",
                        "열자리계좌번호입니다",
                        15000L));
        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("취소 실패 트랜잭션 저장 성공")
    void successSaveFailedCancleTransaction() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountNumber("1234567890")
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(
                        Transaction.builder()
                                .transactionType(CANCEL)
                                .transactionResultType(F)
                                .account(account)
                                .amount(2000L)
                                .balanceSnapshot(18000L)
                                .transactionId("abcdefghikl")
                                .transactedAt(LocalDateTime.now())
                                .build()
                );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        transactionService.saveFailedCancleTransaction("1000000000", 10000L);
        //then
        verify(transactionRepository, times(1)).save(captor.capture()); //captor야 캡쳐해
        assertEquals(F, captor.getValue().getTransactionResultType());
        assertEquals(CANCEL, captor.getValue().getTransactionType());
        assertEquals(200000L, captor.getValue().getBalanceSnapshot()); //account 엔티티에 들어있던 값에서 amount를 뺀 만큼 나머지 잔액이 잘 들어 있는지
        assertEquals(10000L, captor.getValue().getAmount()); // 실제로 서비스에서 저장되는 시점의 값이 들어감
    }

    @Test
    @DisplayName("취소 실패 트랜잭션 저장 성공")
    void failedSaveFailedCancleTransaction_accountNotFound() {
        //given
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.saveFailedCancleTransaction("1000000000", 10000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND,exception.getErrorCode());
    }

}