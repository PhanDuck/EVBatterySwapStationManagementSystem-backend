package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.model.EmailDetail;
import com.evbs.BackEndEvBs.entity.Payment;
import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.SwapTransaction;
import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.entity.SupportTicket;
import com.evbs.BackEndEvBs.entity.TicketResponse;
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


    // ==================== SUPPORT TICKET EMAIL METHODS ====================

    /**
     * Gửi email thông báo ticket mới đến Staff (khi có stationID)
     *
     * @param staffList Danh sách staff của trạm
     * @param ticket Thông tin support ticket
     */
    public void sendTicketCreatedToStaff(java.util.List<User> staffList, SupportTicket ticket) {
        if (staffList == null || staffList.isEmpty()) {
            log.warn("Không có staff nào để gửi email cho ticket: {}", ticket.getId());
            return;
        }

        try {
            log.info("Đang gửi email thông báo ticket mới đến {} staff cho ticket: {}",
                    staffList.size(), ticket.getId());

            Context context = createTicketEmailContext(ticket);

            // Render template
            String htmlContent = templateEngine.process("ticket-created-staff", context);

            // Gửi email đến từng staff
            for (User staff : staffList) {
                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");

                    mimeMessageHelper.setFrom(fromEmail);
                    mimeMessageHelper.setTo(staff.getEmail());
                    mimeMessageHelper.setText(htmlContent, true);
                    mimeMessageHelper.setSubject("🚨 [URGENT] Ticket hỗ trợ mới từ khách hàng - #" + ticket.getId());

                    mailSender.send(mimeMessage);
                    log.info("Email đã được gửi đến staff: {}", staff.getEmail());

                } catch (MessagingException e) {
                    log.error("Lỗi khi gửi email đến staff {}: {}", staff.getEmail(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Lỗi khi tạo email template cho ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }

    /**
     * Gửi email thông báo ticket mới đến Admin (khi không có stationID)
     *
     * @param adminList Danh sách admin
     * @param ticket Thông tin support ticket
     */
    public void sendTicketCreatedToAdmin(java.util.List<User> adminList, SupportTicket ticket) {
        if (adminList == null || adminList.isEmpty()) {
            log.warn("Không có admin nào để gửi email cho ticket: {}", ticket.getId());
            return;
        }

        try {
            log.info("Đang gửi email thông báo ticket mới đến {} admin cho ticket: {}",
                    adminList.size(), ticket.getId());

            Context context = createTicketEmailContext(ticket);

            // Render template
            String htmlContent = templateEngine.process("ticket-created-staff", context);

            // Gửi email đến từng admin
            for (User admin : adminList) {
                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");

                    mimeMessageHelper.setFrom(fromEmail);
                    mimeMessageHelper.setTo(admin.getEmail());
                    mimeMessageHelper.setText(htmlContent, true);
                    mimeMessageHelper.setSubject("🚨 [ADMIN] Ticket hỗ trợ tổng quát mới - #" + ticket.getId());

                    mailSender.send(mimeMessage);
                    log.info("Email đã được gửi đến admin: {}", admin.getEmail());

                } catch (MessagingException e) {
                    log.error("Lỗi khi gửi email đến admin {}: {}", admin.getEmail(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Lỗi khi tạo email template cho ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }

    /**
     * Gửi email thông báo có phản hồi mới đến Driver
     * @param response Thông tin ticket response
     */
    public void sendTicketResponseToDriver(TicketResponse response) {
        try {
            log.info("Đang gửi email phản hồi ticket đến driver: {} cho ticket: {}",
                    response.getTicket().getDriver().getEmail(), response.getTicket().getId());

            Context context = createResponseEmailContext(response);

            // Render template
            String htmlContent = templateEngine.process("ticket-response-driver", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");

            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setTo(response.getTicket().getDriver().getEmail());
            mimeMessageHelper.setText(htmlContent, true);
            mimeMessageHelper.setSubject("💬 Có phản hồi mới cho ticket #" + response.getTicket().getId());

            mailSender.send(mimeMessage);

            log.info("Email phản hồi ticket đã được gửi thành công cho driver: {}",
                    response.getTicket().getDriver().getEmail());

        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email phản hồi ticket cho driver {}: {}",
                    response.getTicket().getDriver().getEmail(), e.getMessage());
        }
    }

    /**
     * Tạo context chứa dữ liệu cho email template support ticket
     */
    private Context createTicketEmailContext(SupportTicket ticket) {
        Context context = new Context();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // Thông tin ticket
        context.setVariable("ticketId", ticket.getId().toString());
        context.setVariable("subject", ticket.getSubject());
        context.setVariable("description", ticket.getDescription());
        context.setVariable("status", ticket.getStatus().toString());
        context.setVariable("createdAt", ticket.getCreatedAt().format(formatter));

        // Thông tin driver
        context.setVariable("driverName", ticket.getDriver().getFullName());
        context.setVariable("driverEmail", ticket.getDriver().getEmail());
        context.setVariable("driverPhone", ticket.getDriver().getPhoneNumber() != null ?
                ticket.getDriver().getPhoneNumber() : "Chưa cập nhật");

        // Thông tin trạm (nếu có)
        if (ticket.getStation() != null) {
            context.setVariable("hasStation", true);
            context.setVariable("stationName", ticket.getStation().getName());
            context.setVariable("stationLocation", ticket.getStation().getLocation());
            context.setVariable("stationContact", ticket.getStation().getContactInfo() != null ?
                    ticket.getStation().getContactInfo() : "Chưa cập nhật");
        } else {
            context.setVariable("hasStation", false);
            context.setVariable("ticketType", "Hỗ trợ tổng quát");
        }

        // Thông tin hệ thống
        context.setVariable("supportEmail", "sp.evswapstation@gmail.com");
        context.setVariable("systemName", "EV Battery Swap Station");

        return context;
    }

    /**
     * Tạo context chứa dữ liệu cho email template ticket response
     */
    private Context createResponseEmailContext(TicketResponse response) {
        Context context = new Context();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // Thông tin response
        context.setVariable("responseId", response.getId().toString());
        context.setVariable("responseMessage", response.getMessage());
        context.setVariable("responseTime", response.getResponseTime().format(formatter));

        // Thông tin staff trả lời
        context.setVariable("staffName", response.getStaff().getFullName());
        context.setVariable("staffRole", response.getStaff().getRole().toString());

        // Thông tin ticket gốc
        SupportTicket ticket = response.getTicket();
        context.setVariable("ticketId", ticket.getId().toString());
        context.setVariable("ticketSubject", ticket.getSubject());
        context.setVariable("ticketDescription", ticket.getDescription());
        context.setVariable("ticketCreatedAt", ticket.getCreatedAt().format(formatter));

        // Thông tin driver
        context.setVariable("driverName", ticket.getDriver().getFullName());

        // Thông tin trạm (nếu có)
        if (ticket.getStation() != null) {
            context.setVariable("hasStation", true);
            context.setVariable("stationName", ticket.getStation().getName());
            context.setVariable("stationLocation", ticket.getStation().getLocation());
        } else {
            context.setVariable("hasStation", false);
        }

        // Thông tin hệ thống
        context.setVariable("supportEmail", "sp.evswapstation@gmail.com");
        context.setVariable("systemName", "EV Battery Swap Station");

        return context;
    }
}