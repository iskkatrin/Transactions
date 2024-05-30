package Transactions.bank.Transactions;

import Transactions.bank.Transactions.model.BankAccount;
import Transactions.bank.Transactions.model.User;
import Transactions.bank.Transactions.repository.UserRepository;
import Transactions.bank.Transactions.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testTransferMoneySuccess() {

        User fromUser = new User();
        fromUser.setId(1L);
        BankAccount fromAccount = new BankAccount();
        fromAccount.setBalance(new BigDecimal("1000"));
        fromUser.setAccount(fromAccount);

        User toUser = new User();
        toUser.setId(2L);
        BankAccount toAccount = new BankAccount();
        toAccount.setBalance(new BigDecimal("500"));
        toUser.setAccount(toAccount);

        when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(toUser));

        // When
        userService.transferMoney(1L, 2L, new BigDecimal("100"));

        // Then
        assertEquals(new BigDecimal("900"), fromUser.getAccount().getBalance());
        assertEquals(new BigDecimal("600"), toUser.getAccount().getBalance());
        verify(userRepository, times(1)).save(fromUser);
        verify(userRepository, times(1)).save(toUser);
    }

    @Test
    void testTransferMoneyInsufficientFunds() {
        // Given
        User fromUser = new User();
        fromUser.setId(1L);
        BankAccount fromAccount = new BankAccount();
        fromAccount.setBalance(new BigDecimal("50"));
        fromUser.setAccount(fromAccount);

        User toUser = new User();
        toUser.setId(2L);
        BankAccount toAccount = new BankAccount();
        toAccount.setBalance(new BigDecimal("500"));
        toUser.setAccount(toAccount);

        when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(toUser));

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.transferMoney(1L, 2L, new BigDecimal("100"));
        });

        assertEquals("Insufficient funds", exception.getMessage());
        assertEquals(new BigDecimal("50"), fromUser.getAccount().getBalance());
        assertEquals(new BigDecimal("500"), toUser.getAccount().getBalance());
        verify(userRepository, never()).save(fromUser);
        verify(userRepository, never()).save(toUser);
    }

    @Test
    void testTransferMoneySenderNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.transferMoney(1L, 2L, new BigDecimal("100"));
        });

        assertEquals("Sender not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testTransferMoneyRecipientNotFound() {
        // Given
        User fromUser = new User();
        fromUser.setId(1L);
        BankAccount fromAccount = new BankAccount();
        fromAccount.setBalance(new BigDecimal("1000"));
        fromUser.setAccount(fromAccount);

        when(userRepository.findById(1L)).thenReturn(Optional.of(fromUser));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.transferMoney(1L, 2L, new BigDecimal("100"));
        });

        assertEquals("Recipient not found", exception.getMessage());
        assertEquals(new BigDecimal("1000"), fromUser.getAccount().getBalance());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testTransferMoneyInvalidAmount() {
        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.transferMoney(1L, 2L, new BigDecimal("-100"));
        });

        assertEquals("Amount must be greater than zero", exception.getMessage());
        verify(userRepository, never()).findById(any(Long.class));
        verify(userRepository, never()).save(any(User.class));
    }
}