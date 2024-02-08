package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.errors.ErrMessage;


@RestControllerAdvice
public class ApiErrorController {

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrMessage> nullPointerException(NullPointerException exception) {
        String errorMessage = "NOT FOUND: " + exception.getClass().getSimpleName() + ": " + exception.getMessage();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrMessage(errorMessage));
    }

}
