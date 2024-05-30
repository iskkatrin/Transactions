package Transactions.bank.Transactions.service;

import Transactions.bank.Transactions.exception.UserNotFoundException;
import Transactions.bank.Transactions.model.BankAccount;
import Transactions.bank.Transactions.repository.UserRepository;
import jakarta.transaction.Transactional;
import Transactions.bank.Transactions.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.persistence.criteria.Predicate;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    private final Lock lock = new ReentrantLock();

    @Transactional
    public User createUser(String login, String password, BigDecimal initialBalance, String phone, String email, String fullName, Date birthDate) {
        log.info("Попытка создания пользователя с логином: {}, email: {}, телефон: {}", login, email, phone);

        if (userRepository.existsByLogin(login) || (email != null && userRepository.existsByEmail(email)) || (phone != null && userRepository.existsByPhone(phone))) {
            log.warn("Не удалось создать пользователя: логин, email или телефон уже заняты");
            throw new DataIntegrityViolationException("Логин, email или телефон уже заняты");
        }

        if ((phone == null || phone.isEmpty()) && (email == null || email.isEmpty())) {
            log.warn("Не удалось создать пользователя: нужно указать либо телефон, либо email");
            throw new RuntimeException("Нужно указать либо телефон, либо email");
        }

        BankAccount account = new BankAccount();
        account.setBalance(initialBalance);
        account.setInitialBalance(initialBalance);

        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        user.setPhone(phone);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setBirthDate(birthDate);
        user.setAccount(account);

        User savedUser = userRepository.save(user);
        log.info("Пользователь успешно создан с ID: {}", savedUser.getId());
        return savedUser;
    }

    public User updateUserContact(Long userId, String newPhone, String newEmail) {
        log.info("Попытка обновления контактной информации пользователя с ID: {}", userId);
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            log.warn("Не удалось обновить информацию: пользователь с ID: {} не найден", userId);
            throw new UserNotFoundException("Пользователь не найден");
        }

        User user = optionalUser.get();
        if (newPhone != null && !newPhone.isEmpty() && !userRepository.existsByPhone(newPhone)) {
            user.setPhone(newPhone);
            log.info("Телефон обновлен для пользователя с ID: {}", userId);
        } else if (newPhone != null && !newPhone.isEmpty()) {
            log.warn("Не удалось обновить телефон: телефон уже занят или неверен");
            throw new RuntimeException("Телефон уже занят или неверен");
        }

        if (newEmail != null && !newEmail.isEmpty() && !userRepository.existsByEmail(newEmail)) {
            user.setEmail(newEmail);
            log.info("Email обновлен для пользователя с ID: {}", userId);
        } else if (newEmail != null && !newEmail.isEmpty()) {
            log.warn("Не удалось обновить email: email уже занят или неверен");
            throw new RuntimeException("Email уже занят или неверен");
        }

        User updatedUser = userRepository.save(user);
        log.info("Контактная информация пользователя успешно обновлена для пользователя с ID: {}", updatedUser.getId());
        return updatedUser;
    }

    public User deleteUserContact(Long userId, boolean deletePhone, boolean deleteEmail) {
        log.info("Попытка удаления контактной информации пользователя с ID: {}", userId);
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            log.warn("Не удалось удалить контактную информацию: пользователь с ID: {} не найден", userId);
            throw new UserNotFoundException("Пользователь не найден");
        }

        User user = optionalUser.get();

        if (deletePhone && (user.getEmail() == null || user.getEmail().isEmpty())) {
            log.warn("Не удалось удалить контактную информацию: нельзя удалить последний контакт (телефон)");
            throw new RuntimeException("Нельзя удалить последний контакт");
        }

        if (deleteEmail && (user.getPhone() == null || user.getPhone().isEmpty())) {
            log.warn("Не удалось удалить контактную информацию: нельзя удалить последний контакт (email)");
            throw new RuntimeException("Нельзя удалить последний контакт");
        }

        if (deletePhone) {
            user.setPhone(null);
            log.info("Телефон удален для пользователя с ID: {}", userId);
        }

        if (deleteEmail) {
            user.setEmail(null);
            log.info("Email удален для пользователя с ID: {}", userId);
        }

        User updatedUser = userRepository.save(user);
        log.info("Контактная информация пользователя успешно удалена для пользователя с ID: {}", updatedUser.getId());
        return updatedUser;
    }

    @Transactional
    public void transferMoney(Long fromUserId, Long toUserId, BigDecimal amount) {
        log.info("Попытка перевода денег от пользователя с ID: {} к пользователю с ID: {}, сумма: {}", fromUserId, toUserId, amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Не удалось выполнить перевод: сумма должна быть больше нуля");
            throw new RuntimeException("Сумма должна быть больше нуля");
        }

        User fromUser = userRepository.findById(fromUserId).orElseThrow(() -> {
            log.warn("Не удалось выполнить перевод: отправитель с ID: {} не найден", fromUserId);
            return new UserNotFoundException("Отправитель не найден");
        });
        User toUser = userRepository.findById(toUserId).orElseThrow(() -> {
            log.warn("Не удалось выполнить перевод: получатель с ID: {} не найден", toUserId);
            return new UserNotFoundException("Получатель не найден");
        });

        BankAccount fromAccount = fromUser.getAccount();
        BankAccount toAccount = toUser.getAccount();

        lock.lock();
        try {
            if (fromAccount.getBalance().compareTo(amount) < 0) {
                log.warn("Не удалось выполнить перевод: недостаточно средств на счете отправителя");
                throw new RuntimeException("Недостаточно средств");
            }

            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));

            userRepository.save(fromUser);
            userRepository.save(toUser);
            log.info("Перевод денег успешно выполнен от пользователя с ID: {} к пользователю с ID: {}, сумма: {}", fromUserId, toUserId, amount);
        } finally {
            lock.unlock();
        }
    }

    @Scheduled(fixedRate = 60000) // каждая минута
    @Transactional
    public void increaseBalances() {
        log.info("Начало планового увеличения балансов для всех пользователей");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            synchronized (user) {
                BigDecimal currentBalance = user.getAccount().getBalance();
                BigDecimal initialBalance = user.getAccount().getInitialBalance();

                // Увеличение баланса на 5%
                BigDecimal increasedBalance = currentBalance.multiply(new BigDecimal("1.05"));

                // Ограничение до 207% от начального баланса
                if (increasedBalance.compareTo(initialBalance.multiply(new BigDecimal("2.07"))) > 0) {
                    increasedBalance = initialBalance.multiply(new BigDecimal("2.07"));
                }

                user.getAccount().setBalance(increasedBalance);
                user.setLastBalanceIncrease(new Date());

                userRepository.save(user);
                log.info("Баланс увеличен для пользователя с ID: {}", user.getId());
            }
        }
    }

    @Transactional
    public Page<User> searchUsers(Date birthDate, String phone, String fullName, String email, Pageable pageable) {
        log.info("Поиск пользователей с параметрами - дата рождения: {}, телефон: {}, полное имя: {}, email: {}", birthDate, phone, fullName, email);

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