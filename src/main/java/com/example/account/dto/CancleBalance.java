package com.example.account.dto;

import com.example.account.aop.AccountLockIdInterface;
import com.example.account.type.TransactionResultType;
import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDateTime;
// UseBalance 와 같다고 해도 나중에 달라질 수 있고 또 추후 혼동이 될 수 있음 > 지금 당장은 동일해도 의도적의로 분리하자
public class CancleBalance {
    @Getter
    @Setter
    @AllArgsConstructor
    public static class Request implements AccountLockIdInterface { //이너 클래스 활용
       @NotBlank
       private String transactionId;

       @NotBlank
       @Size(min = 10, max = 10)
       private String accountNumber;

       @NotNull
       @Min(10)
       @Max(1000_000_000)
       private Long amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String accountNumber;
        private TransactionResultType transactionResult;
        private String transactionId;
        private Long amount;
        private LocalDateTime transactedAt;

        public static Response from(TransactionDto transactionDto){
            return Response.builder()
                    .accountNumber(transactionDto.getAccountNumber())
                    .transactionResult(transactionDto.getTransactionResultType())
                    .transactionId(transactionDto.getTransactionId())
                    .amount(transactionDto.getAmount())
                    .transactedAt(transactionDto.getTransactedAt())
                    .build();
        }
    }
}
