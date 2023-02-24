package com.example.account.controller;

import com.example.account.dto.AccountDto;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.service.AccountService;
import com.example.account.service.RedisTestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {
    // mock bean을 등록해서 우리가 원하는 동작으로 동작하도록 mocking
    // mocking 된 bean들이 진짜 AccountController와 테스트 컨테이너에 올라감
    // mock bean들이 AccountController 안으로 주입됨 -> 주입된 애플리케이션 상대로 mockMvc가 요청을 안쪽으로 날려서 테스트
    @MockBean
    private AccountService accountService;

    @MockBean
    private RedisTestService redisTestService;

    @Autowired
    private MockMvc mockMvc; // 테스트 컨테이너 안에 자동으로 생성되어 주입받을 수 있음

    @Autowired
    private ObjectMapper objectMapper; //json <-> object 상호 변환시켜줌

    @Test
    void createAccountSuccess() throws Exception {
        //given
        given(accountService.createAccount(anyLong(), anyLong()))
                .willReturn(AccountDto.builder()
                        .userId(123L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now()) //실제 서비스와 동일하게 리턴하도록 unRegisteredAt도 담아줌
                        .build()
                );
        //when
        //then
        mockMvc.perform(post("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAccount.Request(1L, 100L) // request는 어떤 값이어도 상관 없음
                        ))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(123))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"));
    }

    @Test
    void deleteAccountSuccess() throws Exception {
        //given
        LocalDateTime current = LocalDateTime.now();
        //delete account에 대한 mocking
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(AccountDto.builder()
                        .userId(23L)
                        .accountNumber("1234567890")
                        .registeredAt(current)
                        .unRegisteredAt(current)
                        .build()
                );
        //when
        //then
        mockMvc.perform(delete("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new DeleteAccount.Request(1L,"임의의열자리계좌번호")
                ))
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(23))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.unRegisteredAt").value(current.toString()));
    }
}