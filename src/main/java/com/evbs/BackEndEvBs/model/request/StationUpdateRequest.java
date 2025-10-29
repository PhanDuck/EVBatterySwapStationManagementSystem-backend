package com.evbs.BackEndEvBs.model.request;

import com.evbs.BackEndEvBs.entity.Station;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StationUpdateRequest {

    private String name;

    private String location;

    private Integer capacity;

    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Số điện thoại liên lạc không hợp lệ! Phải là số điện thoại theo định dạng tiếng Việt.."
    )
    private String contactInfo;

    private String city;

    private String district;

    private Double latitude;

    private Double longitude;

    private Long batteryTypeId;
    
    // Optional status update
    private Station.Status status;
}