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
        if (currentUser.getRole() != User.Role.ADMIN && currentUser.getRole() != User.Role.STAFF) {
            throw new AuthenticationException("Từ chối truy cập. Chỉ Admin/Staff mới được phép thực hiện thao tác này.");
        }
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Payment> getMyPayments() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Chỉ tài xế mới có thể xem lịch sử thanh toán của họ.");
        }
        return paymentRepository.findByDriverId(currentUser.getId());
    }    }