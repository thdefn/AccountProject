package com.example.account.controller;

import com.example.account.dto.AccountDto;
import com.example.account.dto.CancleBalance;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.service.TransactionService;
import com.example.account.type.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.example.account.type.TransactionResultType.S;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    @MockBean //가짜로 빈을 만들어서 TransactionController에 주입
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successUseBalance() throws Exception {
        //given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1234567890")
                        .transactionType(TransactionType.USE)
                        .transactionResultType(S)
                        .amount(10L)
                        .balanceSnapshot(990L)
                        .transactionId("abcdefghijklmnthisisuuidrandomuuid")
                        .transactedAt(LocalDateTime.of(1999,8,14,9,18, 12))
                        .build());
        //when
        //then
        mockMvc.perform(post("/transaction/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UseBalance.Request(2L, "임의의계좌번호열자리", 100L)
                        )))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("abcdefghijklmnthisisuuidrandomuuid"))
                .andExpect(jsonPath("$.amount").value(10))
                .andExpect(jsonPath("$.transactedAt").value(
                        LocalDateTime.of(1999,8,14,9,18, 12).toString()));
    }

    @Test
    void successCancleBalance() throws Exception {
        //given
        given(transactionService.cancleBalance(anyString(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1234567890")
                        .transactionType(TransactionType.CANCEL)
                        .transactionResultType(S)
                        .amount(10L)
                        .balanceSnapshot(990L)
                        .transactionId("abcdefghijklmnthisisuuidrandomuuid")
                        .transactedAt(LocalDateTime.of(1999,8,14,9,18, 12))
                        .build());
        //when
        //then
        mockMvc.perform(post("/transaction/cancle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancleBalance.Request("uuidrandomuuid", "임의의계좌번호열자리", 100L)
                        )))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("abcdefghijklmnthisisuuidrandomuuid"))
                .andExpect(jsonPath("$.amount").value(10))
                .andExpect(jsonPath("$.transactedAt").value(
                        LocalDateTime.of(1999,8,14,9,18, 12).toString()));
    }

    @Test
    void SuccessQueryTransaction() throws Exception {
        //given
        given(transactionService.queryTransaction(anyString()))
                .willReturn(
                        TransactionDto.builder()
                                .accountNumber("1234567890")
                                .transactionType(TransactionType.USE)
                                .transactionResultType(S)
                                .amount(10L)
                                .balanceSnapshot(990L)
                                .transactionId("abcdefghijklmnthisisuuidrandomuuid")
                                .transactedAt(LocalDateTime.of(1999,8,14,9,18, 12))
                                .build()
                );

        //when
        //then
        mockMvc.perform(get("/transaction/veryveryrandomuuid")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.transactionType").value("USE"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("abcdefghijklmnthisisuuidrandomuuid"))
                .andExpect(jsonPath("$.amount").value(10))
                .andExpect(jsonPath("$.transactedAt").value(
                        LocalDateTime.of(1999,8,14,9,18, 12).toString()));

    }
}