package com.perebziak.musiceditor.service;

import com.perebziak.musiceditor.exception.DataConflictException;
import com.perebziak.musiceditor.exception.ValidationException;
import com.perebziak.musiceditor.model.User;
import com.perebziak.musiceditor.repository.UserRepository;

import java.sql.SQLException;
import java.util.List;

public class AdminService {

  private final UserRepository userRepository;

  public AdminService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  public void deleteUser(Long userId, Long currentAdminId) {
    if (userId.equals(currentAdminId)) {
      throw new ValidationException("Неможливо видалити власний обліковий запис");
    }
    try {
      userRepository.delete(userId);
    } catch (RuntimeException e) {
      // SQLite кидає SQLException при порушенні FK (наприклад, у користувача є плейлисти,
      // відредаговані треки або замовлення на конвертацію)
      if (e.getCause() instanceof SQLException) {
        throw new DataConflictException(
            "Неможливо видалити користувача: з ним пов'язані дані (плейлисти, відредаговані треки або замовлення). "
                + "Спочатку видаліть або передайте ці дані.", e);
      }
      throw e;
    }
  }
}