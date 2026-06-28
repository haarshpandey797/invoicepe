package com.invoicepe.service;

import com.invoicepe.exception.InvoicePeException;
import com.invoicepe.model.Client;
import com.invoicepe.model.Invoice;
import com.invoicepe.model.InvoiceItem;
import com.invoicepe.model.User;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates a professional A4 GST invoice PDF using iText 7.
 */
@Service
@Slf4j
public class PdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Color PRIMARY = new DeviceRgb(59, 130, 246);   // #3B82F6
    private static final Color GREY = new DeviceRgb(120, 120, 120);

    public byte[] generateInvoicePdf(Invoice invoice) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            try (Document doc = new Document(pdf, PageSize.A4)) {
                doc.setMargins(36, 36, 36, 36);
                addHeader(doc, invoice);
                addBillTo(doc, invoice);
                addItemsTable(doc, invoice);
                addTotals(doc, invoice);
                addFooter(doc, invoice);
            }
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}", invoice.getInvoiceNumber(), e);
            throw new InvoicePeException("Failed to generate invoice PDF");
        }
    }

    private void addHeader(Document doc, Invoice invoice) {
        User user = invoice.getUser();
        Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();

        Cell left = noBorder();
        left.add(new Paragraph("[ LOGO ]").setFontSize(9).setFontColor(GREY));
        left.add(new Paragraph(businessName(user)).setBold().setFontSize(18));
        if (notBlank(user.getGstin())) {
            left.add(new Paragraph("GSTIN: " + user.getGstin()).setFontSize(9));
        }
        if (notBlank(user.getAddress())) {
            left.add(new Paragraph(user.getAddress()).setFontSize(9).setFontColor(GREY));
        }

        Cell right = noBorder().setTextAlignment(TextAlignment.RIGHT);
        right.add(new Paragraph("TAX INVOICE").setBold().setFontSize(16).setFontColor(PRIMARY));
        right.add(new Paragraph("Invoice #: " + invoice.getInvoiceNumber()).setFontSize(10));
        right.add(new Paragraph("Issue Date: " + formatDate(invoice.getIssueDate())).setFontSize(10));
        right.add(new Paragraph("Due Date: " + formatDate(invoice.getDueDate())).setFontSize(10));

        header.addCell(left);
        header.addCell(right);
        doc.add(header);
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    private void addBillTo(Document doc, Invoice invoice) {
        Client client = invoice.getClient();
        doc.add(new Paragraph("BILL TO").setBold().setFontSize(11).setFontColor(PRIMARY));
        doc.add(new Paragraph(client.getName()).setBold().setFontSize(11));
        if (notBlank(client.getBillingAddress())) {
            doc.add(new Paragraph(client.getBillingAddress()).setFontSize(10).setFontColor(GREY));
        }
        if (notBlank(client.getGstin())) {
            doc.add(new Paragraph("GSTIN: " + client.getGstin()).setFontSize(10));
        }
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    private void addItemsTable(Document doc, Invoice invoice) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{44, 12, 10, 16, 18}))
                .useAllAvailableWidth();

        table.addHeaderCell(headerCell("Description", TextAlignment.LEFT));
        table.addHeaderCell(headerCell("HSN/SAC", TextAlignment.CENTER));
        table.addHeaderCell(headerCell("Qty", TextAlignment.CENTER));
        table.addHeaderCell(headerCell("Rate", TextAlignment.RIGHT));
        table.addHeaderCell(headerCell("Amount", TextAlignment.RIGHT));

        for (InvoiceItem item : invoice.getItems()) {
            table.addCell(bodyCell(item.getDescription(), TextAlignment.LEFT));
            table.addCell(bodyCell(item.getHsnSacCode(), TextAlignment.CENTER));
            table.addCell(bodyCell(plain(item.getQuantity()), TextAlignment.CENTER));
            table.addCell(bodyCell(money(item.getRate()), TextAlignment.RIGHT));
            table.addCell(bodyCell(money(item.getAmount()), TextAlignment.RIGHT));
        }
        doc.add(table);
    }

    private void addTotals(Document doc, Invoice invoice) {
        Table totals = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
        totals.setWidth(UnitValue.createPercentValue(45));
        totals.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        totals.setMarginTop(8);

        totalRow(totals, "Subtotal", money(invoice.getSubtotal()), false);

        boolean interState = invoice.getIgstAmount() != null
                && invoice.getIgstAmount().compareTo(BigDecimal.ZERO) > 0;
        if (interState) {
            totalRow(totals, "IGST (" + plain(invoice.getIgstRate()) + "%)",
                    money(invoice.getIgstAmount()), false);
        } else {
            totalRow(totals, "CGST (" + plain(invoice.getCgstRate()) + "%)",
                    money(invoice.getCgstAmount()), false);
            totalRow(totals, "SGST (" + plain(invoice.getSgstRate()) + "%)",
                    money(invoice.getSgstAmount()), false);
        }

        totalRow(totals, "Total", money(invoice.getTotalAmount()), true);
        doc.add(totals);
    }

    private void addFooter(Document doc, Invoice invoice) {
        User user = invoice.getUser();
        doc.add(new Paragraph("\n").setFontSize(6));
        doc.add(new Paragraph("Payment Details").setBold().setFontSize(11).setFontColor(PRIMARY));
        if (notBlank(user.getBankAccountName())) {
            doc.add(new Paragraph("Account Name: " + user.getBankAccountName()).setFontSize(10));
        }
        if (notBlank(user.getBankAccountNumber())) {
            doc.add(new Paragraph("Account No: " + user.getBankAccountNumber()).setFontSize(10));
        }
        if (notBlank(user.getBankIfsc())) {
            doc.add(new Paragraph("IFSC: " + user.getBankIfsc()).setFontSize(10));
        }
        if (notBlank(user.getUpiId())) {
            doc.add(new Paragraph("UPI: " + user.getUpiId()).setFontSize(10));
        }
        if (notBlank(invoice.getNotes())) {
            doc.add(new Paragraph("\nNotes: " + invoice.getNotes()).setFontSize(9).setFontColor(GREY));
        }
        doc.add(new Paragraph("Thank you for your business!")
                .setItalic().setTextAlignment(TextAlignment.CENTER).setFontColor(GREY)
                .setMarginTop(16));
    }

    // --- cell helpers --------------------------------------------------------

    private Cell headerCell(String text, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(10).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(PRIMARY)
                .setTextAlignment(align)
                .setPadding(5);
    }

    private Cell bodyCell(String text, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text == null || text.isBlank() ? "-" : text).setFontSize(9))
                .setTextAlignment(align)
                .setPadding(5);
    }

    private void totalRow(Table table, String label, String value, boolean bold) {
        Paragraph labelP = new Paragraph(label).setFontSize(10);
        Paragraph valueP = new Paragraph(value).setFontSize(10).setTextAlignment(TextAlignment.RIGHT);
        if (bold) {
            labelP.setBold().setFontSize(11);
            valueP.setBold().setFontSize(11);
        }
        Cell labelCell = noBorder().add(labelP);
        Cell valueCell = noBorder().add(valueP);
        if (bold) {
            labelCell.setBorderTop(new com.itextpdf.layout.borders.SolidBorder(GREY, 0.5f));
            valueCell.setBorderTop(new com.itextpdf.layout.borders.SolidBorder(GREY, 0.5f));
        }
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private Cell noBorder() {
        return new Cell().setBorder(Border.NO_BORDER);
    }

    // --- formatting ----------------------------------------------------------

    private String businessName(User user) {
        if (notBlank(user.getBusinessName())) {
            return user.getBusinessName();
        }
        return notBlank(user.getFullName()) ? user.getFullName() : "Invoice";
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    private String money(BigDecimal value) {
        BigDecimal v = (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
        return "Rs. " + v.toPlainString();
    }

    private String plain(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
