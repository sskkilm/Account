package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.CancelBalance;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    public static final long USE_AMOUNT = 1000L;
    public static final long CANCEL_AMOUNT = 1000L;
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void useBalanceSuccess() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("kim")
                .build();
        accountUser.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(
                        accountUser
                ));
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1231231231")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        account
                ));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.S)
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .transactedAt(LocalDateTime.now())
                        .transactionId("transactionId")
                        .build());
        ArgumentCaptor<Transaction> argumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        //when
        TransactionDto transactionDto = transactionService.useBalance(
                1L,
                "1000000000",
                USE_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(argumentCaptor.capture());
        assertEquals(USE_AMOUNT, argumentCaptor.getValue().getAmount());
        assertEquals(9000L, argumentCaptor.getValue().getBalanceSnapshot());

        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 유저 없음")
    void useBalanceFail_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1231231231", 10000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 계좌 없음")
    void useBalanceFail_AccountNotFound() {
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
                () -> transactionService.useBalance(1L, "1231231231", 10000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 계좌 소유주 다름")
    void useBalanceFail_UserAccountUnMatch() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("Pobi")
                .build();
        accountUser.setId(1L);
        AccountUser accountUser1 = AccountUser.builder()
                .name("Popo")
                .build();
        accountUser.setId(2L);
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
                () -> transactionService.useBalance(1L, "1231231231", 10000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 이미 해지된 계좌")
    void useBalanceFail_AccountAlreadyUnregistered() {
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
                () -> transactionService.useBalance(1L, "1231231231", 10000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 거래 금액이 잔액보다 큰 경우")
    void useBalanceFail_AmountExceedBalance() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("kim")
                .build();
        accountUser.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(
                        accountUser
                ));
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1231231231")
                .accountStatus(AccountStatus.IN_USE)
                .balance(100L)
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        account
                ));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L,
                        "1000000000",
                        USE_AMOUNT)
        );

        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 - 실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("kim")
                .build();
        accountUser.setId(1L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1231231231")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        account
                ));
//        given(transactionRepository.save(any()))
//                .willReturn(Transaction.builder()
//                        .account(account)
//                        .transactionType(TransactionType.USE)
//                        .transactionResultType(TransactionResultType.S)
//                        .amount(2000L)
//                        .balanceSnapshot(9000L)
//                        .transactedAt(LocalDateTime.now())
//                        .transactionId("transactionId")
//                        .build());
        ArgumentCaptor<Transaction> argumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        //when
        transactionService.saveFailedUseTransaction(
                "1000000000", USE_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(argumentCaptor.capture());
        assertEquals(USE_AMOUNT, argumentCaptor.getValue().getAmount());
        assertEquals(10000L, argumentCaptor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.F, argumentCaptor.getValue().getTransactionResultType());

    }

    @Test
    void cancelBalanceSuccess() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("kim")
                .build();
        accountUser.setId(1L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1231231231")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .transactedAt(LocalDateTime.now())
                .transactionId("transactionId")
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(
                        Transaction.builder()
                                .account(account)
                                .transactionType(TransactionType.CANCEL)
                                .transactionResultType(TransactionResultType.S)
                                .amount(CANCEL_AMOUNT)
                                .balanceSnapshot(10000L)
                                .transactedAt(LocalDateTime.now())
                                .transactionId("transactionIdForCancel")
                                .build()
                );
        ArgumentCaptor<Transaction> argumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        //when
        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId",
                "1000000000",
                CANCEL_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(argumentCaptor.capture());
        assertEquals(CANCEL_AMOUNT, argumentCaptor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT, argumentCaptor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
        assertEquals(TransactionType.CANCEL, transactionDto.getTransactionType());

        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 계좌 없음")
    void cancelBalanceFail_AccountNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1231231231", 10000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 거래 없음")
    void cancelBalanceFail_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1231231231", 10000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래와 계좌가 매칭 실패")
    void cancelBalanceFail_TransactionAccountUnMatch() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("kim")
                .build();
        accountUser.setId(1L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1231231231")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        account.setId(1L);
        Account accountNotUse = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1231231232")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        accountNotUse.setId(2L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .transactedAt(LocalDateTime.now())
                .transactionId("transactionId")
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1231231231", 10000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래 금액과 취소 금액이 다름")
    void cancelBalanceFail_CancelMustFully() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("kim")
                .build();
        accountUser.setId(1L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1231231231")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .amount(CANCEL_AMOUNT + 1000L)
                .balanceSnapshot(9000L)
                .transactedAt(LocalDateTime.now())
                .transactionId("transactionId")
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1231231231", CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 1년이 지난 거래는 취소 불가능")
    void cancelBalanceFail_TooOldToCancel() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("kim")
                .build();
        accountUser.setId(1L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1231231231")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .transactedAt(LocalDateTime.now().minusYears(1))
                .transactionId("transactionId")
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1231231231", CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 - 실패 트랜잭션 저장 성공")
    void saveFailedCancelTransaction() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("kim")
                .build();
        accountUser.setId(1L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1231231231")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
//        given(transactionRepository.save(any()))
//                .willReturn(Transaction.builder()
//                        .account(account)
//                        .transactionType(TransactionType.USE)
//                        .transactionResultType(TransactionResultType.S)
//                        .amount(2000L)
//                        .balanceSnapshot(9000L)
//                        .transactedAt(LocalDateTime.now())
//                        .transactionId("transactionId")
//                        .build());
        ArgumentCaptor<Transaction> argumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        //when
        transactionService.saveFailedUseTransaction("1000000000", CANCEL_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(argumentCaptor.capture());
        assertEquals(CANCEL_AMOUNT, argumentCaptor.getValue().getAmount());
        assertEquals(10000L, argumentCaptor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.F, argumentCaptor.getValue().getTransactionResultType());

    }

    @Test
    void successQueryTransaction() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .name("kim")
                .build();
        accountUser.setId(1L);
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1231231231")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        account.setId(1L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .transactedAt(LocalDateTime.now())
                .transactionId("transactionIdForQueryTransaction")
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        //when
        TransactionDto transactionDto = transactionService.queryTransaction("transactionId");

        //then
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(TransactionResultType.S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionIdForQueryTransaction", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("거래 조회 실패 - 해당 거래 없음")
    void queryTransactionFail_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }
}