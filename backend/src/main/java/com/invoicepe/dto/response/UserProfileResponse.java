package com.invoicepe.dto.response;

import com.invoicepe.model.User;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String fullName;
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
    private String plan;
    private Boolean whatsappEnabled;
    private String whatsappPhoneNumberId;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .businessName(user.getBusinessName())
                .gstin(user.getGstin())
                .pan(user.getPan())
                .phone(user.getPhone())
                .address(user.getAddress())
                .bankAccountName(user.getBankAccountName())
                .bankAccountNumber(user.getBankAccountNumber())
                .bankIfsc(user.getBankIfsc())
                .upiId(user.getUpiId())
                .logoUrl(user.getLogoUrl())
                .plan(user.getPlan())
                .whatsappEnabled(user.getWhatsappEnabled())
                .whatsappPhoneNumberId(user.getWhatsappPhoneNumberId())
                .build();
    }
}
