package com.academia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> handleBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(Instant.now(), 401, "INVALID_CREDENTIALS", "Credenciales inválidas."));
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Instant.now(), 404, "USER_NOT_FOUND", "Usuario no encontrado."));
    }

    @ExceptionHandler(StudentNotFoundException.class)
    ResponseEntity<ApiError> handleStudentNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Instant.now(), 404, "STUDENT_NOT_FOUND", "Alumno no encontrado."));
    }

    @ExceptionHandler(SubjectNotFoundException.class)
    ResponseEntity<ApiError> handleSubjectNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Instant.now(), 404, "SUBJECT_NOT_FOUND", "Materia no encontrada."));
    }

    @ExceptionHandler(AcademyClassNotFoundException.class)
    ResponseEntity<ApiError> handleClassNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Instant.now(), 404, "CLASS_NOT_FOUND", "Clase no encontrada."));
    }

    @ExceptionHandler(ClassAccessDeniedException.class)
    ResponseEntity<ApiError> handleClassAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(Instant.now(), 403, "CLASS_ACCESS_DENIED", "No tienes permiso para esta clase."));
    }

    @ExceptionHandler(InvalidTeacherException.class)
    ResponseEntity<ApiError> handleInvalidTeacher() {
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "INVALID_TEACHER", "La clase requiere un profesor activo."));
    }

    @ExceptionHandler(InvalidClassStateException.class)
    ResponseEntity<ApiError> handleInvalidClassState() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Instant.now(), 409, "INVALID_CLASS_STATE", "Solo se puede modificar o cancelar una clase programada."));
    }

    @ExceptionHandler(InvalidClassScheduleException.class)
    ResponseEntity<ApiError> handleInvalidClassSchedule() {
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "INVALID_CLASS_SCHEDULE", "El rango de fechas u horario es inválido."));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    ResponseEntity<ApiError> handleDuplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Instant.now(), 409, "USERNAME_ALREADY_EXISTS", "El username ya existe."));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    ResponseEntity<ApiError> handlePassword() {
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "INVALID_PASSWORD", "La contraseña debe tener al menos 8 caracteres."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "VALIDATION_ERROR", String.join("; ", details)));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> handleInvalidValue() {
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "VALIDATION_ERROR", "Hay un valor inválido en la solicitud."));
    }

    record ApiError(Instant timestamp, int status, String code, String message) {
    }
}
