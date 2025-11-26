package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.config.MoMoConfig;
import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.response.UpgradeCalculationResponse;
import com.evbs.BackEndEvBs.model.response.RenewalCalculationResponse;
import com.evbs.BackEndEvBs.repository.DriverSubscriptionRepository;
import com.evbs.BackEndEvBs.repository.PaymentRepository;
import com.evbs.BackEndEvBs.repository.ServicePackageRepository;
import com.evbs.BackEndEvBs.repository.UserRepository;
import com.evbs.BackEndEvBs.util.MoMoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Autowired
    private com.evbs.BackEndEvBs.repository.VehicleRepository vehicleRepository;

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
                LocalDate.now()
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
                throw new RuntimeException("Lỗi mạng!");
            }

        } catch (Exception e) {
            log.error("Lỗi tạo MoMo payment: {}", e.getMessage());
            throw new RuntimeException("Lỗi mạng!", e);
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

            // BUOC 3.5: Kiểm tra loại thanh toán (NEW, UPGRADE, RENEWAL, DEPOSIT)
            String paymentType = extraDataMap.getOrDefault("type", "NEW");
            boolean isUpgrade = "UPGRADE".equals(paymentType);
            boolean isRenewal = "RENEWAL".equals(paymentType);
            boolean isDeposit = "DEPOSIT".equals(paymentType);

            // BUOC 4: Xử lý kết quả thanh toán
            if ("0".equals(resultCode)) {
                // THANH TOÁN THÀNH CÔNG
                log.info(" IPN - Thanh toán MoMo thành công: orderId={}, transId={}, type={}",
                        orderId, transId, paymentType);

                if (isDeposit) {
                    // XỬ LÝ THANH TOÁN TIỀN CỌC PIN
                    log.info("IPN - Processing DEPOSIT payment...");
                    Long vehicleId = extractLong(extraDataMap, "vehicleId");
                    Long driverId = extractLong(extraDataMap, "driverId");

                    if (vehicleId == null || driverId == null) {
                        throw new RuntimeException("Missing vehicleId or driverId in extraData!");
                    }

                    // Cập nhật deposit status trong vehicle
                    Vehicle vehicle = vehicleRepository.findById(vehicleId)
                            .orElseThrow(() -> new NotFoundException("Không tìm thấy xe với ID: " + vehicleId));

                    vehicle.setDepositStatus("PAID");
                    vehicle.setStatus(Vehicle.VehicleStatus.PENDING);
                    vehicleRepository.save(vehicle);

                    log.info("Deposit paid successfully for vehicle ID: {}, changing status to PENDING", vehicleId);

                    // GỬI EMAIL THÔNG BÁO CHO ADMIN
                    try {
                        List<User> adminList = userRepository.findByRole(User.Role.ADMIN);
                        if (!adminList.isEmpty()) {
                            emailService.sendVehicleRequestToAdmin(adminList, vehicle);
                            log.info("Sent vehicle request email to {} admins", adminList.size());
                        }
                    } catch (Exception e) {
                        log.error("Lỗi khi gửi email thông báo cho admin: {}", e.getMessage());
                    }

                    result.put("success", true);
                    result.put("message", "Thanh toán tiền cọc pin thành công! Xe của bạn đang chờ admin duyệt.");
                    result.put("paymentType", paymentType);
                    result.put("vehicleId", vehicleId);
                    result.put("amount", amount);
                    result.put("transactionCode", transId);
                    result.put("depositStatus", "PAID");

                    log.info("IPN - Deposit payment processed successfully for vehicle ID: {}", vehicleId);

                } else {
                    // XỬ LÝ THANH TOÁN GÓI DỊCH VỤ
                    Long packageId = extractLong(extraDataMap, "packageId");
                    Long driverId = extractLong(extraDataMap, "driverId");

                    if (packageId == null || driverId == null) {
                        throw new RuntimeException("Lỗi mạng!");
                    }

                    ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ ID: " + packageId));

                    DriverSubscription subscription;

                    if (isUpgrade) {
                        // XỬ LÝ UPGRADE GÓI
                        log.info("IPN - Processing UPGRADE payment...");
                        subscription = driverSubscriptionService.upgradeSubscriptionAfterPayment(packageId, driverId);
                    } else if (isRenewal) {
                        // XỬ LÝ RENEWAL/GIA HẠN
                        log.info("IPN - Processing RENEWAL payment...");
                        subscription = driverSubscriptionService.renewSubscriptionAfterPayment(packageId, driverId);
                    } else {
                        // XỬ LÝ MUA GÓI MỚI
                        log.info("IPN - Processing NEW PURCHASE payment...");
                        subscription = driverSubscriptionService.createSubscriptionAfterPayment(packageId, driverId);
                    }

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
                        log.info(" Email thanh toán thành công đã được gửi cho driver: {}", driver.getEmail());

                    } catch (Exception emailException) {
                        log.error(" Lỗi khi gửi email thanh toán thành công: {}", emailException.getMessage());
                        // Không throw exception để không ảnh hưởng đến flow thanh toán chính
                    }

                    result.put("success", true);
                    result.put("message", isUpgrade ?
                            "Nâng cấp gói thành công! Gói mới đã được kích hoạt." :
                            (isRenewal ?
                                    "Gia hạn gói thành công! Gói đã được gia hạn." :
                                    "Thanh toán thành công! Gói dịch vụ đã được kích hoạt."));
                    result.put("paymentType", paymentType);
                    result.put("subscriptionId", subscription.getId());
                    result.put("packageName", servicePackage.getName());
                    result.put("maxSwaps", servicePackage.getMaxSwaps());
                    result.put("remainingSwaps", subscription.getRemainingSwaps());
                    result.put("startDate", subscription.getStartDate().toString());
                    result.put("endDate", subscription.getEndDate().toString());
                    result.put("amount", amount);
                    result.put("transactionCode", transId);
                }

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
     * TẠO PAYMENT URL CHO NÂNG CẤP GÓI (UPGRADE)
     *
     * Tính toán theo công thức:
     * 1. Giá trị hoàn lại = (Lượt chưa dùng) × (Giá gói cũ / Tổng lượt gói cũ)
     * 2. Số tiền cần trả = Giá gói mới - Giá trị hoàn lại
     *
     * Ví dụ:
     * - Gói cũ: 20 lượt = 400,000đ (đã dùng 5, còn 15)
     * - Gói mới: 50 lượt = 800,000đ
     * - Giá trị hoàn lại = 15 × (400,000 / 20) = 15 × 20,000 = 300,000đ
     * - Tổng tiền = 800,000 - 300,000 = 500,000đ
     *
     * @param newPackageId ID gói mới
     * @param customRedirectUrl URL redirect sau khi thanh toán (optional)
     * @return Map chứa paymentUrl, orderId, requestId
     */
    public Map<String, String> createUpgradePaymentUrl(Long newPackageId, String customRedirectUrl) {
        User currentDriver = authenticationService.getCurrentUser();

        // 1. Tính toán chi phí upgrade
        UpgradeCalculationResponse calculation = driverSubscriptionService.calculateUpgradeCost(newPackageId);

        if (!calculation.getCanUpgrade()) {
            throw new IllegalStateException("Không thể nâng cấp gói: " + calculation.getMessage());
        }

        // 2. Lấy số tiền cần thanh toán từ calculation
        long amount = calculation.getTotalPaymentRequired().longValue();

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Số tiền thanh toán không hợp lệ: " + amount + " VNĐ. " +
                            "Vui lòng kiểm tra lại tính toán."
            );
        }

        // 3. Chuẩn bị thông tin thanh toán MoMo
        String orderId = MoMoUtil.generateOrderId();
        String requestId = MoMoUtil.generateRequestId();

        // LƯU THÔNG TIN UPGRADE vào extraData
        // Format: packageId=<newPackageId>&driverId=<id>&type=UPGRADE
        String extraData = String.format(
                "packageId=%d&driverId=%d&type=UPGRADE",
                newPackageId,
                currentDriver.getId()
        );

        String finalRedirectUrl = (customRedirectUrl != null && !customRedirectUrl.trim().isEmpty())
                ? customRedirectUrl
                : moMoConfig.getRedirectUrl();

        log.info("UPGRADE - Creating MoMo payment URL: Driver={}, OldPackage={}, NewPackage={}, Amount={} VND",
                currentDriver.getEmail(),
                calculation.getCurrentPackageName(),
                calculation.getNewPackageName(),
                amount
        );

        // 4. Build signature parameters
        Map<String, String> signatureParams = new LinkedHashMap<>();
        signatureParams.put("accessKey", moMoConfig.getAccessKey());
        signatureParams.put("amount", String.valueOf(amount));
        signatureParams.put("extraData", extraData);
        signatureParams.put("ipnUrl", moMoConfig.getIpnUrl());
        signatureParams.put("orderId", orderId);
        signatureParams.put("orderInfo", "Nang cap goi: " + calculation.getCurrentPackageName() +
                " → " + calculation.getNewPackageName());
        signatureParams.put("partnerCode", moMoConfig.getPartnerCode());
        signatureParams.put("redirectUrl", finalRedirectUrl);
        signatureParams.put("requestId", requestId);
        signatureParams.put("requestType", moMoConfig.getRequestType());

        String rawSignature = MoMoUtil.buildRawSignature(signatureParams);
        String signature = MoMoUtil.hmacSHA256(rawSignature, moMoConfig.getSecretKey());

        // 5. Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", moMoConfig.getPartnerCode());
        requestBody.put("partnerName", "EVBattery Swap System - Upgrade");
        requestBody.put("storeId", "EVBatteryStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", "Nang cap goi: " + calculation.getCurrentPackageName() +
                " → " + calculation.getNewPackageName());
        requestBody.put("redirectUrl", finalRedirectUrl);
        requestBody.put("ipnUrl", moMoConfig.getIpnUrl());
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", moMoConfig.getRequestType());
        requestBody.put("signature", signature);

        // 6. Gọi MoMo API
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

                log.info("UPGRADE - MoMo payment URL created: OrderID={}, Amount={} VND",
                        orderId, amount);

                Map<String, String> result = new HashMap<>();
                result.put("paymentUrl", payUrl);
                result.put("orderId", orderId);
                result.put("requestId", requestId);
                result.put("amount", String.valueOf(amount));
                result.put("upgradeType", "PACKAGE_UPGRADE");
                result.put("oldPackage", calculation.getCurrentPackageName());
                result.put("newPackage", calculation.getNewPackageName());
                result.put("refundValue", calculation.getRefundValue().toString());
                result.put("upgradeFee", calculation.getUpgradeFee().toString());
                result.put("message", "Redirect user to this URL to complete upgrade payment");

                return result;
            } else {
                throw new RuntimeException("Lỗi mạng!");
            }

        } catch (Exception e) {
            log.error("UPGRADE - Lỗi tạo MoMo payment: {}", e.getMessage());
            throw new RuntimeException("Lỗi mạng!", e);
        }
    }

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

    /**
     * TẠO URL THANH TOÁN MOMO CHO GIA HẠN GÓI (RENEWAL)
     *
     * Hỗ trợ flexible renewal:
     * - Early renewal: Được discount và stack swaps
     * - Late renewal: Không discount, reset swaps
     * - Allow package change: Renew sang gói khác
     *
     * @param renewalPackageId ID của gói muốn gia hạn
     * @param customRedirectUrl URL redirect sau payment (optional, nếu null thì dùng config)
     * @return Map chứa payUrl và orderId
     */
    public Map<String, String> createRenewalPaymentUrl(Long renewalPackageId, String customRedirectUrl) {
        // BUOC 1: Tính toán cost gia hạn (có discount nếu early renewal)
        RenewalCalculationResponse calculation = driverSubscriptionService.calculateRenewalCost(renewalPackageId);

        if (!calculation.getCanRenew()) {
            throw new AuthenticationException(calculation.getMessage());
        }

        User currentDriver = authenticationService.getCurrentUser();

        // BUOC 2: Chuẩn bị thông tin thanh toán
        String orderId = MoMoUtil.generateOrderId();
        String requestId = MoMoUtil.generateRequestId();
        long amount = calculation.getFinalPrice().longValue(); // Đã trừ discount

        // QUAN TRỌNG: Thêm type=RENEWAL để callback biết đây là renewal
        String extraData = String.format("packageId=%d&driverId=%d&type=RENEWAL",
                renewalPackageId, currentDriver.getId());

        // Xác định redirectUrl
        String finalRedirectUrl = (customRedirectUrl != null && !customRedirectUrl.trim().isEmpty())
                ? customRedirectUrl
                : moMoConfig.getRedirectUrl();

        log.info("RENEWAL Payment - Driver: {} | Package: {} | Amount: {} VNĐ (discount: {} VNĐ) | Type: {}",
                currentDriver.getEmail(),
                calculation.getRenewalPackageName(),
                amount,
                calculation.getTotalDiscount().longValue(),
                calculation.getRenewalType()
        );

        // BUOC 3: Signature parameters
        Map<String, String> signatureParams = new LinkedHashMap<>();
        signatureParams.put("accessKey", moMoConfig.getAccessKey());
        signatureParams.put("amount", String.valueOf(amount));
        signatureParams.put("extraData", extraData);
        signatureParams.put("ipnUrl", moMoConfig.getIpnUrl());
        signatureParams.put("orderId", orderId);
        signatureParams.put("orderInfo", "Gia han goi: " + calculation.getRenewalPackageName());
        signatureParams.put("partnerCode", moMoConfig.getPartnerCode());
        signatureParams.put("redirectUrl", finalRedirectUrl);
        signatureParams.put("requestId", requestId);
        signatureParams.put("requestType", moMoConfig.getRequestType());

        String rawSignature = MoMoUtil.buildRawSignature(signatureParams);
        String signature = MoMoUtil.hmacSHA256(rawSignature, moMoConfig.getSecretKey());

        // BUOC 4: Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", moMoConfig.getPartnerCode());
        requestBody.put("partnerName", "EVBattery Swap System");
        requestBody.put("storeId", "EVBatteryStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", "Gia han goi: " + calculation.getRenewalPackageName());
        requestBody.put("redirectUrl", finalRedirectUrl);
        requestBody.put("ipnUrl", moMoConfig.getIpnUrl());
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", moMoConfig.getRequestType());
        requestBody.put("signature", signature);

        // BUOC 5: Gọi MoMo API
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    moMoConfig.getEndpoint(),
                    entity,
                    Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !"0".equals(String.valueOf(responseBody.get("resultCode")))) {
                String errorMessage = responseBody != null
                        ? (String) responseBody.get("message")
                        : "Unknown error";

                log.error("RENEWAL - MoMo payment URL creation failed: {}", errorMessage);
                throw new RuntimeException("Lỗi mạng!");
            }

            String payUrl = (String) responseBody.get("payUrl");

            log.info("RENEWAL - Payment URL created successfully: orderId={}, payUrl={}",
                    orderId, payUrl);

            Map<String, String> result = new HashMap<>();
            result.put("payUrl", payUrl);
            result.put("orderId", orderId);
            result.put("amount", String.valueOf(amount));
            result.put("discount", calculation.getTotalDiscount().toString());
            result.put("renewalType", calculation.getRenewalType());
            result.put("packageName", calculation.getRenewalPackageName());

            return result;

        } catch (Exception e) {
            log.error("RENEWAL - Exception when calling MoMo API: {}", e.getMessage());
            throw new RuntimeException("Lỗi mạng!", e);
        }
    }

    /**
     * TẠO PAYMENT URL CHO TIỀN CỌC PIN (BATTERY DEPOSIT)
     *
     * Số tiền cố định: 400,000 VND
     * Sau khi thanh toán thành công, vehicle sẽ chuyển sang PENDING (chờ admin duyệt)
     * @return Map chứa paymentUrl, orderId, requestId
     */
    @Transactional
    public Map<String, String> createDepositPaymentUrl(Long vehicleId, String customRedirectUrl) {
        User currentDriver = authenticationService.getCurrentUser();

        if (currentDriver.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Chỉ tài xế mới có thể thanh toán tiền cọc!");
        }

        // Kiểm tra vehicle tồn tại và thuộc về driver hiện tại
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe với ID: " + vehicleId));

        if (!vehicle.getDriver().getId().equals(currentDriver.getId())) {
            throw new AuthenticationException("Xe này không thuộc về bạn!");
        }

        // Kiểm tra vehicle đang ở trạng thái UNPAID (chưa thanh toán cọc)
        if (vehicle.getStatus() != Vehicle.VehicleStatus.UNPAID) {
            throw new IllegalStateException("Xe này không ở trạng thái chưa cọc!");
        }

        // Cố định số tiền cọc 400k VND
        long amount = 400000;
//        test trước đã

        // Chuẩn bị thông tin thanh toán MoMo
        String orderId = MoMoUtil.generateOrderId();
        String requestId = MoMoUtil.generateRequestId();

        // LƯU THÔNG TIN DEPOSIT vào extraData
        // Format: vehicleId=<vehicleId>&driverId=<id>&type=DEPOSIT
        String extraData = String.format(
                "vehicleId=%d&driverId=%d&type=DEPOSIT",
                vehicleId,
                currentDriver.getId()
        );

        String finalRedirectUrl = (customRedirectUrl != null && !customRedirectUrl.trim().isEmpty())
                ? customRedirectUrl
                : moMoConfig.getRedirectUrl();

        log.info("DEPOSIT - Creating MoMo payment URL: Driver={}, VehicleID={}, Amount={} VND",
                currentDriver.getEmail(), vehicleId, amount);

        // Build signature parameters
        Map<String, String> signatureParams = new LinkedHashMap<>();
        signatureParams.put("accessKey", moMoConfig.getAccessKey());
        signatureParams.put("amount", String.valueOf(amount));
        signatureParams.put("extraData", extraData);
        signatureParams.put("ipnUrl", moMoConfig.getIpnUrl());
        signatureParams.put("orderId", orderId);
        signatureParams.put("orderInfo", "Tien coc pin dang ky xe");
        signatureParams.put("partnerCode", moMoConfig.getPartnerCode());
        signatureParams.put("redirectUrl", finalRedirectUrl);
        signatureParams.put("requestId", requestId);
        signatureParams.put("requestType", moMoConfig.getRequestType());

        String rawSignature = MoMoUtil.buildRawSignature(signatureParams);
        String signature = MoMoUtil.hmacSHA256(rawSignature, moMoConfig.getSecretKey());

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", moMoConfig.getPartnerCode());
        requestBody.put("partnerName", "EVBattery Swap System");
        requestBody.put("storeId", "EVBatteryStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", "Tien coc pin dang ky xe");
        requestBody.put("redirectUrl", finalRedirectUrl);
        requestBody.put("ipnUrl", moMoConfig.getIpnUrl());
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", moMoConfig.getRequestType());
        requestBody.put("signature", signature);

        // Gọi MoMo API
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    moMoConfig.getEndpoint(),
                    entity,
                    Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !"0".equals(String.valueOf(responseBody.get("resultCode")))) {
                String errorMessage = responseBody != null
                        ? (String) responseBody.get("message")
                        : "Unknown error";

                log.error("DEPOSIT - MoMo payment URL creation failed: {}", errorMessage);
                throw new RuntimeException("Không thể tạo payment URL: " + errorMessage);
            }

            String payUrl = (String) responseBody.get("payUrl");

            // LƯU DEPOSIT STATUS vào Vehicle
            vehicle.setDepositStatus("PENDING");
            vehicleRepository.save(vehicle);

            log.info("DEPOSIT - Payment URL created successfully: orderId={}, payUrl={}", orderId, payUrl);

            Map<String, String> result = new HashMap<>();
            result.put("payUrl", payUrl);
            result.put("orderId", orderId);
            result.put("requestId", requestId);
            result.put("amount", String.valueOf(amount));
            result.put("message", "Redirect driver to this URL to complete deposit payment");

            return result;

        } catch (Exception e) {
            log.error("DEPOSIT - Exception when calling MoMo API: {}", e.getMessage());
            throw new RuntimeException("Lỗi kết nối với MoMo: " + e.getMessage(), e);
        }
    }
}
