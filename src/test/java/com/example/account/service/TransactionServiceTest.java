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
        verify(transactionRepository, times(1)).save(captor.capture()); //captor??? ?????????
        assertEquals("1234567890", transactionDto.getAccountNumber());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(2000L, transactionDto.getAmount()); //?????????????????? mocking??? ?????? ????????????
        assertEquals(18000L, transactionDto.getBalanceSnapshot()); //?????????????????? mocking??? ?????? ????????????
        assertEquals(190000L, captor.getValue().getBalanceSnapshot()); //account ???????????? ???????????? ????????? amount??? ??? ?????? ????????? ????????? ??? ?????? ?????????
        assertEquals(10000L, captor.getValue().getAmount()); // ????????? ??????????????? ???????????? ????????? ?????? ?????????
    }

    @Test
    @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
    void failedUseBalance_UserNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "?????????????????????", 1000L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("?????? ?????? ?????? - ?????? ?????? ??????")
    void failedUseBalance_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder().id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "?????????????????????", 1000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("?????? ???????????? ?????? - ?????? ?????? ??????")
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
                                .accountNumber("1000000012").build() //?????? ?????? ????????? accountNumber???
                ));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "?????????????????????", 1000L));
        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UNMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("????????? ?????? ????????? ????????? ?????? - ?????? ?????? ??????")
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
                () -> transactionService.useBalance(1L, "?????????????????????", 1000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("?????? ????????? ???????????? ??? ?????? - ?????? ?????? ??????")
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
                () -> transactionService.useBalance(1L, "?????????????????????", 1000L));
        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("?????? ?????? ???????????? ?????? ??????")
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
        verify(transactionRepository, times(1)).save(captor.capture()); //captor??? ?????????
        assertEquals(F, captor.getValue().getTransactionResultType());
        assertEquals(USE, captor.getValue().getTransactionType());
        assertEquals(200000L, captor.getValue().getBalanceSnapshot()); //account ???????????? ???????????? ????????? amount??? ??? ?????? ????????? ????????? ??? ?????? ?????????
        assertEquals(10000L, captor.getValue().getAmount()); // ????????? ??????????????? ???????????? ????????? ?????? ?????????
    }

    @Test
    @DisplayName("?????? ?????? ???????????? ?????? ??????")
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

        //????????? ?????? ??????
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
                .cancleBalance("?????????uuid", "??????????????????????????????", 20000L);
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
    @DisplayName("??? ?????? ?????? ?????? - ?????? ?????? ?????? ??????")
    void failedCancleBalance_trasactionNotFound() {
        //given

        //????????? ?????? ??????
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        TransactionException exception = assertThrows(TransactionException.class,
                ()-> transactionService.cancleBalance(
                        "?????????uuid",
                        "??????????????????????????????",
                        20000L));
        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("???????????? ????????? ?????? ?????? - ?????? ?????? ?????? ??????")
    void failedCancleBalance_accountNotFound() {
        //given
        //????????? ?????? ??????
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
                ()-> transactionService.cancleBalance("?????????uuid", "??????????????????????????????", 20000L));
        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("????????? ?????? ?????? ?????? - ?????? ?????? ?????? ??????")
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
        //????????? ?????? ??????
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
                ()-> transactionService.cancleBalance("?????????uuid", "??????????????????????????????", 20000L));
        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UNMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("?????? ????????? ?????? ????????? ?????? - ?????? ?????? ?????? ??????")
    void failedCancleBalance_cancleMustFully() {
        //given
        Account account = Account.builder()
                .accountNumber("123456778")
                .id(12L)
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();

        //????????? ?????? ??????
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
                        "?????????uuid",
                        "??????????????????????????????",
                        20000L));
        //then
        assertEquals(ErrorCode.CANCLE_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("????????? 1???????????? ?????? - ?????? ?????? ?????? ??????")
    void failedCancleBalance_tooOldOrdertoCancle() {
        //given
        Account account = Account.builder()
                .accountNumber("123456778")
                .id(12L)
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();

        //????????? ?????? ??????
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
                        "?????????uuid",
                        "??????????????????????????????",
                        15000L));
        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("?????? ?????? ???????????? ?????? ??????")
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
        verify(transactionRepository, times(1)).save(captor.capture()); //captor??? ?????????
        assertEquals(F, captor.getValue().getTransactionResultType());
        assertEquals(CANCEL, captor.getValue().getTransactionType());
        assertEquals(200000L, captor.getValue().getBalanceSnapshot()); //account ???????????? ???????????? ????????? amount??? ??? ?????? ????????? ????????? ??? ?????? ?????????
        assertEquals(10000L, captor.getValue().getAmount()); // ????????? ??????????????? ???????????? ????????? ?????? ?????????
    }

    @Test
    @DisplayName("?????? ?????? ???????????? ?????? ??????")
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

    @Test
    void successQueryTransaction() {
        //given
        Account account = Account.builder()
                .accountNumber("123456778")
                .id(12L)
                .accountStatus(AccountStatus.IN_USE)
                .balance(200000L)
                .build();

        //????????? ?????? ??????
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
        //when
        TransactionDto transactionDto = transactionService.queryTransaction("abcd");
        //then
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals("123456778", transactionDto.getAccountNumber());
        assertEquals("usedtransaction", transactionDto.getTransactionId());
        assertEquals(18000L, transactionDto.getBalanceSnapshot());
    }

    @Test
    @DisplayName("??? ?????? ?????? ?????? - ?????? ?????? ??????")
    void failedQueryTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());
        //when
        TransactionException exception = assertThrows(TransactionException.class,
                ()->transactionService.queryTransaction("abcd"));
        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

}