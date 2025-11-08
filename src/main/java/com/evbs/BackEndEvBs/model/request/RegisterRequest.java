package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotEmpty(message = "FullName không được để trống!")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @NotEmpty(message = "Email không được để trống!")
    private String email;

    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Số điện thoại không hợp lệ!"
    )
    private String phoneNumber;

    @NotEmpty(message = "Mật khẩu không được để trống!")
    private String password;

    @NotEmpty(message = "Captcha không được để trống!")
    private String captchaToken;
}
