package com.evbs.BackEndEvBs.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * MoMo Payment Configuration
 * Đọc config từ application.properties
 * 
 * Documentation: https://developers.momo.vn/#/docs/qr_payment
 */
@Configuration
@Getter
public class MoMoConfig {

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.partner.code}")
    private String partnerCode;

    @Value("${momo.access.key}")
    private String accessKey;

    @Value("${momo.secret.key}")
    private String secretKey;

    @Value("${momo.redirect.url}")
    private String redirectUrl;

    @Value("${momo.ipn.url}")
    private String ipnUrl;

    @Value("${momo.request.type}")
    private String requestType;
}
