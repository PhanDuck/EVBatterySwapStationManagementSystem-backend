package com.evbs.BackEndEvBs.entity;

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
@Table(name = "Users") // map với bảng Users trong DB
@Getter
@Setter
public class User implements UserDetails {

    public enum Role {
        DRIVER, STAFF, ADMIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Long id;

    @NotEmpty(message = "FullName cannot be empty!")
    @Column(name = "FullName", nullable = false, length = 150)
    private String fullName;

    @Email(message = "Email invalid!")
    @NotEmpty(message = "Email cannot be empty!")
    @Column(name = "Email", nullable = false, unique = true, length = 150)
    private String email;

    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Phone invalid!"
    )
    @Column(name = "PhoneNumber", unique = true, length = 30) // 👈 nên unique để login
    private String phoneNumber;

    @NotEmpty(message = "Password cannot be empty!")
    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false, length = 50)
    private Role role = Role.DRIVER; // default

    @Column(name = "Status", length = 50)
    private String status = "Active";

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
        return this.phoneNumber; // 👈 login bằng phone
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
        return "Active".equalsIgnoreCase(this.status);
    }

    @OneToMany(mappedBy = "user")
    List<Vehicle> vehicles;
}
