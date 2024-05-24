package Transactions.bank.Transactions.controller;

import Transactions.bank.Transactions.service.UserService;
import Transactions.bank.Transactions.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestParam String login,
                                           @RequestParam String password,
                                           @RequestParam BigDecimal initialBalance,
                                           @RequestParam String phone,
                                           @RequestParam String email,
                                           @RequestParam String fullName,
                                           @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date birthDate) {
        User user = userService.createUser(login, password, initialBalance, phone, email, fullName, birthDate);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/contact")
    public ResponseEntity<User> updateUserContact(@PathVariable Long id,
                                                  @RequestParam(required = false) String newPhone,
                                                  @RequestParam(required = false) String newEmail) {
        User user = userService.updateUserContact(id, newPhone, newEmail);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}/contact")
    public ResponseEntity<User> deleteUserContact(@PathVariable Long id,
                                                  @RequestParam boolean deletePhone,
                                                  @RequestParam boolean deleteEmail) {
        User user = userService.deleteUserContact(id, deletePhone, deleteEmail);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transferMoney(@RequestParam Long fromUserId,
                                                @RequestParam Long toUserId,
                                                @RequestParam BigDecimal amount) {
        userService.transferMoney(fromUserId, toUserId, amount);
        return ResponseEntity.ok("Transfer successful");
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam(required = false) Date birthDate,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<User> users = userService.searchUsers(birthDate, phone, fullName, email, pageable);
        return ResponseEntity.ok(users.getContent());
    }
}
