package com.academia.dto.request;

import com.academia.model.AttendanceMark;
import jakarta.validation.constraints.NotNull;

public record RecordAttendanceRequest(@NotNull AttendanceMark status) {
}
