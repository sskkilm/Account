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
    void successCreateAccount() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi")
                .build();
        accountUser.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012")
                        .build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1000000015")
                        .build());

        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 10000L);

        //then
        verify(accountRepository, times(1)).save(accountArgumentCaptor.capture());
        assertEquals(1L, accountDto.getUserId());
        assertEquals("1000000015", accountDto.getAccountNumber());
        assertEquals("1000000013", accountArgumentCaptor.getValue().getAccountNumber());
    }

    @Test
    void createFirstAccount() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi")
                .build();
        accountUser.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1000000015")
                        .build());

        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 10000L);

        //then
        verify(accountRepository, times(1)).save(accountArgumentCaptor.capture());
        assertEquals(1L, accountDto.getUserId());
        assertEquals("1000000000", accountArgumentCaptor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 생성 실패")
    void createAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 10000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("유저당 최대 계좌는 10개")
    void createAccount_maxAccountIs10() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi")
                .build();
        accountUser.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 10000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, accountException.getErrorCode());
    }

    @Test
    void successDeleteAccount() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi")
                .build();
        accountUser.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1231231231")
                        .balance(0L)
                        .build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(accountUser)
                        .build());

        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        //then
        verify(accountRepository, times(1)).save(accountArgumentCaptor.capture());
        assertEquals(AccountStatus.UNREGISTERED, accountArgumentCaptor.getValue().getAccountStatus());
        assertEquals("1231231231", accountArgumentCaptor.getValue().getAccountNumber());
        assertEquals(1L, accountDto.getUserId());
    }

    @Test
    @DisplayName("해당 유저 없음 - 계좌 해지 실패")
    void deleteAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1231231231"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccount_AccountNotFound() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi")
                .build();
        accountUser.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1231231231"));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 계좌 해지 실패")
    void deleteAccount_UserAccountUnMatch() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi")
                .build();
        accountUser.setId(1L);
        AccountUser accountUser1 = AccountUser.builder()
                .name("Popo")
                .build();
        accountUser1.setId(2L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser1)
                        .accountNumber("1234567890")
                        .balance(0L)
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1231231231"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("이미 해지된 계좌 - 계좌 해지 실패")
    void deleteAccount_AccountAlreadyUnregistered() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi")
                .build();
        accountUser.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1234567890")
                        .balance(0L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1231231231"));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액이 남은 계좌 - 계좌 해지 실패")
    void deleteAccount_BalanceNotEmpty() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi")
                .build();
        accountUser.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1234567890")
                        .balance(1L)
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1231231231"));

        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, accountException.getErrorCode());
    }

    @Test
    void getAccountsByUserIdSuccess() {
        AccountUser accountUser = AccountUser.builder()
                .name("ho")
                .build();
        accountUser.setId(1L);
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountUser(any()))
                .willReturn(List.of(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountNumber("1231231231")
                                .balance(1000L)
                                .build(),
                        Account.builder()
                                .accountUser(accountUser)
                                .accountNumber("1122334455")
                                .balance(2000L)
                                .build(),
                        Account.builder()
                                .accountUser(accountUser)
                                .accountNumber("1112223331")
                                .balance(3000L)
                                .build()
                ));
        //when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);

        //then
        assertEquals(3, accountDtos.size());
        assertEquals("1231231231", accountDtos.get(0).getAccountNumber());
        assertEquals(1000L, accountDtos.get(0).getBalance());
        assertEquals("1122334455", accountDtos.get(1).getAccountNumber());
        assertEquals(2000L, accountDtos.get(1).getBalance());
        assertEquals("1112223331", accountDtos.get(2).getAccountNumber());
        assertEquals(3000L, accountDtos.get(2).getBalance());
    }

    @Test
    void getAccountsByUserIdFail_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }
}