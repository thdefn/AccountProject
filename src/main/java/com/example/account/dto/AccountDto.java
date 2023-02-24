package com.example.account.dto;

import com.example.account.domain.Account;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 컨트롤러와 서비스 간에 데이터를 주고받는 데 최적화된 dto
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDto { // 엔티티 클래스와 비슷한데 단순화된 버전으로 딱 필요한 것만
    private Long userId;
    private String accountNumber;
    private Long balance;
    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    public static AccountDto fromEntity(Account account){
        // 생성자를 쓰는 것도 좋지만 dto는 엔티티를 가지고 가장 많이 만들어지기 때문에 static한 메서드를 만들어주는 게 훨씬 안전하고 가독성도 좋다
        return AccountDto.builder()
                .userId(account.getAccountUser().getId())
                .accountNumber(account.getAccountNumber())
                .registeredAt(account.getRegisteredAt())
                .unRegisteredAt(account.getUnRegisteredAt())
                .balance(account.getBalance())
                .build();
    }
}
