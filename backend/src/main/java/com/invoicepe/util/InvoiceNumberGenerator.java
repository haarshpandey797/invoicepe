package com.invoicepe.util;

import com.invoicepe.model.User;
import com.invoicepe.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;

/**
 * Generates invoice numbers in the format INV-{YEAR}-{USER_PREFIX}-{SEQ}
 * e.g. INV-2024-RSH-047
 * - USER_PREFIX: first 3 alphanumeric chars of the business name (falls back to
 *   full name, then "INV"), uppercased and padded to 3 chars.
 * - SEQ: per-user count for the current year + 1, zero-padded to 3 digits.
 */
@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;

    public String generate(User user) {
        int year = Year.now().getValue();
        String prefix = prefixFor(user);
        long sequence = invoiceRepository.countByUserIdAndYear(user.getId(), year) + 1;

        String number = format(year, prefix, sequence);
        // Defend against collisions (e.g. concurrent creation) using the unique index.
        while (invoiceRepository.existsByInvoiceNumber(number)) {
            sequence++;
            number = format(year, prefix, sequence);
        }
        return number;
    }

    static String prefixFor(User user) {
        String source = firstNonBlank(user.getBusinessName(), user.getFullName(), "INV");
        return prefixFrom(source);
    }

    static String prefixFrom(String name) {
        String cleaned = (name == null ? "" : name).replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (cleaned.isEmpty()) {
            cleaned = "INV";
        }
        return (cleaned + "XXX").substring(0, 3);
    }

    static String format(int year, String prefix, long sequence) {
        return String.format("INV-%d-%s-%03d", year, prefix, sequence);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "INV";
    }
}
