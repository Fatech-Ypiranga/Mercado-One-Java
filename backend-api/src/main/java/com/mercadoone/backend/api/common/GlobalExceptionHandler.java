package com.mercadoone.backend.api.common;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleInvalidArgument(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest().body(ApiEnvelope.failed(new ApiError(
                "VALIDATION_ERROR",
                "Dados invalidos.",
                details
        )));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return ResponseEntity.badRequest().body(ApiEnvelope.failed(new ApiError(
                "VALIDATION_ERROR",
                "Dados invalidos.",
                details
        )));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiEnvelope.failed(
                ApiError.of("INVALID_CREDENTIALS", "Login ou senha invalidos.")
        ));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiEnvelope.failed(
                ApiError.of("NOT_FOUND", exception.getMessage())
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleBusinessRule(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiEnvelope.failed(
                ApiError.of("BUSINESS_RULE", exception.getMessage())
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleIntegrity(DataIntegrityViolationException exception) {
        LOG.warn("Data integrity violation: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiEnvelope.failed(
                ApiError.of("DATA_CONFLICT", "Registro conflita com dados existentes (valor duplicado ou referencia invalida).")
        ));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiEnvelope<Void>> handleMalformedRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiEnvelope.failed(
                ApiError.of("MALFORMED_REQUEST", "Requisicao invalida: verifique formato, parametros e valores enviados.")
        ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleNoResource(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiEnvelope.failed(
                ApiError.of("NOT_FOUND", "Recurso nao encontrado.")
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiEnvelope<Void>> handleUnexpected(Exception exception) {
        LOG.error("Unexpected API error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiEnvelope.failed(
                ApiError.of("INTERNAL_ERROR", "Nao foi possivel concluir a operacao.")
        ));
    }
}
