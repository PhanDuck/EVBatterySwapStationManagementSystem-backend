package com.example.EVBatterySwapStationManagementSystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User implements UserDetails {

    public enum Role {
        DRIVER, STAFF, ADMIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "FullName cannot be empty!")
    private String fullName;

    @Email(message = "Email invalid!")
    @Column(unique = true)
    private String email;

    private String gender;

    @NotEmpty(message = "Password cannot be empty!")
    private String password;

    @Pattern(regexp = "^(03|05|07|08|09)[0-9]{8}$", message = "Phone invalid!")
    @Column(unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role = Role.DRIVER; // Mặc định là DRIVER

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return this.phone; // Dùng phone làm username để login
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
        return true;
    }
}