package com.evbs.BackEndEvBs.model;
import lombok.Data;

@Data
public class EmailDetail {
    String recipient;
    String subject;
    String fullName;

    // Booking details (optional - for booking confirmation emails)
    Long bookingId;
    String stationName;
    String stationLocation;
    String stationContact;
    String bookingTime;
    String vehicleModel;
    String batteryType;
    String status;
    String confirmationCode;  // Mã xác nhận swap pin
    String confirmedBy;       // Người xác nhận booking
}
