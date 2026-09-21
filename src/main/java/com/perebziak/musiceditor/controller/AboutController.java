package com.perebziak.musiceditor.controller;

import com.perebziak.musiceditor.core.AppContext;
import com.perebziak.musiceditor.core.SceneNavigator;

import javafx.fxml.FXML;

public class AboutController {

  private final AppContext context;
  private final SceneNavigator navigator;
  private final String returnFxml;
  private final String returnTitle;

  public AboutController(AppContext context, SceneNavigator navigator, String returnFxml, String returnTitle) {
    this.context = context;
    this.navigator = navigator;
    this.returnFxml = returnFxml;
    this.returnTitle = returnTitle;
  }

  @FXML
  private void onBackClick() {
    if ("/login-view.fxml".equals(returnFxml)) {
      navigator.switchScene(returnFxml, returnTitle, new LoginController(context, navigator));
    } else {
      navigator.switchScene(returnFxml, returnTitle, new RegisterController(context, navigator));
    }
  }
}