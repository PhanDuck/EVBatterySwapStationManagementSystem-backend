package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TicketResponse")
@Getter
@Setter
public class TicketResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ResponseID")
    @JsonIgnore  // Ẩn ID trong response JSON
    private Long id;

    @ManyToOne
    @JoinColumn(name = "TicketID", nullable = false)
    @JsonIgnore
    private SupportTicket ticket;

    @ManyToOne
    @JoinColumn(name = "StaffID", nullable = false)
    @JsonIgnore
    private User staff;

    @NotEmpty(message = "Tin nhắn không được để trống!")
    @Column(name = "Message", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Column(name = "ResponseTime")
    private LocalDateTime responseTime = LocalDateTime.now();

    // Getter để expose staffId trong JSON
    @JsonProperty("staffId")
    public Long getStaffId() {
        return staff != null ? staff.getId() : null;
    }

    // Getter để expose staff name trong JSON
    @JsonProperty("staffName")
    public String getStaffName() {
        return staff != null ? staff.getFullName() : null;
    }

    // Getter để expose staff email trong JSON
    @JsonProperty("staffEmail")
    public String getStaffEmail() {
        return staff != null ? staff.getEmail() : null;
    }
}