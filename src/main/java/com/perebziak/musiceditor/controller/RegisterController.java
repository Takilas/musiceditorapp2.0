package com.perebziak.musiceditor.controller;

import com.perebziak.musiceditor.core.AppContext;
import com.perebziak.musiceditor.core.SceneNavigator;
import com.perebziak.musiceditor.dto.RegisterRequest;
import com.perebziak.musiceditor.dto.VerifyCodeRequest;
import com.perebziak.musiceditor.exception.AppException;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegisterController {

  private final AppContext context;
  private final SceneNavigator navigator;
  private String pendingEmail;

  @FXML private VBox registrationPane;
  @FXML private VBox verificationPane;

  @FXML private TextField usernameField;
  @FXML private TextField emailField;
  @FXML private PasswordField passwordField;
  @FXML private Label registerErrorLabel;

  @FXML private TextField codeField;
  @FXML private Label verificationInfoLabel;
  @FXML private Label verifyErrorLabel;

  public RegisterController(AppContext context, SceneNavigator navigator) {
    this.context = context;
    this.navigator = navigator;
  }

  @FXML
  private void onRegisterClick() {
    RegisterRequest request = new RegisterRequest(
        usernameField.getText(), emailField.getText(), passwordField.getText());
    try {
      context.getAuthService().initiateRegistration(request);
      pendingEmail = request.getEmail();
      verificationInfoLabel.setText("Код підтвердження надіслано на " + pendingEmail);
      registrationPane.setVisible(false);
      registrationPane.setManaged(false);
      verificationPane.setVisible(true);
      verificationPane.setManaged(true);
    } catch (AppException e) {
      registerErrorLabel.setStyle("-fx-text-fill: #c0392b;");
      registerErrorLabel.setText(e.getMessage());
    }
  }

  @FXML
  private void onVerifyClick() {
    try {
      context.getAuthService().completeRegistration(new VerifyCodeRequest(pendingEmail, codeField.getText()));
      LoginController controller = navigator.switchScene("/login-view.fxml",
          "Музичний редактор — Вхід", new LoginController(context, navigator));
      controller.showSuccessMessage("Реєстрацію завершено! Тепер увійдіть під своїм акаунтом.");
    } catch (AppException e) {
      verifyErrorLabel.setStyle("-fx-text-fill: #c0392b;");
      verifyErrorLabel.setText(e.getMessage());
    }
  }

  @FXML
  private void onBackToLoginClick() {
    navigator.switchScene("/login-view.fxml", "Музичний редактор — Вхід",
        new LoginController(context, navigator));
  }

  @FXML
  private void onAboutClick() {
    navigator.switchScene("/about-view.fxml", "Музичний редактор — Про програму",
        new AboutController(context, navigator, "/register-view.fxml", "Музичний редактор — Реєстрація"));
  }
}