package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "Users") // map với bảng Users trong DB
@Getter
@Setter
public class User implements UserDetails {

    public enum Role {
        DRIVER, STAFF, ADMIN
    }

    public enum Status {
        ACTIVE, INACTIVE, SUSPENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Long id;

    @NotEmpty(message = "FullName không được để trống!")
    @Column(name = "FullName", nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
    private String fullName;

    @Email(message = "Email không hợp lệ!")
    @NotEmpty(message = "Email không được để trống!")
    @Column(name = "Email", nullable = false, unique = true, length = 150)
    private String email;

    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Số điện thoại không hợp lệ!"
    )
    @Column(name = "PhoneNumber", unique = true, length = 30) // nên unique để login
    private String phoneNumber;

    @NotEmpty(message = "Mật khẩu không được để trống!")
    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false, length = 50)
    private Role role = Role.DRIVER; // default

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private Status status = Status.ACTIVE;
    
    // Relationships
    @OneToMany(mappedBy = "driver")
    @JsonIgnore
    private List<Vehicle> vehicles = new ArrayList<>();
    
    @OneToMany(mappedBy = "driver")
    @JsonIgnore
    private List<Booking> bookings = new ArrayList<>();
    
    @OneToMany(mappedBy = "driver")
    @JsonIgnore
    private List<SwapTransaction> driverTransactions = new ArrayList<>();
    
    @OneToMany(mappedBy = "staff")
    @JsonIgnore
    private List<SwapTransaction> staffTransactions = new ArrayList<>();
    
    @OneToMany(mappedBy = "driver")
    @JsonIgnore
    private List<DriverSubscription> subscriptions = new ArrayList<>();
    
    @OneToMany(mappedBy = "driver")
    @JsonIgnore
    private List<SupportTicket> supportTickets = new ArrayList<>();
    
    @OneToMany(mappedBy = "staff")
    @JsonIgnore
    private List<TicketResponse> ticketResponses = new ArrayList<>();
    
    // ===============================
    // Implement UserDetails
    // ===============================
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.phoneNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Status.ACTIVE.equals(this.status);
    }

}
