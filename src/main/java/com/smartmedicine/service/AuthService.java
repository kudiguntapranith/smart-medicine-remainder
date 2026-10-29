package com.smartmedicine.service;

import com.smartmedicine.dao.UserDao;
import com.smartmedicine.database.DatabaseManager;
import com.smartmedicine.model.User;
import com.smartmedicine.util.PasswordUtil;
import com.smartmedicine.util.ValidationException;
import com.smartmedicine.util.Validator;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

public class AuthService {
    private final UserDao userDao = new UserDao();

    public User register(String username, String fullName, String password, String confirm) {
        String u = Validator.username(username);
        String name = Validator.requireText(fullName, "Full name", 80);
        Validator.password(password);
        if (!password.equals(confirm)) throw new ValidationException("Passwords do not match.");
        String hash = PasswordUtil.hash(password);
        try {
            return DatabaseManager.inTransaction(c -> {
                if (userDao.findByUsername(c, u).isPresent())
                    throw new ValidationException("That username is already taken.");
                long id = userDao.insert(c, u, name, hash);
                return new User(id, u, name);
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof SQLIntegrityConstraintViolationException)
                throw new ValidationException("That username is already taken.");
            throw e;
        }
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty())
            throw new ValidationException("Please enter your username and password.");
        Optional<UserDao.StoredUser> stored = DatabaseManager.withConnection(c -> userDao.findByUsername(c, username.trim()));
        if (stored.isEmpty() || !PasswordUtil.verify(password, stored.get().passwordHash()))
            throw new ValidationException("Invalid username or password.");
        return stored.get().user();
    }
}
