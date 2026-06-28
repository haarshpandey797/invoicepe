package com.invoicepe.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * GST calculation per CLAUDE.md rules:
 * - Same state (user state == client state): CGST 9% + SGST 9% (18% total)
 * - Inter-state: IGST 18%
 *
 * The supply state is derived from the first 2 digits of the GSTIN (the state code).
 * If either party's state cannot be determined (missing GSTIN), we default to an
 * intra-state (CGST + SGST) transaction, which is the common case for local clients.
 */
@Component
public class GstCalculator {

    public static final BigDecimal INTRA_RATE = new BigDecimal("9.00");
    public static final BigDecimal INTER_RATE = new BigDecimal("18.00");
    private static final BigDecimal ZERO_RATE = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public GstBreakdown calculate(BigDecimal subtotal, String userGstin, String clientGstin) {
        BigDecimal base = subtotal.setScale(2, RoundingMode.HALF_UP);

        if (isInterState(userGstin, clientGstin)) {
            BigDecimal igst = percentage(base, INTER_RATE);
            return new GstBreakdown(base, ZERO_RATE, ZERO_RATE, INTER_RATE,
                    money(0), money(0), igst, base.add(igst));
        }

        BigDecimal cgst = percentage(base, INTRA_RATE);
        BigDecimal sgst = percentage(base, INTRA_RATE);
        return new GstBreakdown(base, INTRA_RATE, INTRA_RATE, ZERO_RATE,
                cgst, sgst, money(0), base.add(cgst).add(sgst));
    }

    /**
     * Inter-state when both GSTINs carry a state code and the codes differ.
     * Missing/short GSTIN -> treated as intra-state (default).
     */
    public boolean isInterState(String userGstin, String clientGstin) {
        String userState = stateCode(userGstin);
        String clientState = stateCode(clientGstin);
        if (userState == null || clientState == null) {
            return false;
        }
        return !userState.equals(clientState);
    }

    private String stateCode(String gstin) {
        if (gstin == null || gstin.trim().length() < 2) {
            return null;
        }
        return gstin.trim().substring(0, 2);
    }

    private BigDecimal percentage(BigDecimal base, BigDecimal rate) {
        return base.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(int value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
