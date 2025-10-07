package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SupportTicket")
@Getter
@Setter
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "DriverID", nullable = false)
    @JsonIgnore
    private User driver;

    @ManyToOne
    @JoinColumn(name = "StationID")
    @JsonIgnore
    private Station station;

    @NotEmpty(message = "Subject cannot be empty!")
    @Column(name = "Subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "Status", length = 50)
    private String status = "Open";

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "ticket")
    @JsonIgnore
    private List<TicketResponse> responses = new ArrayList<>();
}