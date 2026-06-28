package com.invoicepe.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateWhatsAppConfigRequest {
    private Boolean whatsappEnabled;
    private String whatsappPhoneNumberId;
    private String whatsappAccessToken;
}
