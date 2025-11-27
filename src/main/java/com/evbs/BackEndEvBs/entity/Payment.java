package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payment")
@Getter
@Setter
public class Payment {

    public enum Status {
        COMPLETED, FAILED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "TransactionID")
    @JsonIgnore
    private SwapTransaction transaction;

    @ManyToOne
    @JoinColumn(name = "SubscriptionID")
    @JsonIgnore
    private DriverSubscription subscription;

    @ManyToOne
    @JoinColumn(name = "VehicleID")
    @JsonIgnore
    private Vehicle vehicle;

    @Column(name = "Amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "PaymentMethod", length = 50)
    private String paymentMethod;

    @Column(name = "PaymentDate", nullable = false)
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private Status status = Status.COMPLETED;

    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("customerName")
    private String customerName;

    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("paymentType")
    private String paymentType;

    public String getCustomerName() {
        if (this.subscription != null && this.subscription.getDriver() != null) {
            return this.subscription.getDriver().getFullName();
        }
        if (this.transaction != null && this.transaction.getDriver() != null) {
            return this.transaction.getDriver().getFullName();
        }
        if (this.vehicle != null && this.vehicle.getDriver() != null) {
            return this.vehicle.getDriver().getFullName();
        }
        return null;
    }

    public String getPaymentType() {
        if (this.subscription != null) {
            return "Gói dịch vụ: " + this.subscription.getPackageName();
        }
        if (this.transaction != null) {
            return "Giao dịch đổi pin";
        }
        if (this.vehicle != null) {
            return "Cọc xe: " + this.vehicle.getPlateNumber();
        }
        return "Khác";
    }
    
}