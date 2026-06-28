package com.invoicepe.controller;

import com.invoicepe.config.UserPrincipal;
import com.invoicepe.dto.request.UpdateProfileRequest;
import com.invoicepe.dto.request.UpdateWhatsAppConfigRequest;
import com.invoicepe.dto.response.ApiResponse;
import com.invoicepe.dto.response.UserProfileResponse;
import com.invoicepe.model.User;
import com.invoicepe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(UserProfileResponse.from(principal.getUser())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest request) {

        User user = principal.getUser();
        if (request.getBusinessName() != null) user.setBusinessName(request.getBusinessName());
        if (request.getGstin() != null) user.setGstin(request.getGstin());
        if (request.getPan() != null) user.setPan(request.getPan());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getBankAccountName() != null) user.setBankAccountName(request.getBankAccountName());
        if (request.getBankAccountNumber() != null) user.setBankAccountNumber(request.getBankAccountNumber());
        if (request.getBankIfsc() != null) user.setBankIfsc(request.getBankIfsc());
        if (request.getUpiId() != null) user.setUpiId(request.getUpiId());
        if (request.getLogoUrl() != null) user.setLogoUrl(request.getLogoUrl());

        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", UserProfileResponse.from(user)));
    }

    @PutMapping("/whatsapp-config")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateWhatsAppConfig(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateWhatsAppConfigRequest request) {

        User user = principal.getUser();
        if (request.getWhatsappEnabled() != null) user.setWhatsappEnabled(request.getWhatsappEnabled());
        if (request.getWhatsappPhoneNumberId() != null) user.setWhatsappPhoneNumberId(request.getWhatsappPhoneNumberId());
        if (request.getWhatsappAccessToken() != null) user.setWhatsappAccessToken(request.getWhatsappAccessToken());

        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp config updated", UserProfileResponse.from(user)));
    }
}
