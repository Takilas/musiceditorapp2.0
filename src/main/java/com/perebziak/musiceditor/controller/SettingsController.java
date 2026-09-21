package com.perebziak.musiceditor.controller;

import com.perebziak.musiceditor.core.AppContext;
import com.perebziak.musiceditor.core.SceneNavigator;
import com.perebziak.musiceditor.exception.AppException;
import com.perebziak.musiceditor.util.SettingsManager;
import com.perebziak.musiceditor.util.ThemeManager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class SettingsController {

  private final AppContext context;
  private final SceneNavigator navigator;
  private boolean initializing = true;

  @FXML private ChoiceBox<String> themeChoiceBox;
  @FXML private PasswordField oldPasswordField;
  @FXML private PasswordField newPasswordField;
  @FXML private PasswordField confirmPasswordField;
  @FXML private Label passwordStatusLabel;
  @FXML private Button adminButton;

  public SettingsController(AppContext context, SceneNavigator navigator) {
    this.context = context;
    this.navigator = navigator;
  }

  @FXML
  private void initialize() {
    themeChoiceBox.setItems(FXCollections.observableArrayList("Світла", "Темна"));

    String currentTheme = SettingsManager.getInstance().getTheme();
    themeChoiceBox.getSelectionModel().select("dark".equals(currentTheme) ? "Темна" : "Світла");

    initializing = false;

    themeChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
      if (initializing || newVal == null) return;
      SettingsManager.getInstance().setTheme("Темна".equals(newVal) ? "dark" : "light");
      ThemeManager.apply(themeChoiceBox.getScene());
    });

    if (context.getSessionManager().isAdmin()) {
      adminButton.setVisible(true);
      adminButton.setManaged(true);
    }
  }

  @FXML
  private void onChangePasswordClick() {
    String oldPassword = oldPasswordField.getText();
    String newPassword = newPasswordField.getText();
    String confirmPassword = confirmPasswordField.getText();

    if (!newPassword.equals(confirmPassword)) {
      showStatus("Новий пароль і підтвердження не збігаються", false);
      return;
    }

    try {
      Long userId = context.getSessionManager().getCurrentUser().getId();
      context.getAuthService().changePassword(userId, oldPassword, newPassword);
      showStatus("Пароль успішно змінено", true);
      oldPasswordField.clear();
      newPasswordField.clear();
      confirmPasswordField.clear();
    } catch (AppException e) {
      showStatus(e.getMessage(), false);
    }
  }

  private void showStatus(String message, boolean success) {
    passwordStatusLabel.setStyle(success ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #c0392b;");
    passwordStatusLabel.setText(message);
  }

  @FXML
  private void onLogoutClick() {
    context.getPlayerService().stop();
    context.getSessionManager().logout();
    navigator.switchScene("/login-view.fxml", "Музичний редактор — Вхід",
        new LoginController(context, navigator));
  }

  @FXML
  private void onGoLibraryClick() {
    context.getPlayerService().stop();
    navigator.switchScene("/main-view.fxml", "Музичний редактор — Бібліотека",
        new MainController(context, navigator));
  }

  @FXML
  private void onOpenPlaylistsClick() {
    context.getPlayerService().stop();
    navigator.switchScene("/playlist-view.fxml", "Музичний редактор — Плейлисти",
        new PlaylistController(context, navigator));
  }

  @FXML
  private void onOpenConverterClick() {
    context.getPlayerService().stop();
    navigator.switchScene("/converter-view.fxml", "Музичний редактор — Конвертер",
        new ConverterController(context, navigator));
  }

  @FXML
  private void onOpenAdminClick() {
    context.getPlayerService().stop();
    navigator.switchScene("/admin-view.fxml", "Музичний редактор — Адміністрування",
        new AdminController(context, navigator));
  }
}