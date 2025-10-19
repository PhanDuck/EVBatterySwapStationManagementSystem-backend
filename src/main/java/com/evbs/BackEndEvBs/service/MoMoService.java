package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.config.MoMoConfig;
import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.entity.Payment;
import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.DriverSubscriptionRepository;
import com.evbs.BackEndEvBs.repository.PaymentRepository;
import com.evbs.BackEndEvBs.repository.ServicePackageRepository;
import com.evbs.BackEndEvBs.util.MoMoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoMoService {

    @Autowired
    private final MoMoConfig moMoConfig;

    @Autowired
    private final ServicePackageRepository servicePackageRepository;

    @Autowired
    private final PaymentRepository paymentRepository;

    @Autowired
    private final DriverSubscriptionService driverSubscriptionService;

    @Autowired
    private final DriverSubscriptionRepository driverSubscriptionRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tạo MoMo payment URL cho gói dịch vụ
     * 
     * @param packageId ID của service package
     * @return Payment URL để redirect
     */
    public Map<String, String> createPaymentUrl(Long packageId) {
        // 1. Validate service package
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("Service package not found"));

        // 2. Validate driver không có gói active + còn lượt swap
        User currentDriver = authenticationService.getCurrentUser();
        var activeSubscriptionOpt = driverSubscriptionRepository.findActiveSubscriptionByDriver(
                currentDriver,
                java.time.LocalDate.now()
        );

        if (activeSubscriptionOpt.isPresent()) {
            DriverSubscription existingSub = activeSubscriptionOpt.get();

            if (existingSub.getRemainingSwaps() > 0) {
                throw new AuthenticationException(
                        "❌ Bạn đã có gói dịch vụ ACTIVE và còn " + existingSub.getRemainingSwaps() + " lượt swap! " +
                                "Vui lòng sử dụng hết lượt swap hiện tại trước khi mua gói mới."
                );
            }

            log.info("🔄 Driver {} has active subscription but 0 swaps remaining. Allowing new purchase...",
                    currentDriver.getEmail());
        }

        // 3. Build MoMo request parameters
        String orderId = MoMoUtil.generateOrderId();
        String requestId = MoMoUtil.generateRequestId();
        long amount = servicePackage.getPrice().longValue();
        
        // ⚠️ LƯU DRIVER ID vào extraData vì callback không có token!
        String extraData = "packageId=" + packageId + "&driverId=" + currentDriver.getId();

        // Parameters for signature (sorted by key)
        Map<String, String> signatureParams = new LinkedHashMap<>();
        signatureParams.put("accessKey", moMoConfig.getAccessKey());
        signatureParams.put("amount", String.valueOf(amount));
        signatureParams.put("extraData", extraData);
        signatureParams.put("ipnUrl", moMoConfig.getIpnUrl());
        signatureParams.put("orderId", orderId);
        signatureParams.put("orderInfo", "Thanh toan goi dich vu: " + servicePackage.getName());
        signatureParams.put("partnerCode", moMoConfig.getPartnerCode());
        signatureParams.put("redirectUrl", moMoConfig.getRedirectUrl());
        signatureParams.put("requestId", requestId);
        signatureParams.put("requestType", moMoConfig.getRequestType());

        // Build raw signature string
        String rawSignature = MoMoUtil.buildRawSignature(signatureParams);
        String signature = MoMoUtil.hmacSHA256(rawSignature, moMoConfig.getSecretKey());

        // 4. Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", moMoConfig.getPartnerCode());
        requestBody.put("partnerName", "EVBattery Swap System");
        requestBody.put("storeId", "EVBatteryStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", "Thanh toan goi dich vu: " + servicePackage.getName());
        requestBody.put("redirectUrl", moMoConfig.getRedirectUrl());
        requestBody.put("ipnUrl", moMoConfig.getIpnUrl());
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData); // ⚠️ Dùng extraData có cả packageId và driverId
        requestBody.put("requestType", moMoConfig.getRequestType());
        requestBody.put("signature", signature);

        // 5. Call MoMo API
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    moMoConfig.getEndpoint(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.get("resultCode").equals(0)) {
                String payUrl = (String) responseBody.get("payUrl");

                log.info("🔗 MoMo payment URL created for package {}: {} - {} VND",
                        packageId, servicePackage.getName(), amount);

                Map<String, String> result = new HashMap<>();
                result.put("paymentUrl", payUrl);
                result.put("orderId", orderId);
                result.put("requestId", requestId);
                result.put("message", "Redirect user to this URL to complete payment");

                return result;
            } else {
                throw new RuntimeException("MoMo API error: " + responseBody);
            }

        } catch (Exception e) {
            log.error("❌ Error creating MoMo payment: {}", e.getMessage());
            throw new RuntimeException("Failed to create MoMo payment", e);
        }
    }

    /**
     * Xử lý callback từ MoMo sau khi thanh toán
     * 
     * @param request HttpServletRequest containing callback params
     * @return Result map
     */
    @Transactional
    public Map<String, Object> handleMoMoReturn(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Lấy parameters từ MoMo
            String partnerCode = request.getParameter("partnerCode");
            String orderId = request.getParameter("orderId");
            String requestId = request.getParameter("requestId");
            String amount = request.getParameter("amount");
            String orderInfo = request.getParameter("orderInfo");
            String orderType = request.getParameter("orderType");
            String transId = request.getParameter("transId");
            String resultCode = request.getParameter("resultCode");
            String message = request.getParameter("message");
            String payType = request.getParameter("payType");
            String responseTime = request.getParameter("responseTime");
            String extraData = request.getParameter("extraData");
            String signature = request.getParameter("signature");

            log.info("📱 MoMo callback received: orderId={}, resultCode={}, message={}",
                    orderId, resultCode, message);

            // 2. Verify signature
            Map<String, String> signatureParams = new LinkedHashMap<>();
            signatureParams.put("accessKey", moMoConfig.getAccessKey());
            signatureParams.put("amount", amount);
            signatureParams.put("extraData", extraData != null ? extraData : "");
            signatureParams.put("message", message);
            signatureParams.put("orderId", orderId);
            signatureParams.put("orderInfo", orderInfo);
            signatureParams.put("orderType", orderType);
            signatureParams.put("partnerCode", partnerCode);
            signatureParams.put("payType", payType);
            signatureParams.put("requestId", requestId);
            signatureParams.put("responseTime", responseTime);
            signatureParams.put("resultCode", resultCode);
            signatureParams.put("transId", transId);

            String rawSignature = MoMoUtil.buildRawSignature(signatureParams);
            String calculatedSignature = MoMoUtil.hmacSHA256(rawSignature, moMoConfig.getSecretKey());

            if (!calculatedSignature.equals(signature)) {
                throw new SecurityException("❌ Invalid MoMo signature!");
            }

            // 3. Extract packageId và driverId từ extraData
            Map<String, String> extraDataMap = parseExtraData(extraData);
            Long packageId = extractLong(extraDataMap, "packageId");
            Long driverId = extractLong(extraDataMap, "driverId");
            
            if (packageId == null || driverId == null) {
                throw new RuntimeException("❌ Cannot extract packageId or driverId from extraData: " + extraData);
            }

            ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                    .orElseThrow(() -> new NotFoundException("Service package not found"));

            // 4. Xử lý theo result code
            if ("0".equals(resultCode)) {
                // ✅ Thanh toán thành công
                log.info("✅ MoMo payment successful: orderId={}, transId={}, driverId={}", orderId, transId, driverId);

                // Tạo subscription (dùng overload method với driverId vì không có token)
                DriverSubscription subscription = driverSubscriptionService.createSubscriptionAfterPayment(packageId, driverId);

                // Tạo payment record
                Payment payment = new Payment();
                payment.setSubscription(subscription);
                payment.setAmount(new BigDecimal(amount));
                payment.setPaymentMethod("MOMO");
                payment.setPaymentDate(LocalDateTime.now());
                payment.setStatus(Payment.Status.COMPLETED);
                paymentRepository.save(payment);

                result.put("success", true);
                result.put("message", "✅ Thanh toán thành công! Gói dịch vụ đã được kích hoạt.");
                result.put("subscriptionId", subscription.getId());
                result.put("packageName", servicePackage.getName());
                result.put("maxSwaps", servicePackage.getMaxSwaps());
                result.put("remainingSwaps", subscription.getRemainingSwaps());
                result.put("startDate", subscription.getStartDate().toString());
                result.put("endDate", subscription.getEndDate().toString());
                result.put("amount", amount);
                result.put("transactionCode", transId);

            } else {
                // ❌ Thanh toán thất bại
                log.warn("⚠️ MoMo payment failed: orderId={}, resultCode={}, message={}",
                        orderId, resultCode, message);

                result.put("success", false);
                result.put("message", "❌ Thanh toán thất bại: " + message);
                result.put("resultCode", resultCode);
            }

        } catch (Exception e) {
            log.error("❌ Error handling MoMo callback: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "❌ Lỗi xử lý thanh toán: " + e.getMessage());
        }

        return result;
    }

    /**
     * Parse extraData thành Map
     * Format: "packageId=1&driverId=13"
     */
    private Map<String, String> parseExtraData(String extraData) {
        Map<String, String> result = new HashMap<>();
        if (extraData != null && !extraData.isEmpty()) {
            String[] pairs = extraData.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    result.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return result;
    }

    /**
     * Extract Long từ Map
     */
    private Long extractLong(Map<String, String> map, String key) {
        String value = map.get(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.error("Invalid Long value for key {}: {}", key, value);
            }
        }
        return null;
    }
}
