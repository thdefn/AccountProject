package com.example.account.controller;

import com.example.account.dto.CancleBalance;
import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 잔액 관련 컨트롤러
 * 1. 잔액 사용
 * 2. 잔액 사용 취소
 * 3. 거래 확인
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transaction/use")
    public UseBalance.Response useBalance(
            @Valid @RequestBody UseBalance.Request request
    ) {
        try {
            return UseBalance.Response.from(
                    transactionService.useBalance(request.getUserId(),
                            request.getAccountNumber(), request.getAmount()));
        } catch (AccountException e) {
            // 비즈니스적으로 의도적으로 만들어 둔 익셉션이 발생했을 때
            log.error("Failed to use balance. ");
            transactionService.saveFailedUseTransaction( //트랜잭션 테이블에 실패 부분을 남긴다
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw e; //처리 후 담은 내용을 컨트롤러에서 다시 던져 클라이언트에 알려줌
        }
    }

    @PostMapping("/transaction/cancle")
    public CancleBalance.Response cancleBalance(
            @Valid @RequestBody CancleBalance.Request request
    ) {
        try {
            return CancleBalance.Response.from(
                    transactionService.cancleBalance(request.getTransactionId(),
                            request.getAccountNumber(), request.getAmount()));
        } catch (AccountException e) {
            log.error("Failed to use balance. ");
            transactionService.saveFailedCancleTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw e;
        }
    }

}
