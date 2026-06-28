package com.invoicepe.repository;

import com.invoicepe.model.Client;
import com.invoicepe.model.Invoice;
import com.invoicepe.model.ReminderLog;
import com.invoicepe.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class InvoiceQueryRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private ReminderLogRepository reminderLogRepository;

    private User user;
    private Client acme;
    private Invoice acmeSent;

    @BeforeEach
    void setUp() {
        user = userRepository.save(user("owner@test.com"));
        acme = clientRepository.save(client(user, "Acme Corp"));
        Client globex = clientRepository.save(client(user, "Globex Ltd"));

        acmeSent = invoiceRepository.save(invoice(user, acme, "INV-A1", "sent"));
        invoiceRepository.save(invoice(user, acme, "INV-A2", "paid"));
        invoiceRepository.save(invoice(user, globex, "INV-G1", "sent"));

        // Another user's invoice must never leak into results.
        User other = userRepository.save(user("intruder@test.com"));
        Client otherClient = clientRepository.save(client(other, "Acme Corp"));
        invoiceRepository.save(invoice(other, otherClient, "INV-X1", "sent"));
    }

    @Test
    void returnsAllUserInvoicesWhenNoFilters() {
        Page<Invoice> page =
                invoiceRepository.searchForUser(user.getId(), null, null, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void filtersByStatus() {
        Page<Invoice> page =
                invoiceRepository.searchForUser(user.getId(), "sent", null, PageRequest.of(0, 10));
        assertThat(page.getContent())
                .extracting(Invoice::getInvoiceNumber)
                .containsExactlyInAnyOrder("INV-A1", "INV-G1");
    }

    @Test
    void filtersByClientNameCaseInsensitive() {
        Page<Invoice> page =
                invoiceRepository.searchForUser(user.getId(), null, "ACME", PageRequest.of(0, 10));
        assertThat(page.getContent())
                .extracting(Invoice::getInvoiceNumber)
                .containsExactlyInAnyOrder("INV-A1", "INV-A2");
    }

    @Test
    void combinesStatusAndSearch() {
        Page<Invoice> page =
                invoiceRepository.searchForUser(user.getId(), "sent", "globex", PageRequest.of(0, 10));
        assertThat(page.getContent())
                .extracting(Invoice::getInvoiceNumber)
                .containsExactly("INV-G1");
    }

    @Test
    void findsRemindersForInvoiceOnly() {
        reminderLogRepository.save(reminder(acmeSent, "pre_due", "sent"));
        reminderLogRepository.save(reminder(acmeSent, "on_due", "failed"));

        List<ReminderLog> logs =
                reminderLogRepository.findByInvoiceIdOrderBySentAtDesc(acmeSent.getId());

        assertThat(logs).hasSize(2);
        assertThat(logs).allMatch(l -> l.getInvoice().getId().equals(acmeSent.getId()));
    }

    private User user(String email) {
        return User.builder()
                .email(email)
                .passwordHash("hash")
                .fullName("Test User")
                .build();
    }

    private Client client(User owner, String name) {
        return Client.builder().user(owner).name(name).build();
    }

    private Invoice invoice(User owner, Client client, String number, String status) {
        return Invoice.builder()
                .user(owner)
                .client(client)
                .invoiceNumber(number)
                .status(status)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .subtotal(new BigDecimal("1000.00"))
                .totalAmount(new BigDecimal("1180.00"))
                .build();
    }

    private ReminderLog reminder(Invoice invoice, String type, String status) {
        return ReminderLog.builder()
                .invoice(invoice)
                .reminderType(type)
                .channel("whatsapp")
                .status(status)
                .build();
    }
}
