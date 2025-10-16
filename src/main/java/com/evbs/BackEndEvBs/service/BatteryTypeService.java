package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.BatteryTypeRequest;
import com.evbs.BackEndEvBs.repository.BatteryTypeRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BatteryTypeService {

    @Autowired
    private final BatteryTypeRepository batteryTypeRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    @Transactional
    public BatteryType createBatteryType(BatteryTypeRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        if (batteryTypeRepository.existsByName(request.getName())) {
            throw new AuthenticationException("Battery type name already exists");
        }

        BatteryType batteryType = modelMapper.map(request, BatteryType.class);
        return batteryTypeRepository.save(batteryType);
    }

    @Transactional(readOnly = true)
    public List<BatteryType> getAllBatteryTypes() {
        return batteryTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public BatteryType getBatteryTypeById(Long id) {
        return batteryTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery type not found"));
    }

    @Transactional
    public BatteryType updateBatteryType(Long id, BatteryTypeRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        BatteryType batteryType = batteryTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery type not found"));

        // Kiểm tra trùng tên nếu thay đổi
        if (request.getName() != null && !batteryType.getName().equals(request.getName()) &&
                batteryTypeRepository.existsByName(request.getName())) {
            throw new AuthenticationException("Battery type name already exists");
        }

        // Cập nhật các field
        if (request.getName() != null) {
            batteryType.setName(request.getName());
        }
        if (request.getDescription() != null) {
            batteryType.setDescription(request.getDescription());
        }
        if (request.getVoltage() != null) {
            batteryType.setVoltage(request.getVoltage());
        }
        if (request.getCapacity() != null) {
            batteryType.setCapacity(request.getCapacity());
        }
        if (request.getWeight() != null) {
            batteryType.setWeight(request.getWeight());
        }
        if (request.getDimensions() != null) {
            batteryType.setDimensions(request.getDimensions());
        }

        return batteryTypeRepository.save(batteryType);
    }

    @Transactional
    public void deleteBatteryType(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        BatteryType batteryType = batteryTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery type not found"));

        // Kiểm tra xem battery type có đang được sử dụng không
        if (!batteryType.getBatteries().isEmpty() ||
                !batteryType.getVehicles().isEmpty() ||
                !batteryType.getStations().isEmpty()) {
            throw new AuthenticationException("Cannot delete battery type that is in use");
        }

        batteryTypeRepository.delete(batteryType);
    }
}