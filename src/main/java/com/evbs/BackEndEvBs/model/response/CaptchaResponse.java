package com.evbs.BackEndEvBs.model.response;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CaptchaResponse {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("challenge_ts")
    private String challengeTs;
    
    @SerializedName("hostname")
    private String hostname;
    
    @SerializedName("score")
    private double score; // Cho reCAPTCHA v3
    
    @SerializedName("action")
    private String action; // Cho reCAPTCHA v3
    
    @SerializedName("error-codes")
    private List<String> errorCodes;
}
