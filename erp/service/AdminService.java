package edu.univ.erp.service;

import edu.univ.erp.data.UserDao;
import edu.univ.erp.domain.User;
import edu.univ.erp.util.AccessChecker;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

public class AdminService {
    private final UserDao userDao = new UserDao();

    public List<User> listUsers() { return userDao.findAll(); }

    public Optional<User> getUser(int id) { return userDao.findById(id); }

    // create user with plaintext password -> stored as bcrypt
    public int createUser(String username, String plainPassword, String role) {
        AccessChecker.checkWritableOrThrow();
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        User u = new User(null, username, hash, role);
        return userDao.insert(u);
    }

    // update: if plainPassword != null and not empty, re-hash and update password; else keep hash
    public void updateUser(Integer userId, String username, String plainPassword, String role) {
        AccessChecker.checkWritableOrThrow();
        Optional<User> existing = userDao.findById(userId);
        if (!existing.isPresent()) throw new IllegalArgumentException("No such user id: " + userId);
        User u = existing.get();
        u.setUsername(username);
        if (plainPassword != null && !plainPassword.isEmpty()) {
            String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
            u.setPasswordHash(hash);
        }
        u.setRole(role);
        userDao.update(u);
    }

    public void deleteUser(int userId) {
        AccessChecker.checkWritableOrThrow();
        userDao.delete(userId);
    }
}
