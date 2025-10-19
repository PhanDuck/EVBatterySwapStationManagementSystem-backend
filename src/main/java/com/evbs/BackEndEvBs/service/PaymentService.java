package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Payment;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.model.request.PaymentRequest;
import com.evbs.BackEndEvBs.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Autowired
    private final PaymentRepository paymentRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Payment> getMyPayments() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can view their payments");
        }
        return paymentRepository.findByDriverId(currentUser.getId());
    }

    /**
     * ❌ DEPRECATED - Swap không cần thanh toán riêng
     * Thanh toán chỉ khi MUA GÓI (DriverSubscription)
     * 
     * Method này giữ lại để backward compatibility
     * Nhưng trong thực tế, swap MIỄN PHÍ nếu có subscription active
     */
    @Deprecated
    @Transactional
    public Payment createPayment(PaymentRequest request) {
        throw new AuthenticationException(
            "❌ Swap không cần thanh toán riêng! " +
            "Thanh toán chỉ khi MUA GÓI dịch vụ. " +
            "Vui lòng sử dụng POST /api/driver-subscription để mua gói."
        );
    }
}