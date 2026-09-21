package com.perebziak.musiceditor.controller;

import com.perebziak.musiceditor.core.AppContext;
import com.perebziak.musiceditor.core.SceneNavigator;
import com.perebziak.musiceditor.exception.AppException;
import com.perebziak.musiceditor.model.User;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

  private final AppContext context;
  private final SceneNavigator navigator;

  @FXML private TextField usernameField;
  @FXML private PasswordField passwordField;
  @FXML private Label errorLabel;

  public LoginController(AppContext context, SceneNavigator navigator) {
    this.context = context;
    this.navigator = navigator;
  }

  @FXML
  private void onLoginClick() {
    try {
      User user = context.getAuthService().login(usernameField.getText(), passwordField.getText());
      context.getSessionManager().login(user);
      navigator.switchScene("/main-view.fxml", "Музичний редактор — Бібліотека",
          new MainController(context, navigator));
    } catch (AppException e) {
      errorLabel.setStyle("-fx-text-fill: #c0392b;");
      errorLabel.setText(e.getMessage());
    }
  }

  @FXML
  private void onGoToRegisterClick() {
    navigator.switchScene("/register-view.fxml", "Музичний редактор — Реєстрація",
        new RegisterController(context, navigator));
  }

  @FXML
  private void onAboutClick() {
    navigator.switchScene("/about-view.fxml", "Музичний редактор — Про програму",
        new AboutController(context, navigator, "/login-view.fxml", "Музичний редактор — Вхід"));
  }

  public void showSuccessMessage(String message) {
    errorLabel.setStyle("-fx-text-fill: #27ae60;");
    errorLabel.setText(message);
  }
}