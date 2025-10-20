package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.model.EmailDetail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;


    public void sendMailTemplate(EmailDetail emailDetail){
        try {
            Context context = new Context();
            context.setVariable("name", emailDetail.getFullName());

            String text = templateEngine.process("email-template", context);

            // creating a simple mail message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);


            // setting up necessary details
            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setTo(emailDetail.getRecipient());
            mimeMessageHelper.setText(text , true);
            mimeMessageHelper.setSubject(emailDetail.getSubject());
            mailSender.send(mimeMessage);


        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

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
}