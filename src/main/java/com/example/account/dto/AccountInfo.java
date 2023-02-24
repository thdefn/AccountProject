package com.example.account.dto;

import lombok.*;

/**
 * dto와 달리 클라이언트와 컨트롤러와 간에 데이터를 주고받을 때 사용
 * 왜 AccountDto 가 있는데 굳이 따로 빼서 만들까 ?
 * 전용 dto를 만들지 않고 다목적 dto를 만들면 나중에 복잡한 상황이 생기고,
 * 그런 상황이 생기면 의도치 않은 동작을 해서 시스템 장애가 발생함
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountInfo {
    private String accountNumber;
    private Long balance;
}
