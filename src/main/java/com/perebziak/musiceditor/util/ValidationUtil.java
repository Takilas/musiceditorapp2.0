package com.perebziak.musiceditor.util;

import com.perebziak.musiceditor.model.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ValidationUtil {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  public ValidationResult validateRegistration(String username, String email, String password) {
    List<String> errors = new ArrayList<>();

    if (username == null || username.isBlank()) {
      errors.add("Ім'я користувача не може бути порожнім");
    } else if (username.length() < 3 || username.length() > 50) {
      errors.add("Ім'я користувача має містити від 3 до 50 символів");
    }

    if (!isValidEmail(email)) {
      errors.add("Некоректний формат email");
    }

    if (!isValidPassword(password)) {
      errors.add("Пароль має містити щонайменше 8 символів, хоча б одну літеру і одну цифру");
    }

    return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
  }

  public boolean isValidEmail(String email) {
    return email != null && EMAIL_PATTERN.matcher(email).matches();
  }

  public boolean isValidPassword(String password) {
    return password != null && password.length() >= 8
        && password.matches(".*[A-Za-z].*") && password.matches(".*\\d.*");
  }

  public boolean isValidVerificationCode(String code) {
    return code != null && code.matches("\\d{6}");
  }
}