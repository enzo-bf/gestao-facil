package com.gestaofacil.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Valor inválido." : error.getDefaultMessage(),
                        (first, ignored) -> first
                ));
        return response(HttpStatus.BAD_REQUEST, "Dados inválidos.", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = exception.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, ignored) -> first
                ));
        return response(HttpStatus.BAD_REQUEST, "Dados inválidos.", request, fieldErrors);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            ValidationException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "Dados inválidos.", request, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.", request, Map.of());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(SetupAlreadyCompletedException.class)
    public ResponseEntity<ApiErrorResponse> handleSetupAlreadyCompleted(
            SetupAlreadyCompletedException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidSetupTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSetupToken(
            InvalidSetupTokenException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(SetupUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleSetupUnavailable(
            SetupUnavailableException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.CONFLICT, "Os dados informados já estão em uso ou são inválidos.", request, Map.of());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
            EntityNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Erro não tratado durante a requisição path={} type={}",
                request.getRequestURI(), exception.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno.", request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                java.time.Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
