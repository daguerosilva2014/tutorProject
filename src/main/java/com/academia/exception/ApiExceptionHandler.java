package com.academia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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

    @ExceptionHandler(ClassParticipationNotFoundException.class)
    ResponseEntity<ApiError> handleParticipationNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Instant.now(), 404, "PARTICIPATION_NOT_FOUND", "Participación no encontrada."));
    }

    @ExceptionHandler(ClassPackageNotFoundException.class)
    ResponseEntity<ApiError> handlePackageNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Instant.now(), 404, "PACKAGE_NOT_FOUND", "Paquete no encontrado."));
    }

    @ExceptionHandler(InvalidPaymentException.class)
    ResponseEntity<ApiError> handleInvalidPayment() {
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "INVALID_PAYMENT", "El pago debe ser mayor que cero."));
    }

    @ExceptionHandler(PaymentExceedsBalanceException.class)
    ResponseEntity<ApiError> handlePaymentExceedsBalance() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Instant.now(), 409, "PAYMENT_EXCEEDS_BALANCE", "El pago supera el saldo pendiente."));
    }

    @ExceptionHandler(NoPackageBalanceException.class)
    ResponseEntity<ApiError> handleNoPackageBalance() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Instant.now(), 409, "NO_PACKAGE_BALANCE", "El alumno no tiene clases disponibles en un paquete."));
    }

    @ExceptionHandler(InvalidBillingModeException.class)
    ResponseEntity<ApiError> handleInvalidBillingMode() {
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "INVALID_BILLING_MODE", "La modalidad de cobro no coincide con los datos de la participación."));
    }

    @ExceptionHandler(AttendanceConflictException.class)
    ResponseEntity<ApiError> handleAttendanceConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Instant.now(), 409, "ATTENDANCE_CONFLICT", "La asistencia ya tiene otro estado."));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleConcurrentUpdate() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Instant.now(), 409, "CONCURRENT_UPDATE", "Los datos cambiaron durante la operación. Reintenta la consulta."));
    }

    @ExceptionHandler(DuplicateClassParticipationException.class)
    ResponseEntity<ApiError> handleDuplicateParticipation() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Instant.now(), 409, "DUPLICATE_PARTICIPATION", "El alumno ya está asignado a la clase."));
    }

    @ExceptionHandler(DuplicateSubjectException.class)
    ResponseEntity<ApiError> handleDuplicateSubject() {
        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), 400, "DUPLICATE_SUBJECT", "La solicitud contiene una materia repetida."));
    }

    @ExceptionHandler(InactiveStudentException.class)
    ResponseEntity<ApiError> handleInactiveStudent() {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(Instant.now(), 422, "INACTIVE_STUDENT", "No se puede asignar un alumno inactivo."));
    }

    @ExceptionHandler(InactiveSubjectException.class)
    ResponseEntity<ApiError> handleInactiveSubject() {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError(Instant.now(), 422, "INACTIVE_SUBJECT", "No se puede agregar una materia inactiva."));
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
                .body(new ApiError(Instant.now(), 409, "INVALID_CLASS_STATE", "La clase debe estar programada para esta operación."));
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
