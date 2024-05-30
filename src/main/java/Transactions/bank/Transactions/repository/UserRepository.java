package Transactions.bank.Transactions.repository;

import Transactions.bank.Transactions.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    User findByLogin(String login);
}