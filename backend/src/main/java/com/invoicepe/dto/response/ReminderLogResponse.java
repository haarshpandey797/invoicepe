package com.invoicepe.dto.response;

import com.invoicepe.model.ReminderLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReminderLogResponse {

    private UUID id;
    private String reminderType;
    private String channel;
    private LocalDateTime sentAt;
    private String status;
    private String errorMessage;

    public static ReminderLogResponse from(ReminderLog log) {
        return ReminderLogResponse.builder()
                .id(log.getId())
                .reminderType(log.getReminderType())
                .channel(log.getChannel())
                .sentAt(log.getSentAt())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .build();
    }
}
