package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Payment;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
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
}