package com.example.account.controller;

import com.example.account.dto.AccountDto;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.exception.AccountException;
import com.example.account.service.AccountService;
import com.example.account.type.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {
    @MockBean
    private AccountService accountService;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successCreateAccount() throws Exception {
        //given
        given(accountService.createAccount(anyLong(), anyLong()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .balance(10000L)
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        //when
        //then
        mockMvc.perform(post("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAccount.Request(3333L, 101010L)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());

    }

    @Test
    void successDeleteAccount() throws Exception {
        //given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .balance(10000L)
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        //when
        //then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeleteAccount.Request(3333L, "1231231231")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());

    }

    @Test
    void getAccountsByUserIdSuccess() throws Exception {
        //given
        given(accountService.getAccountsByUserId(anyLong()))
                .willReturn(List.of(
                        AccountDto.builder()
                                .userId(1L)
                                .accountNumber("1231231231")
                                .balance(1000L)
                                .build(),
                        AccountDto.builder()
                                .userId(1L)
                                .accountNumber("1234567890")
                                .balance(2000L)
                                .build(),
                        AccountDto.builder()
                                .userId(1L)
                                .accountNumber("2234567890")
                                .balance(3000L)
                                .build()
                ));
        //when
        //then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$[0].accountNumber")
                        .value("1231231231"))
                .andExpect(jsonPath("$[1].accountNumber")
                        .value("1234567890"))
                .andExpect(jsonPath("$[2].accountNumber")
                        .value("2234567890"))
                .andExpect(jsonPath("$[0].balance")
                        .value(1000L))
                .andExpect(jsonPath("$[1].balance")
                        .value(2000L))
                .andExpect(jsonPath("$[2].balance")
                        .value(3000L));
    }

    @Test
    void getAccountFail() throws Exception {
        //given
        given(accountService.getAccountsByUserId(anyLong()))
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        //when
        //then
        mockMvc.perform(get("/account?user_id=876"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"));

    }
}