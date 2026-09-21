package com.perebziak.musiceditor.exception;

public class DataConflictException extends AppException {
  public DataConflictException(String message) {
    super(message);
  }

  public DataConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}