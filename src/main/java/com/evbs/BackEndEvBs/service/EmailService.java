package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.model.EmailDetail;
import com.evbs.BackEndEvBs.entity.Payment;
import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.math.BigDecimal;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Gửi email xác nhận đặt lịch booking
     */
    public void sendBookingConfirmationEmail(EmailDetail emailDetail){
        try {
            Context context = new Context();
            context.setVariable("customerName", emailDetail.getFullName());
            context.setVariable("bookingId", emailDetail.getBookingId());
            context.setVariable("stationName", emailDetail.getStationName());
            context.setVariable("stationLocation", emailDetail.getStationLocation());
            context.setVariable("stationContact", emailDetail.getStationContact());
            context.setVariable("bookingTime", emailDetail.getBookingTime());
            context.setVariable("vehicleModel", emailDetail.getVehicleModel());
            context.setVariable("batteryType", emailDetail.getBatteryType());
            context.setVariable("status", emailDetail.getStatus());

            String text = templateEngine.process("booking-confirmation", context);

            // creating a simple mail message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // setting up necessary details
            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setTo(emailDetail.getRecipient());
            mimeMessageHelper.setText(text , true);
            mimeMessageHelper.setSubject(emailDetail.getSubject());
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            // Log error nhưng không throw exception để không ảnh hưởng đến luồng booking
            System.err.println("Failed to send booking confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Gửi email thông báo booking đã được confirm với confirmation code
     */
    public void sendBookingConfirmedEmail(EmailDetail emailDetail){
        try {
            Context context = new Context();
            context.setVariable("customerName", emailDetail.getFullName());
            context.setVariable("bookingId", emailDetail.getBookingId());
            context.setVariable("stationName", emailDetail.getStationName());
            context.setVariable("stationLocation", emailDetail.getStationLocation());
            context.setVariable("stationContact", emailDetail.getStationContact());
            context.setVariable("bookingTime", emailDetail.getBookingTime());
            context.setVariable("vehicleModel", emailDetail.getVehicleModel());
            context.setVariable("batteryType", emailDetail.getBatteryType());
            context.setVariable("status", emailDetail.getStatus());
            context.setVariable("confirmationCode", emailDetail.getConfirmationCode());
            context.setVariable("confirmedBy", emailDetail.getConfirmedBy());

            String text = templateEngine.process("booking-confirmed", context);

            // creating a simple mail message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // setting up necessary details
            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setTo(emailDetail.getRecipient());
            mimeMessageHelper.setText(text , true);
            mimeMessageHelper.setSubject(emailDetail.getSubject());
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            // Log error nhưng không throw exception để không ảnh hưởng đến luồng booking
            System.err.println("Failed to send booking confirmed email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gửi email thông báo thanh toán thành công
     *
     * @param driver Thông tin driver nhận email
     * @param payment Thông tin thanh toán
     * @param servicePackage Thông tin gói dịch vụ đã mua
     */
    public void sendPaymentSuccessEmail(User driver, Payment payment, ServicePackage servicePackage) {
        try {
            log.info("Đang gửi email thanh toán thành công cho driver: {}", driver.getEmail());

            Context context = createPaymentEmailContext(driver, payment, servicePackage);

            // Render template
            String htmlContent = templateEngine.process("payment-success-email", context);

            // Tạo email message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // Thiết lập thông tin email
            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setTo(driver.getEmail());
            mimeMessageHelper.setText(htmlContent, true);
            mimeMessageHelper.setSubject("🎉 Thanh toán thành công - Gói dịch vụ EV Battery Swap");

            mailSender.send(mimeMessage);

            log.info("Email thanh toán thành công đã được gửi thành công cho: {}", driver.getEmail());

        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email thanh toán thành công cho {}: {}", driver.getEmail(), e.getMessage());
            // Không throw exception để không ảnh hưởng đến luồng thanh toán
            System.err.println("Failed to send payment success email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tạo context chứa dữ liệu cho email template thanh toán
     */
    private Context createPaymentEmailContext(User driver, Payment payment, ServicePackage servicePackage) {
        Context context = new Context();

        // Thông tin driver
        context.setVariable("driverName", driver.getFullName());
        context.setVariable("driverEmail", driver.getEmail());

        // Thông tin gói dịch vụ
        context.setVariable("packageName", servicePackage.getName());
        context.setVariable("validDays", servicePackage.getDuration());
        context.setVariable("swapLimit", servicePackage.getMaxSwaps());
        context.setVariable("packageDescription", servicePackage.getDescription());

        // Thông tin thanh toán
        context.setVariable("paymentId", payment.getId());
        context.setVariable("transactionId", payment.getTransaction() != null ? payment.getTransaction().getId() : "N/A");
        context.setVariable("amount", formatCurrency(payment.getAmount()));
        context.setVariable("paymentMethod", payment.getPaymentMethod());
        context.setVariable("paymentStatus", payment.getStatus());

        // Format ngày giờ
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        context.setVariable("paymentDate", payment.getPaymentDate().format(formatter));

        // Thông tin hệ thống
        context.setVariable("systemName", "EV Battery Swap Station");
        context.setVariable("supportEmail", "sp.evswapstation@gmail.com");

        return context;
    }

    /**
     * Format số tiền theo định dạng Việt Nam
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + " VNĐ";
    }
}