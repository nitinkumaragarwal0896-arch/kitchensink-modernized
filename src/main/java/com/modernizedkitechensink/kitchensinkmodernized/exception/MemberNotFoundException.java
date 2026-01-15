package com.modernizedkitechensink.kitchensinkmodernized.exception;

public class MemberNotFoundException extends RuntimeException {

  public MemberNotFoundException(String id) {
    super("Member not found with ID: " + id);
  }
}
