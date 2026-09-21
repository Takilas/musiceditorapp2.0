package com.perebziak.musiceditor.service;

import com.perebziak.musiceditor.dto.RegisterRequest;
import com.perebziak.musiceditor.dto.VerifyCodeRequest;
import com.perebziak.musiceditor.exception.AuthException;
import com.perebziak.musiceditor.exception.ValidationException;
import com.perebziak.musiceditor.model.User;
import com.perebziak.musiceditor.model.ValidationResult;
import com.perebziak.musiceditor.model.VerificationCode;
import com.perebziak.musiceditor.repository.UserRepository;
import com.perebziak.musiceditor.repository.VerificationCodeRepository;
import com.perebziak.musiceditor.util.PasswordHasher;
import com.perebziak.musiceditor.util.ValidationUtil;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

  private static final int CODE_EXPIRY_MINUTES = 10;

  private final UserRepository userRepository;
  private final VerificationCodeRepository verificationCodeRepository;
  private final EmailService emailService;
  private final PasswordHasher passwordHasher;
  private final ValidationUtil validationUtil;

  // Тимчасове сховище "недозареєстрованих" користувачів: email -> дані реєстрації.
  // Заповнюється в initiateRegistration(), очищується в completeRegistration().
  private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

  public AuthService(UserRepository userRepository,
      VerificationCodeRepository verificationCodeRepository,
      EmailService emailService,
      PasswordHasher passwordHasher,
      ValidationUtil validationUtil) {
    this.userRepository = userRepository;
    this.verificationCodeRepository = verificationCodeRepository;
    this.emailService = emailService;
    this.passwordHasher = passwordHasher;
    this.validationUtil = validationUtil;
  }

  /**
   * Крок 1 реєстрації: валідація, перевірка унікальності, генерація і надсилання коду.
   */
  public void initiateRegistration(RegisterRequest request) {
    ValidationResult validation = validationUtil.validateRegistration(
        request.getUsername(), request.getEmail(), request.getPassword());
    if (!validation.isValid()) {
      throw new ValidationException(validation.getErrors());
    }

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new ValidationException("Користувач з такою поштою вже зареєстрований");
    }
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new ValidationException("Це ім'я користувача вже зайняте");
    }

    String code = generateCode();

    VerificationCode vc = new VerificationCode();
    vc.setEmail(request.getEmail());
    vc.setCode(code);
    vc.setExpiresDate(LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES));
    verificationCodeRepository.save(vc);

    pendingRegistrations.put(
        request.getEmail(),
        new PendingRegistration(request.getUsername(), passwordHasher.hash(request.getPassword()))
    );

    try {
      emailService.sendVerificationCode(request.getEmail(), code);
    } catch (com.perebziak.musiceditor.exception.EmailException e) {
      // Відкат: якщо лист не пішов, не лишаємо "мертвий" код і незавершену реєстрацію
      verificationCodeRepository.delete(vc.getId());
      pendingRegistrations.remove(request.getEmail());
      throw new AuthException("Не вдалося надіслати код підтвердження. " +
          "Перевірте налаштування пошти або спробуйте пізніше.");
    }
  }

  /**
   * Крок 2 реєстрації: перевірка коду і фінальне створення користувача в БД.
   */
  public User completeRegistration(VerifyCodeRequest request) {
    if (!validationUtil.isValidVerificationCode(request.getCode())) {
      throw new ValidationException("Код має складатися з 6 цифр");
    }

    VerificationCode vc = verificationCodeRepository.findActiveByEmail(request.getEmail())
        .orElseThrow(() -> new AuthException("Код прострочений або не знайдений. Спробуйте зареєструватися ще раз."));

    if (!vc.getCode().equals(request.getCode())) {
      throw new AuthException("Невірний код підтвердження");
    }

    PendingRegistration pending = pendingRegistrations.remove(request.getEmail());
    if (pending == null) {
      throw new AuthException("Дані реєстрації не знайдено, спробуйте зареєструватися заново");
    }

    verificationCodeRepository.markAsUsed(vc.getId());

    User user = new User();
    user.setUsername(pending.username());
    user.setEmail(request.getEmail());
    user.setPasswordHash(pending.passwordHash());
    user.setRole("USER");

    return userRepository.save(user);
  }

  /**
   * Логін за username АБО email.
   */
  public User login(String usernameOrEmail, String password) {
    User user = userRepository.findByUsername(usernameOrEmail)
        .or(() -> userRepository.findByEmail(usernameOrEmail))
        .orElseThrow(() -> new AuthException("Невірний логін або пароль"));

    boolean matches;
    try {
      matches = passwordHasher.matches(password, user.getPasswordHash());
    } catch (IllegalArgumentException e) {
      // Хеш пароля пошкоджений або не bcrypt-формату (наприклад, фейкові seed-дані)
      matches = false;
    }

    if (!matches) {
      throw new AuthException("Невірний логін або пароль");
    }

    return user;
  }

  private String generateCode() {
    SecureRandom random = new SecureRandom();
    int number = 100000 + random.nextInt(900000);
    return String.valueOf(number);
  }

  public void changePassword(Long userId, String oldPassword, String newPassword) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AuthException("Користувача не знайдено"));
    if (!passwordHasher.matches(oldPassword, user.getPasswordHash())) {
      throw new AuthException("Поточний пароль введено невірно");
    }
    if (!validationUtil.isValidPassword(newPassword)) {
      throw new ValidationException("Новий пароль має містити щонайменше 8 символів, хоча б одну літеру і одну цифру");
    }
    user.setPasswordHash(passwordHasher.hash(newPassword));
    userRepository.save(user);
  }

  private record PendingRegistration(String username, String passwordHash) {
  }
}