package io.github.snailrake.maintainer.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AuthenticationController.class, AccountAdministrationController.class})
class AccountErrorHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableRequest() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Проверьте данные запроса.");
    }

    static ResponseEntity<ProblemDetail> accountNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Пользователь не найден.");
    }

    static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.of(ProblemDetail.forStatusAndDetail(status, detail)).build();
    }
}
