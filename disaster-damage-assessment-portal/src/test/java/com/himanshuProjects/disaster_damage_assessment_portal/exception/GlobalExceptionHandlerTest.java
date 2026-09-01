package com.himanshuProjects.disaster_damage_assessment_portal.exception;

import com.himanshuProjects.disaster_damage_assessment_portal.dto.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    // ── Business Exceptions ──────────────────────────────────────────

    @Test
    @DisplayName("shouldReturn404ForResourceNotFound")
    void shouldReturn404ForResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "email", "x@y.com");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getMessage()).contains("User").contains("email").contains("x@y.com");
        assertThat(body.getPath()).isEqualTo("/api/test");
        assertThat(body.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("shouldReturn400ForBadRequest")
    void shouldReturn400ForBadRequest() {
        BadRequestException ex = new BadRequestException("Invalid input data");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getMessage()).isEqualTo("Invalid input data");
        assertThat(body.getPath()).isEqualTo("/api/test");
        assertThat(body.getErrorCode()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("shouldReturn409ForConflict")
    void shouldReturn409ForConflict() {
        ConflictException ex = new ConflictException("Email already exists");

        ResponseEntity<ErrorResponse> response = handler.handleConflict(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
        assertThat(body.getMessage()).isEqualTo("Email already exists");
        assertThat(body.getErrorCode()).isEqualTo("CONFLICT");
    }

    @Test
    @DisplayName("shouldReturn401ForUnauthorized")
    void shouldReturn401ForUnauthorized() {
        UnauthorizedException ex = new UnauthorizedException("Token expired");

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(401);
        assertThat(body.getMessage()).isEqualTo("Token expired");
        assertThat(body.getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("shouldReturn403ForForbidden")
    void shouldReturn403ForForbidden() {
        ForbiddenException ex = new ForbiddenException("Insufficient permissions");

        ResponseEntity<ErrorResponse> response = handler.handleForbidden(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(403);
        assertThat(body.getMessage()).isEqualTo("Insufficient permissions");
        assertThat(body.getErrorCode()).isEqualTo("FORBIDDEN");
    }

    // ── Validation Errors ────────────────────────────────────────────

    @Test
    @DisplayName("shouldReturn400WithFieldErrorsForValidationFailure")
    void shouldReturn400WithFieldErrorsForValidationFailure() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError nameError = new FieldError("registerRequest", "fullName", "Full name is required");
        FieldError emailError = new FieldError("registerRequest", "email", "Email is required");
        when(bindingResult.getAllErrors()).thenReturn(List.of(nameError, emailError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Validation Failed");
        assertThat(body.getPath()).isEqualTo("/api/test");
        assertThat(body.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getFieldErrors()).isNotNull();
        assertThat(body.getFieldErrors()).hasSize(2);
        assertThat(body.getFieldErrors()).containsEntry("fullName", "Full name is required");
        assertThat(body.getFieldErrors()).containsEntry("email", "Email is required");
    }

    @Test
    @DisplayName("shouldReturn400WithSingleFieldErrorForValidationFailure")
    void shouldReturn400WithSingleFieldErrorForValidationFailure() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError phoneError = new FieldError("user", "phoneNumber", "Please enter a valid phone number");
        when(bindingResult.getAllErrors()).thenReturn(List.of(phoneError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);

        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getFieldErrors()).hasSize(1);
        assertThat(body.getFieldErrors()).containsEntry("phoneNumber", "Please enter a valid phone number");
    }

    @Test
    @DisplayName("shouldReturn400ForConstraintViolation")
    void shouldReturn400ForConstraintViolation() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("email");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("must be a valid email");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Validation Failed");
        assertThat(body.getErrorCode()).isEqualTo("CONSTRAINT_VIOLATION");
        assertThat(body.getFieldErrors()).containsEntry("email", "must be a valid email");
    }

    @Test
    @DisplayName("shouldReturn400ForMissingRequestParameter")
    void shouldReturn400ForMissingRequestParameter() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("districtId", "Long");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParams(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getMessage()).contains("districtId");
        assertThat(body.getErrorCode()).isEqualTo("MISSING_PARAMETER");
    }

    @Test
    @DisplayName("shouldReturn400ForTypeMismatch")
    void shouldReturn400ForTypeMismatch() {
        MethodArgumentTypeMismatchException ex =
                mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("reportId");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getMessage()).contains("reportId");
        assertThat(body.getErrorCode()).isEqualTo("TYPE_MISMATCH");
    }

    // ── Authentication / Security Errors ─────────────────────────────

    @Test
    @DisplayName("shouldReturn401ForBadCredentials")
    void shouldReturn401ForBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Authentication failed");

        ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(401);
        assertThat(body.getMessage()).isEqualTo("Invalid email or password");
        assertThat(body.getErrorCode()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("shouldReturn403ForDisabledAccount")
    void shouldReturn403ForDisabledAccount() {
        DisabledException ex = new DisabledException("Account disabled");

        ResponseEntity<ErrorResponse> response = handler.handleDisabledAccount(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(403);
        assertThat(body.getMessage()).isEqualTo("Account is disabled. Please verify your email.");
        assertThat(body.getErrorCode()).isEqualTo("ACCOUNT_DISABLED");
    }

    @Test
    @DisplayName("shouldReturn403ForLockedAccount")
    void shouldReturn403ForLockedAccount() {
        LockedException ex = new LockedException("Account locked");

        ResponseEntity<ErrorResponse> response = handler.handleLockedAccount(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(403);
        assertThat(body.getMessage()).isEqualTo("Account is locked. Please contact admin.");
        assertThat(body.getErrorCode()).isEqualTo("ACCOUNT_LOCKED");
    }

    @Test
    @DisplayName("shouldReturn401ForGenericAuthenticationException")
    void shouldReturn401ForGenericAuthenticationException() {
        AuthenticationException ex = mock(AuthenticationException.class);

        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(401);
        assertThat(body.getMessage()).isEqualTo("Authentication failed");
        assertThat(body.getErrorCode()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    @DisplayName("shouldReturn403ForAccessDenied")
    void shouldReturn403ForAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(403);
        assertThat(body.getMessage()).isEqualTo("Access denied. You don't have permission.");
        assertThat(body.getErrorCode()).isEqualTo("ACCESS_DENIED");
    }

    // ── HTTP Errors ──────────────────────────────────────────────────

    @Test
    @DisplayName("shouldReturn405ForMethodNotAllowed")
    void shouldReturn405ForMethodNotAllowed() {
        when(request.getMethod()).thenReturn("DELETE");
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotAllowed(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(405);
        assertThat(body.getMessage()).contains("DELETE");
        assertThat(body.getErrorCode()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    @DisplayName("shouldReturn404ForNoResourceFound")
    void shouldReturn404ForNoResourceFound() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);

        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getMessage()).isEqualTo("The requested resource was not found");
        assertThat(body.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ── Catch-All ────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldReturn500ForGenericException")
    void shouldReturn500ForGenericException() {
        Exception ex = new RuntimeException("Something unexpected");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(body.getPath()).isEqualTo("/api/test");
        assertThat(body.getErrorCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("shouldAlwaysSetTimestampOnErrorResponse")
    void shouldAlwaysSetTimestampOnErrorResponse() {
        BadRequestException ex = new BadRequestException("test");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
