package dev.sidequestlab.backend.shared.api.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleMethodArgumentNotValidReturns400WithFieldErrorsAndFallbackMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new BeanWrapperImpl(new Object()), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        bindingResult.addError(new FieldError("request", "nickname", null));

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleMethod", String.class);
        MethodArgumentNotValidException exception =
            new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        ResponseEntity<ProblemDetail> response =
            handler.handleMethodArgumentNotValid(exception, request("POST", "/api/users"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Validation error");
        assertThat(body.getDetail()).isEqualTo("Request validation failed");
        assertCommonProperties(body, "/api/users");
        assertThat(body.getProperties().get("errors"))
            .isEqualTo(List.of(
                Map.of("field", "email", "message", "must not be blank"),
                Map.of("field", "nickname", "message", "")
            ));
    }

    @Test
    void handleConstraintViolationReturns400WithPathAndMessage() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("query.limit");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("must be greater than or equal to 1");

        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ProblemDetail> response =
            handler.handleConstraintViolation(exception, request("GET", "/api/search"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Validation error");
        assertThat(body.getDetail()).isEqualTo("Request validation failed");
        assertCommonProperties(body, "/api/search");
        assertThat(body.getProperties().get("errors"))
            .isEqualTo(List.of(Map.of("path", "query.limit", "message", "must be greater than or equal to 1")));
    }

    @Test
    void handleHttpMessageNotReadableReturns400() {
        HttpMessageNotReadableException exception =
            new HttpMessageNotReadableException("malformed", emptyInputMessage());

        ResponseEntity<ProblemDetail> response =
            handler.handleHttpMessageNotReadable(exception, request("POST", "/api/payload"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Malformed JSON");
        assertThat(body.getDetail()).isEqualTo("Malformed JSON");
        assertCommonProperties(body, "/api/payload");
    }

    private HttpInputMessage emptyInputMessage() {
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public HttpHeaders getHeaders() {
                return HttpHeaders.EMPTY;
            }
        };
    }

    @Test
    void handleIllegalArgumentReturnsMessageWhenPresent() {
        IllegalArgumentException exception = new IllegalArgumentException("invalid page size");

        ResponseEntity<ProblemDetail> response =
            handler.handleIllegalArgument(exception, request("GET", "/api/items"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Invalid argument");
        assertThat(body.getDetail()).isEqualTo("invalid page size");
        assertCommonProperties(body, "/api/items");
    }

    @Test
    void handleIllegalArgumentReturnsDefaultDetailWhenMessageIsNull() {
        IllegalArgumentException exception = new IllegalArgumentException((String) null);

        ResponseEntity<ProblemDetail> response =
            handler.handleIllegalArgument(exception, request("GET", "/api/items"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Invalid argument");
        assertThat(body.getDetail()).isEqualTo("Invalid argument");
        assertCommonProperties(body, "/api/items");
    }

    @Test
    void handleNoResourceFoundReturns404() {
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/api/missing");

        ResponseEntity<ProblemDetail> response =
            handler.handleNoResourceFound(exception, request("GET", "/api/missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Not Found");
        assertThat(body.getDetail()).isEqualTo("Resource not found");
        assertCommonProperties(body, "/api/missing");
    }

    @Test
    void handleNoHandlerFoundReturns404() {
        NoHandlerFoundException exception = new NoHandlerFoundException("GET", "/api/unknown", new HttpHeaders());

        ResponseEntity<ProblemDetail> response =
            handler.handleNoHandlerFound(exception, request("GET", "/api/unknown"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Not Found");
        assertThat(body.getDetail()).isEqualTo("Resource not found");
        assertCommonProperties(body, "/api/unknown");
    }

    @Test
    void handleMethodNotSupportedReturns405() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<ProblemDetail> response =
            handler.handleMethodNotSupported(exception, request("PATCH", "/api/items"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Method Not Allowed");
        assertThat(body.getDetail()).isEqualTo("Method not allowed");
        assertCommonProperties(body, "/api/items");
    }

    @Test
    void handleAccessDeniedReturns403() {
        AccessDeniedException exception = new AccessDeniedException("forbidden");

        ResponseEntity<ProblemDetail> response =
            handler.handleAccessDenied(exception, request("GET", "/api/admin"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Forbidden");
        assertThat(body.getDetail()).isEqualTo("Access is denied");
        assertCommonProperties(body, "/api/admin");
    }

    @Test
    void handleAuthenticationReturns401() {
        BadCredentialsException exception = new BadCredentialsException("bad credentials");

        ResponseEntity<ProblemDetail> response =
            handler.handleAuthentication(exception, request("POST", "/api/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Unauthorized");
        assertThat(body.getDetail()).isEqualTo("Authentication failed");
        assertCommonProperties(body, "/api/auth/login");
    }

    @Test
    void handleAllReturns500() {
        RuntimeException exception = new RuntimeException("boom");

        ResponseEntity<ProblemDetail> response =
            handler.handleAll(exception, request("GET", "/api/crash"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        ProblemDetail body = response.getBody();
        assertThat(body.getTitle()).isEqualTo("Internal server error");
        assertThat(body.getDetail()).isEqualTo("An unexpected error occurred");
        assertCommonProperties(body, "/api/crash");
    }

    private static MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    private static void assertCommonProperties(ProblemDetail body, String path) {
        assertThat(body.getProperties()).containsEntry("path", path);
        assertThat(body.getProperties()).containsKey("timestamp");
        assertThat(body.getProperties().get("timestamp")).isInstanceOf(String.class);
        Instant.parse((String) body.getProperties().get("timestamp"));
    }

    @SuppressWarnings("unused")
    private void sampleMethod(String value) {
        // Method signature used to build a MethodParameter in tests.
    }
}
