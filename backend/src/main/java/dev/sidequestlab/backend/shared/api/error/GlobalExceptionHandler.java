package dev.sidequestlab.backend.shared.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setTitle("Validation error");

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", defaultMessage(fe)))
                .collect(Collectors.toList());

        pd.setProperty("errors", errors);
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    private String defaultMessage(FieldError fe) {
        return fe.getDefaultMessage() == null ? "" : fe.getDefaultMessage();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setTitle("Validation error");

        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(cv -> Map.of("path", cv.getPropertyPath().toString(), "message", cv.getMessage()))
                .collect(Collectors.toList());

        pd.setProperty("errors", errors);
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON");
        pd.setTitle("Malformed JSON");
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage() == null ? "Invalid argument" : ex.getMessage());
        pd.setTitle("Invalid argument");
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAll(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setTitle("Internal server error");
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {} {}", request.getMethod(), request.getRequestURI());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access is denied");
        pd.setTitle("Forbidden");
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed on {} {}", request.getMethod(), request.getRequestURI());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication failed");
        pd.setTitle("Unauthorized");
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }
}
