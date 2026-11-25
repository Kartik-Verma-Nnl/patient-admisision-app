package com.hospital.patientadmission.exception;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.validation.FieldError;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Turns every failure into the same clean JSON {@link ApiError}. Extending
 * ResponseEntityExceptionHandler also covers framework errors (405, 415, unknown URL, ...).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- our own exceptions -------------------------------------------------

    @ExceptionHandler(PatientNotFoundException.class)
    ResponseEntity<Object> handleNotFound(PatientNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Object> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<Object> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request.getRequestURI(), null);
    }

    // ---- validation & parsing errors ----------------------------------------

    /** @Valid failures: one entry per invalid field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField)
                        .thenComparing(fe -> String.valueOf(fe.getDefaultMessage())))
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", path(request), errors);
    }

    /** Bad JSON, a wrong type (age: "abc", age: 25.5) or an unparseable date. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException ife) {
            return fieldProblem(path(request), lastField(ife), describeInvalidFormat(ife));
        }
        if (cause instanceof MismatchedInputException mie) {
            return fieldProblem(path(request), lastField(mie), "has an invalid type");
        }
        if (cause instanceof JsonProcessingException) {
            return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body", path(request), null);
        }
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or unreadable", path(request), null);
    }

    /** e.g. GET /api/patients/abc */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getPropertyName() + "'";
        return build(HttpStatus.BAD_REQUEST, message, path(request), null);
    }

    /** Everything else the framework raises (405, 404 for unknown URLs, 415, ...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = status == HttpStatus.NOT_FOUND
                ? "Resource not found: " + path(request)
                : status.is5xxServerError() ? "An unexpected error occurred." : ex.getMessage();
        return build(status, message, path(request), null);
    }

    // ---- helpers --------------------------------------------------------------
    private static ResponseEntity<Object> build(HttpStatus status, String message, String path,
                                                Map<String, String> errors) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, errors);
        return ResponseEntity.status(status).body(body);
    }

    private static ResponseEntity<Object> fieldProblem(String path, String field, String message) {
        if (field == null) {
            return build(HttpStatus.BAD_REQUEST, "Request body must be a JSON object", path, null);
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", path, Map.of(field, message));
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null;
    }

    private static String lastField(JsonMappingException ex) {
        List<JsonMappingException.Reference> path = ex.getPath();
        return path.isEmpty() ? null : path.get(path.size() - 1).getFieldName();
    }

    private static String describeInvalidFormat(InvalidFormatException ex) {
        Class<?> target = ex.getTargetType();
        if (LocalDate.class.equals(target)) {
            return "must be a valid date in yyyy-MM-dd format";
        }
        if (target != null && (Number.class.isAssignableFrom(target) || target.isPrimitive())) {
            return "must be a whole number";
        }
        if (target != null && target.isEnum()) {
            return "must be one of: " + Arrays.stream(target.getEnumConstants())
                    .map(Object::toString).collect(Collectors.joining(", "));
        }
        return "has an invalid value";
    }
}
