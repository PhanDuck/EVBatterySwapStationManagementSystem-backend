package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.model.EmailDetail;
import com.evbs.BackEndEvBs.entity.Payment;
import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.SwapTransaction;
import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.DriverSubscription;
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
import java.time.LocalDateTime;
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
     * Gửi email thông báo đổi pin thành công
     *
     * @param driver Thông tin driver nhận email
     * @param swapTransaction Thông tin giao dịch đổi pin
     * @param subscription Thông tin subscription của driver (optional)
     */
    public void sendSwapSuccessEmail(User driver, SwapTransaction swapTransaction, DriverSubscription subscription) {
        try {
            log.info("Đang gửi email đổi pin thành công cho driver: {}", driver.getEmail());

            Context context = createSwapEmailContext(driver, swapTransaction, subscription);

            // Render template
            String htmlContent = templateEngine.process("swap-success-email", context);

            // Tạo email message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // Thiết lập thông tin email
            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setTo(driver.getEmail());
            mimeMessageHelper.setText(htmlContent, true);
            mimeMessageHelper.setSubject("🔋 Đổi pin thành công - EV Battery Swap Station");

            mailSender.send(mimeMessage);

            log.info("Email đổi pin thành công đã được gửi thành công cho: {}", driver.getEmail());

        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email đổi pin thành công cho {}: {}", driver.getEmail(), e.getMessage());
            // Không throw exception để không ảnh hưởng đến luồng đổi pin
            System.err.println("Failed to send swap success email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tạo context chứa dữ liệu cho email template đổi pin
     */
    private Context createSwapEmailContext(User driver, SwapTransaction swapTransaction, DriverSubscription subscription) {
        Context context = new Context();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // Thông tin driver
        context.setVariable("driverName", driver.getFullName());

        // Thông tin giao dịch đổi pin
        context.setVariable("swapId", swapTransaction.getId().toString());
        context.setVariable("swapTime", swapTransaction.getEndTime() != null ?
                swapTransaction.getEndTime().format(formatter) :
                LocalDateTime.now().format(formatter));

        // Thông tin xe
        if (swapTransaction.getVehicle() != null) {
            String vehicleInfo = swapTransaction.getVehicle().getPlateNumber();
            if (swapTransaction.getVehicle().getModel() != null) {
                vehicleInfo += " (" + swapTransaction.getVehicle().getModel() + ")";
            }
            context.setVariable("vehicleInfo", vehicleInfo);
        }

        // Thông tin nhân viên
        if (swapTransaction.getStaff() != null) {
            context.setVariable("staffName", swapTransaction.getStaff().getFullName());
        }

        // Thông tin trạm
        if (swapTransaction.getStation() != null) {
            context.setVariable("stationName", swapTransaction.getStation().getName());
            context.setVariable("stationLocation", swapTransaction.getStation().getLocation());
            context.setVariable("stationContact", swapTransaction.getStation().getContactInfo() != null ?
                    swapTransaction.getStation().getContactInfo() : "Chưa cập nhật");
        }

        // Thông tin pin cũ (được lấy ra)
        if (swapTransaction.getSwapInBattery() != null) {
            Battery oldBattery = swapTransaction.getSwapInBattery();
            context.setVariable("oldBatteryModel", oldBattery.getModel() != null ? oldBattery.getModel() : "N/A");
            context.setVariable("oldBatteryCharge", oldBattery.getChargeLevel() != null ?
                    oldBattery.getChargeLevel().intValue() : 0);
            context.setVariable("oldBatteryHealth", oldBattery.getStateOfHealth() != null ?
                    oldBattery.getStateOfHealth().intValue() : 0);
        }

        // Thông tin pin mới (được lắp vào)
        if (swapTransaction.getSwapOutBattery() != null) {
            Battery newBattery = swapTransaction.getSwapOutBattery();
            context.setVariable("newBatteryModel", newBattery.getModel() != null ? newBattery.getModel() : "N/A");
            context.setVariable("newBatteryCharge", newBattery.getChargeLevel() != null ?
                    newBattery.getChargeLevel().intValue() : 0);
            context.setVariable("newBatteryHealth", newBattery.getStateOfHealth() != null ?
                    newBattery.getStateOfHealth().intValue() : 0);
        }

        // Thông tin subscription (nếu có)
        if (subscription != null) {
            context.setVariable("remainingSwaps", subscription.getRemainingSwaps());
            context.setVariable("subscriptionInfo", "Gói dịch vụ đang hoạt động");
        }

        // Thông tin hệ thống
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