package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.model.response.CaptchaResponse;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class CaptchaService {

    @Value("${google.recaptcha.secret}")
    private String recaptchaSecret;

    @Value("${google.recaptcha.verify.url}")
    private String recaptchaVerifyUrl;

    /**
     * Xác thực Google reCAPTCHA token
     */
    public boolean verifyCaptcha(String captchaToken) {
        if (captchaToken == null || captchaToken.isEmpty()) {
            return false;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            
            // Tạo URL request
            String url = String.format("%s?secret=%s&response=%s", 
                recaptchaVerifyUrl, 
                recaptchaSecret, 
                captchaToken);

            // Gọi Google API để verify
            URI uri = new URI(url);
            String response = restTemplate.postForObject(uri, null, String.class);

            // Parse response
            Gson gson = new Gson();
            CaptchaResponse captchaResponse = gson.fromJson(response, CaptchaResponse.class);

            // Kiểm tra kết quả
            return captchaResponse.isSuccess();
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xác thực reCAPTCHA v3 với score threshold
     */
    public boolean verifyCaptchaV3(String captchaToken, double scoreThreshold) {
        if (captchaToken == null || captchaToken.isEmpty()) {
            return false;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            
            String url = String.format("%s?secret=%s&response=%s", 
                recaptchaVerifyUrl, 
                recaptchaSecret, 
                captchaToken);

            URI uri = new URI(url);
            String response = restTemplate.postForObject(uri, null, String.class);

            Gson gson = new Gson();
            CaptchaResponse captchaResponse = gson.fromJson(response, CaptchaResponse.class);

            // Với reCAPTCHA v3, kiểm tra cả success và score
            return captchaResponse.isSuccess() && captchaResponse.getScore() >= scoreThreshold;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
