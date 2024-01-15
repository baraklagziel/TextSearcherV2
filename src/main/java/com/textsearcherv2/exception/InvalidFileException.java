package com.textsearcherv2.exception;

public class InvalidFileException extends IllegalArgumentException {
    public InvalidFileException(String message) {
        super(message);
    }
}