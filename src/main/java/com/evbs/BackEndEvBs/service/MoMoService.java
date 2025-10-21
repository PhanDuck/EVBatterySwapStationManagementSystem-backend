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
import com.evbs.BackEndEvBs.repository.UserRepository;
import com.evbs.BackEndEvBs.util.MoMoUtil;
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

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // Tạo URL thanh toán MoMo cho gói dịch vụ (trả về paymentUrl để frontend redirect)
    public Map<String, String> createPaymentUrl(Long packageId, String customRedirectUrl) {
        // BUOC 1: Validate service package tồn tại
        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + packageId));

        // BUOC 2: Kiểm tra driver có gói active và còn lượt swap không
        User currentDriver = authenticationService.getCurrentUser();
        var activeSubscriptionOpt = driverSubscriptionRepository.findActiveSubscriptionByDriver(
                currentDriver,
                java.time.LocalDate.now()
        );

        if (activeSubscriptionOpt.isPresent()) {
            DriverSubscription existingSub = activeSubscriptionOpt.get();

            if (existingSub.getRemainingSwaps() > 0) {
                throw new AuthenticationException(
                        "Bạn đã có gói dịch vụ ACTIVE và còn " + existingSub.getRemainingSwaps() + " lượt swap! " +
                                "Vui lòng sử dụng hết lượt swap hiện tại trước khi mua gói mới."
                );
            }

            log.info("Driver {} có gói active nhưng hết lượt swap. Cho phép mua gói mới...",
                    currentDriver.getEmail());
        }

        // BUOC 3: Chuẩn bị thông tin thanh toán MoMo
        String orderId = MoMoUtil.generateOrderId();
        String requestId = MoMoUtil.generateRequestId();
        long amount = servicePackage.getPrice().longValue();
        
        // LƯU DRIVER ID vào extraData vì callback không có token!
        String extraData = "packageId=" + packageId + "&driverId=" + currentDriver.getId();

        // Xác định redirectUrl: Frontend gửi thì dùng, không thì dùng config fallback
        String finalRedirectUrl = (customRedirectUrl != null && !customRedirectUrl.trim().isEmpty()) 
                ? customRedirectUrl 
                : moMoConfig.getRedirectUrl();
        
        log.info("Using redirect URL: {}", finalRedirectUrl);

        // Parameters for signature (sorted by key)
        Map<String, String> signatureParams = new LinkedHashMap<>();
        signatureParams.put("accessKey", moMoConfig.getAccessKey());
        signatureParams.put("amount", String.valueOf(amount));
        signatureParams.put("extraData", extraData);
        signatureParams.put("ipnUrl", moMoConfig.getIpnUrl());
        signatureParams.put("orderId", orderId);
        signatureParams.put("orderInfo", "Thanh toan goi dich vu: " + servicePackage.getName());
        signatureParams.put("partnerCode", moMoConfig.getPartnerCode());
        signatureParams.put("redirectUrl", finalRedirectUrl);
        signatureParams.put("requestId", requestId);
        signatureParams.put("requestType", moMoConfig.getRequestType());

        // Build raw signature string
        String rawSignature = MoMoUtil.buildRawSignature(signatureParams);
        String signature = MoMoUtil.hmacSHA256(rawSignature, moMoConfig.getSecretKey());

        // BUOC 4: Build request body gửi đến MoMo
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", moMoConfig.getPartnerCode());
        requestBody.put("partnerName", "EVBattery Swap System");
        requestBody.put("storeId", "EVBatteryStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", "Thanh toan goi dich vu: " + servicePackage.getName());
        requestBody.put("redirectUrl", finalRedirectUrl); // Dùng redirectUrl từ frontend hoặc config
        requestBody.put("ipnUrl", moMoConfig.getIpnUrl());
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData); // Dùng extraData có cả packageId và driverId
        requestBody.put("requestType", moMoConfig.getRequestType());
        requestBody.put("signature", signature);

        // BUOC 5: Gọi MoMo API
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

                log.info("MoMo payment URL created for package {}: {} - {} VND",
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
            log.error("Lỗi tạo MoMo payment: {}", e.getMessage());
            throw new RuntimeException("Không thể tạo MoMo payment URL", e);
        }
    }

    // Xử lý IPN từ MoMo (JSON body) - tạo subscription nếu thanh toán thành công
    @Transactional
    public Map<String, Object> handleMoMoIPN(Map<String, String> momoData) {
        Map<String, Object> result = new HashMap<>();

        try {
            // BUOC 1: Lấy data từ JSON
            String partnerCode = momoData.get("partnerCode");
            String orderId = momoData.get("orderId");
            String requestId = momoData.get("requestId");
            String amount = momoData.get("amount");
            String orderInfo = momoData.get("orderInfo");
            String orderType = momoData.get("orderType");
            String transId = momoData.get("transId");
            String resultCode = momoData.get("resultCode");
            String message = momoData.get("message");
            String payType = momoData.get("payType");
            String responseTime = momoData.get("responseTime");
            String extraData = momoData.get("extraData");
            String signature = momoData.get("signature");

            log.info(" IPN - Nhận callback từ MoMo: orderId={}, resultCode={}, message={}",
                    orderId, resultCode, message);

            // BUOC 2: Verify signature
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
                log.error(" IPN - Signature không hợp lệ!");
                throw new SecurityException("Chữ ký MoMo không hợp lệ! Có thể bị giả mạo.");
            }

            log.info(" IPN - Signature hợp lệ - Request từ MoMo thật");

            // BUOC 3: Parse extraData
            Map<String, String> extraDataMap = parseExtraData(extraData);
            Long packageId = extractLong(extraDataMap, "packageId");
            Long driverId = extractLong(extraDataMap, "driverId");
            
            if (packageId == null || driverId == null) {
                throw new RuntimeException("Không thể lấy packageId hoặc driverId từ extraData: " + extraData);
            }

            ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ ID: " + packageId));

            // BUOC 4: Xử lý kết quả thanh toán
            if ("0".equals(resultCode)) {
                // THANH TOÁN THÀNH CÔNG
                log.info(" IPN - Thanh toán MoMo thành công: orderId={}, transId={}, driverId={}", 
                         orderId, transId, driverId);

                // Tạo subscription tự động
                DriverSubscription subscription = driverSubscriptionService.createSubscriptionAfterPayment(packageId, driverId);

                // Lưu Payment record
                Payment payment = new Payment();
                payment.setSubscription(subscription);
                payment.setAmount(new BigDecimal(amount));
                payment.setPaymentMethod("MOMO");
                payment.setPaymentDate(LocalDateTime.now());
                payment.setStatus(Payment.Status.COMPLETED);
                paymentRepository.save(payment);

                log.info("IPN - Đã lưu Payment và tạo Subscription ID: {}", subscription.getId());

                // Gửi email thông báo thanh toán thành công
                try {
                    User driver = userRepository.findById(driverId)
                            .orElseThrow(() -> new NotFoundException("Không tìm thấy driver ID: " + driverId));

                    emailService.sendPaymentSuccessEmail(driver, payment, servicePackage);
                    log.info("📧 Email thanh toán thành công đã được gửi cho driver: {}", driver.getEmail());

                } catch (Exception emailException) {
                    log.error("❌ Lỗi khi gửi email thanh toán thành công: {}", emailException.getMessage());
                    // Không throw exception để không ảnh hưởng đến flow thanh toán chính
                }

                result.put("success", true);
                result.put("message", "Thanh toán thành công! Gói dịch vụ đã được kích hoạt.");
                result.put("subscriptionId", subscription.getId());
                result.put("packageName", servicePackage.getName());
                result.put("maxSwaps", servicePackage.getMaxSwaps());
                result.put("remainingSwaps", subscription.getRemainingSwaps());
                result.put("startDate", subscription.getStartDate().toString());
                result.put("endDate", subscription.getEndDate().toString());
                result.put("amount", amount);
                result.put("transactionCode", transId);

            } else {
                // THANH TOÁN THẤT BẠI
                log.warn(" IPN - Thanh toán MoMo thất bại: orderId={}, resultCode={}, message={}",
                        orderId, resultCode, message);

                result.put("success", false);
                result.put("message", "Thanh toán thất bại: " + message);
                result.put("resultCode", resultCode);
            }

        } catch (Exception e) {
            log.error(" IPN - Lỗi xử lý callback MoMo: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "Lỗi xử lý thanh toán: " + e.getMessage());
        }

        return result;
    }

    // HELPER METHODS

    /**
     * PARSE EXTRADATA THÀNH MAP
     * 
     * Chuyển string "packageId=1&driverId=13" thành Map:
     * {
     *   "packageId": "1",
     *   "driverId": "13"
     * }
     * 
     * @param extraData String dạng "key1=value1&key2=value2"
     * @return Map<String, String>
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
     * LẤY GIÁ TRỊ LONG TỪ MAP
     * 
     * Lấy value từ map và parse thành Long
     * Nếu không parse được thì return null
     * 
     * @param map Map chứa data
     * @param key Key cần lấy
     * @return Long value hoặc null nếu không hợp lệ
     */
    private Long extractLong(Map<String, String> map, String key) {
        String value = map.get(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.error("Giá trị Long không hợp lệ cho key {}: {}", key, value);
            }
        }
        return null;
    }
}
