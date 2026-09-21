package com.perebziak.musiceditor.controller;

import com.perebziak.musiceditor.core.AppContext;
import com.perebziak.musiceditor.core.SceneNavigator;
import com.perebziak.musiceditor.exception.AppException;
import com.perebziak.musiceditor.model.User;
import com.perebziak.musiceditor.util.AlertHelper;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminController {

  private final AppContext context;
  private final SceneNavigator navigator;

  @FXML private TableView<User> usersTable;
  @FXML private TableColumn<User, String> usernameColumn;
  @FXML private TableColumn<User, String> emailColumn;
  @FXML private TableColumn<User, String> roleColumn;
  @FXML private TableColumn<User, String> registrationColumn;
  @FXML private Label statusLabel;

  public AdminController(AppContext context, SceneNavigator navigator) {
    this.context = context;
    this.navigator = navigator;
  }

  @FXML
  private void initialize() {
    usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
    emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
    roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
    registrationColumn.setCellValueFactory(cellData -> {
      var date = cellData.getValue().getRegistrationDate();
      return new javafx.beans.property.SimpleStringProperty(
          date != null ? date.toLocalDate().toString() : "");
    });

    refreshTable();
  }

  private void refreshTable() {
    usersTable.setItems(FXCollections.observableArrayList(context.getAdminService().getAllUsers()));
  }

  @FXML
  private void onDeleteUserClick() {
    User selected = usersTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      AlertHelper.showError("Оберіть користувача для видалення");
      return;
    }

    boolean confirmed = AlertHelper.showConfirm(
        "Видалити користувача \"" + selected.getUsername() + "\"? Цю дію неможливо скасувати.");
    if (!confirmed) {
      return;
    }

    try {
      Long currentAdminId = context.getSessionManager().getCurrentUser().getId();
      context.getAdminService().deleteUser(selected.getId(), currentAdminId);
      refreshTable();
      statusLabel.setStyle("-fx-text-fill: #27ae60;");
      statusLabel.setText("Користувача \"" + selected.getUsername() + "\" видалено");
    } catch (AppException e) {
      statusLabel.setStyle("-fx-text-fill: #c0392b;");
      statusLabel.setText(e.getMessage());
    }
  }

  @FXML
  private void onGoLibraryClick() {
    navigator.switchScene("/main-view.fxml", "Музичний редактор — Бібліотека",
        new MainController(context, navigator));
  }

  @FXML
  private void onOpenPlaylistsClick() {
    navigator.switchScene("/playlist-view.fxml", "Музичний редактор — Плейлисти",
        new PlaylistController(context, navigator));
  }

  @FXML
  private void onOpenConverterClick() {
    navigator.switchScene("/converter-view.fxml", "Музичний редактор — Конвертер",
        new ConverterController(context, navigator));
  }

  @FXML
  private void onOpenSettingsClick() {
    navigator.switchScene("/settings-view.fxml", "Музичний редактор — Налаштування",
        new SettingsController(context, navigator));
  }
}