package Transactions.bank.Transactions.service;

import Transactions.bank.Transactions.model.BankAccount;
import Transactions.bank.Transactions.repository.UserRepository;
import jakarta.transaction.Transactional;
import Transactions.bank.Transactions.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User createUser(String login, String password, BigDecimal initialBalance, String phone, String email, String fullName, Date birthDate) {
        if (userRepository.existsByLogin(login) || userRepository.existsByEmail(email) || userRepository.existsByPhone(phone)) {
            throw new RuntimeException("Login, email or phone already taken");
        }

        BankAccount account = new BankAccount();
        account.setBalance(initialBalance);

        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        user.setPhone(phone);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setBirthDate(birthDate);
        user.setAccount(account);

        return userRepository.save(user);
    }

    public User updateUserContact(Long userId, String newPhone, String newEmail) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = optionalUser.get();
        if (newPhone != null && !newPhone.isEmpty() && !userRepository.existsByPhone(newPhone)) {
            user.setPhone(newPhone);
        } else {
            throw new RuntimeException("Phone already taken or invalid");
        }

        if (newEmail != null && !newEmail.isEmpty() && !userRepository.existsByEmail(newEmail)) {
            user.setEmail(newEmail);
        } else {
            throw new RuntimeException("Email already taken or invalid");
        }

        return userRepository.save(user);
    }

    public User deleteUserContact(Long userId, boolean deletePhone, boolean deleteEmail) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = optionalUser.get();

        if (deletePhone && user.getEmail() == null) {
            throw new RuntimeException("Cannot delete last contact");
        }

        if (deleteEmail && user.getPhone() == null) {
            throw new RuntimeException("Cannot delete last contact");
        }

        if (deletePhone) {
            user.setPhone(null);
        }

        if (deleteEmail) {
            user.setEmail(null);
        }

        return userRepository.save(user);
    }

    public void transferMoney(Long fromUserId, Long toUserId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        User fromUser = userRepository.findById(fromUserId).orElseThrow(() -> new RuntimeException("Sender not found"));
        User toUser = userRepository.findById(toUserId).orElseThrow(() -> new RuntimeException("Recipient not found"));

        BankAccount fromAccount = fromUser.getAccount();
        BankAccount toAccount = toUser.getAccount();

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        userRepository.save(fromUser);
        userRepository.save(toUser);
    }

    @Scheduled(fixedRate = 60000) // каждая минута
    public void increaseBalances() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            BigDecimal currentBalance = user.getAccount().getBalance();
            BigDecimal increasedBalance = currentBalance.multiply(new BigDecimal("1.05"));
            if (increasedBalance.compareTo(initialBalance.multiply(new BigDecimal("2.07"))) > 0) {
                increasedBalance = initialBalance.multiply(new BigDecimal("2.07"));
            }
            user.getAccount().setBalance(increasedBalance);
            user.setLastBalanceIncrease(new Date());
            userRepository.save(user);
        }
    }

    @Transactional
    public Page<User> searchUsers(Date birthDate, String phone, String fullName, String email, Pageable pageable) {
        return userRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (birthDate != null) {
                predicates.add(criteriaBuilder.greaterThan(root.get("birthDate"), birthDate));
            }
            if (phone != null && !phone.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("phone"), phone));
            }
            if (fullName != null && !fullName.isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("fullName"), fullName + "%"));
            }
            if (email != null && !email.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("email"), email));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }
}