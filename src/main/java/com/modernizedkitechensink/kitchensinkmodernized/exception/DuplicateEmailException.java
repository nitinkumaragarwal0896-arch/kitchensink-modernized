package com.modernizedkitechensink.kitchensinkmodernized.exception;

public class DuplicateEmailException extends RuntimeException {

  public DuplicateEmailException(String email) {
    super("Email already exists: " + email);
  }
}
