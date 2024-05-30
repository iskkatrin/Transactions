```sql
-- liquibase formatted sql
-- changeset author:you

CREATE TABLE User (
    id BIGINT PRIMARY KEY,
    login VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(10),
    fullName VARCHAR(255) NOT NULL,
    birthDate DATE NOT NULL,
    account_id BIGINT,
    lastBalanceIncrease TIMESTAMP
);

CREATE TABLE BankAccount (
    id BIGINT PRIMARY KEY,
    balance DECIMAL(19, 2) NOT NULL,
    initialBalance DECIMAL(19, 2) NOT NULL
);
