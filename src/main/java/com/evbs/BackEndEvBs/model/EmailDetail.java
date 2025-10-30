package com.evbs.BackEndEvBs.model;
import lombok.Data;

@Data
public class EmailDetail {
    String recipient;
    String subject;
    String fullName;
    String url;

    // Booking details
    Long bookingId;
    String stationName;
    String stationLocation;
    String stationContact;
    String bookingTime;
    String vehicleModel;
    String batteryType;
    String status;
    String confirmationCode;
    String confirmedBy;

    // Cancellation details
    String cancellationPolicy;
    String cancellationType; // "AUTO", "DRIVER", "STAFF"
    String cancellationReason; // <- THÊM TRƯỜNG NÀY (cho staff)
}