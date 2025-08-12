package com.example.authapp;

public class ApiResponse<T> {
  private T data;
  private String message;
  private Object error;

  public ApiResponse(T data) {
    this.data = data;
  }

  public ApiResponse(T data, String message) {
    this.data = data;
    this.message = message;
  }

  public ApiResponse(T data, String message, Object error) {
    this.data = data;
    this.message = message;
    this.error = error;
  }
  // getters y setters (si los necesitas)
}