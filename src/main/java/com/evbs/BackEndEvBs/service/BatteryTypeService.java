package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.BatteryTypeRequest;
import com.evbs.BackEndEvBs.model.request.BatteryTypeUpdateRequest;
import com.evbs.BackEndEvBs.repository.BatteryTypeRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public BatteryType createBatteryType(BatteryTypeRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Quyền truy cập bị từ chối. Yêu cầu vai trò quản trị viên.");
        }

        if (batteryTypeRepository.existsByName(request.getName())) {
            throw new AuthenticationException("Tên loại pin đã tồn tại");
        }

        //  Tạo battery type thủ công thay vì dùng ModelMapper (tránh conflict)
        BatteryType batteryType = new BatteryType();
        batteryType.setName(request.getName());
        batteryType.setDescription(request.getDescription());
        batteryType.setVoltage(request.getVoltage());
        batteryType.setCapacity(request.getCapacity());
        batteryType.setWeight(request.getWeight());
        batteryType.setDimensions(request.getDimensions());
        
        return batteryTypeRepository.save(batteryType);
    }

    @Transactional(readOnly = true)
    public List<BatteryType> getAllBatteryTypes() {
        return batteryTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public BatteryType getBatteryTypeById(Long id) {
        return batteryTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));
    }

    @Transactional
    public BatteryType updateBatteryType(Long id, BatteryTypeUpdateRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Quyền truy cập bị từ chối. Yêu cầu vai trò quản trị viên.");
        }

        BatteryType batteryType = batteryTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));

        // Kiểm tra trùng tên nếu thay đổi
        if (request.getName() != null && !batteryType.getName().equals(request.getName()) &&
                batteryTypeRepository.existsByName(request.getName())) {
            throw new AuthenticationException("Tên loại pin đã tồn tại");
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

}