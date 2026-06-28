package com.invoicepe.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {
    private String businessName;
    private String gstin;
    private String pan;
    private String phone;
    private String address;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankIfsc;
    private String upiId;
    private String logoUrl;
}
