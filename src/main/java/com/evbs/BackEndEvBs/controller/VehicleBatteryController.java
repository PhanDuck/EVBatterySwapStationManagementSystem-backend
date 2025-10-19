package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.SwapTransaction;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.SwapTransactionRepository;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import com.evbs.BackEndEvBs.service.AuthenticationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller để quản lý thông tin pin của xe
 * - Driver xem pin hiện tại đang dùng
 * - Driver xem lịch sử đổi pin
 * - Admin xem tất cả xe đang dùng pin nào
 */
@RestController
@RequestMapping("/api/vehicle-battery")
@RequiredArgsConstructor
@SecurityRequirement(name = "api")
public class VehicleBatteryController {

    private final VehicleRepository vehicleRepository;
    private final SwapTransactionRepository swapTransactionRepository;
    private final BatteryRepository batteryRepository;
    private final AuthenticationService authenticationService;

    /**
     * 🚗 Driver xem pin hiện tại đang sử dụng
     * GET /api/vehicle-battery/my-current-battery
     */
    @GetMapping("/my-current-battery")
    @PreAuthorize("hasAuthority('DRIVER')")
    public ResponseEntity<Map<String, Object>> getMyCurrentBattery() {
        User currentUser = authenticationService.getCurrentUser();

        // Lấy tất cả vehicles của driver
        List<Vehicle> vehicles = vehicleRepository.findByDriver(currentUser);

        if (vehicles.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "message", "Bạn chưa có xe nào trong hệ thống",
                "vehicles", Collections.emptyList()
            ));
        }

        // Lấy pin hiện tại của mỗi xe
        List<Map<String, Object>> vehiclesWithBattery = vehicles.stream()
                .map(vehicle -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("vehicleId", vehicle.getId());
                    info.put("model", vehicle.getModel());
                    info.put("plateNumber", vehicle.getPlateNumber());

                    // Tìm swap transaction gần nhất
                    Optional<SwapTransaction> lastSwap = swapTransactionRepository
                            .findTopByVehicleAndStatusOrderByStartTimeDesc(
                                    vehicle, 
                                    SwapTransaction.Status.COMPLETED
                            );

                    if (lastSwap.isPresent() && lastSwap.get().getSwapOutBattery() != null) {
                        Battery currentBattery = lastSwap.get().getSwapOutBattery();
                        info.put("currentBattery", Map.of(
                            "batteryId", currentBattery.getId(),
                            "model", currentBattery.getModel(),
                            "chargeLevel", currentBattery.getChargeLevel(),
                            "stateOfHealth", currentBattery.getStateOfHealth(),
                            "status", currentBattery.getStatus(),
                            "lastSwapTime", lastSwap.get().getStartTime()
                        ));
                        info.put("hasBattery", true);
                    } else {
                        info.put("currentBattery", null);
                        info.put("hasBattery", false);
                        info.put("message", "Xe chưa có pin (chưa swap lần nào)");
                    }

                    return info;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "vehicles", vehiclesWithBattery,
            "totalVehicles", vehicles.size()
        ));
    }

    /**
     * 🚗 Driver xem pin của xe cụ thể
     * GET /api/vehicle-battery/{vehicleId}/current
     */
    @GetMapping("/{vehicleId}/current")
    @PreAuthorize("hasAuthority('DRIVER')")
    public ResponseEntity<Map<String, Object>> getCurrentBattery(@PathVariable Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));

        // Check ownership
        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Xe này không thuộc về bạn"
            ));
        }

        // Tìm swap transaction gần nhất
        Optional<SwapTransaction> lastSwap = swapTransactionRepository
                .findTopByVehicleAndStatusOrderByStartTimeDesc(
                        vehicle, 
                        SwapTransaction.Status.COMPLETED
                );

        if (lastSwap.isEmpty() || lastSwap.get().getSwapOutBattery() == null) {
            return ResponseEntity.ok(Map.of(
                "vehicleId", vehicleId,
                "message", "Xe chưa có pin (chưa swap lần nào)",
                "hasBattery", false
            ));
        }

        Battery currentBattery = lastSwap.get().getSwapOutBattery();
        SwapTransaction transaction = lastSwap.get();

        return ResponseEntity.ok(Map.of(
            "vehicleId", vehicleId,
            "vehicleModel", vehicle.getModel(),
            "plateNumber", vehicle.getPlateNumber(),
            "hasBattery", true,
            "currentBattery", Map.of(
                "batteryId", currentBattery.getId(),
                "model", currentBattery.getModel(),
                "chargeLevel", currentBattery.getChargeLevel(),
                "capacity", currentBattery.getCapacity(),
                "stateOfHealth", currentBattery.getStateOfHealth(),
                "status", currentBattery.getStatus(),
                "manufactureDate", currentBattery.getManufactureDate(),
                "usageCount", currentBattery.getUsageCount()
            ),
            "lastSwap", Map.of(
                "transactionId", transaction.getId(),
                "swapTime", transaction.getStartTime(),
                "stationName", transaction.getStation().getName(),
                "oldBatteryId", transaction.getSwapInBattery() != null ? transaction.getSwapInBattery().getId() : null
            )
        ));
    }

    /**
     * 📜 Driver xem lịch sử đổi pin của xe
     * GET /api/vehicle-battery/{vehicleId}/history
     */
    @GetMapping("/{vehicleId}/history")
    @PreAuthorize("hasAuthority('DRIVER')")
    public ResponseEntity<Map<String, Object>> getBatterySwapHistory(@PathVariable Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));

        // Check ownership
        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Xe này không thuộc về bạn"
            ));
        }

        // Lấy tất cả swap transactions của xe
        List<SwapTransaction> swapHistory = swapTransactionRepository
                .findByVehicleOrderByStartTimeDesc(vehicle);

        List<Map<String, Object>> history = swapHistory.stream()
                .map(swap -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("transactionId", swap.getId());
                    record.put("swapTime", swap.getStartTime());
                    record.put("endTime", swap.getEndTime());
                    record.put("status", swap.getStatus());
                    record.put("station", Map.of(
                        "id", swap.getStation().getId(),
                        "name", swap.getStation().getName()
                    ));

                    if (swap.getSwapOutBattery() != null) {
                        record.put("batteryReceived", Map.of(
                            "batteryId", swap.getSwapOutBattery().getId(),
                            "model", swap.getSwapOutBattery().getModel(),
                            "chargeLevel", swap.getSwapOutBattery().getChargeLevel()
                        ));
                    }

                    if (swap.getSwapInBattery() != null) {
                        record.put("batteryReturned", Map.of(
                            "batteryId", swap.getSwapInBattery().getId(),
                            "model", swap.getSwapInBattery().getModel(),
                            "chargeLevel", swap.getSwapInBattery().getChargeLevel()
                        ));
                    }

                    // totalCost có thể null nếu chưa tính
                    return record;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "vehicleId", vehicleId,
            "totalSwaps", history.size(),
            "swapHistory", history
        ));
    }

    /**
     * 🔧 Admin xem tất cả xe đang dùng pin nào
     * GET /api/vehicle-battery/admin/all-vehicles-with-battery
     */
    @GetMapping("/admin/all-vehicles-with-battery")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getAllVehiclesWithBattery() {
        List<Vehicle> allVehicles = vehicleRepository.findAll();

        List<Map<String, Object>> vehiclesInfo = allVehicles.stream()
                .map(vehicle -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("vehicleId", vehicle.getId());
                    info.put("model", vehicle.getModel());
                    info.put("plateNumber", vehicle.getPlateNumber());
                    info.put("driver", Map.of(
                        "driverId", vehicle.getDriver().getId(),
                        "fullName", vehicle.getDriver().getFullName(),
                        "email", vehicle.getDriver().getEmail()
                    ));

                    // Tìm pin hiện tại
                    Optional<SwapTransaction> lastSwap = swapTransactionRepository
                            .findTopByVehicleAndStatusOrderByStartTimeDesc(
                                    vehicle, 
                                    SwapTransaction.Status.COMPLETED
                            );

                    if (lastSwap.isPresent() && lastSwap.get().getSwapOutBattery() != null) {
                        Battery currentBattery = lastSwap.get().getSwapOutBattery();
                        info.put("currentBattery", Map.of(
                            "batteryId", currentBattery.getId(),
                            "model", currentBattery.getModel(),
                            "chargeLevel", currentBattery.getChargeLevel(),
                            "stateOfHealth", currentBattery.getStateOfHealth(),
                            "status", currentBattery.getStatus(),
                            "usageCount", currentBattery.getUsageCount()
                        ));
                        info.put("lastSwapTime", lastSwap.get().getStartTime());
                        info.put("hasBattery", true);
                    } else {
                        info.put("currentBattery", null);
                        info.put("hasBattery", false);
                    }

                    return info;
                })
                .collect(Collectors.toList());

        // Thống kê
        long vehiclesWithBattery = vehiclesInfo.stream()
                .filter(v -> (Boolean) v.get("hasBattery"))
                .count();

        return ResponseEntity.ok(Map.of(
            "vehicles", vehiclesInfo,
            "totalVehicles", allVehicles.size(),
            "vehiclesWithBattery", vehiclesWithBattery,
            "vehiclesWithoutBattery", allVehicles.size() - vehiclesWithBattery
        ));
    }

    /**
     * 🔋 Admin xem pin nào đang được xe nào sử dụng
     * GET /api/vehicle-battery/admin/battery/{batteryId}/vehicle
     */
    @GetMapping("/admin/battery/{batteryId}/vehicle")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getVehicleUsingBattery(@PathVariable Long batteryId) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        // Tìm swap transaction gần nhất có swapOutBattery = battery này
        List<SwapTransaction> transactions = swapTransactionRepository.findAll();
        Optional<SwapTransaction> lastSwapWithThisBattery = transactions.stream()
                .filter(t -> t.getSwapOutBattery() != null && 
                            t.getSwapOutBattery().getId().equals(batteryId) &&
                            t.getStatus() == SwapTransaction.Status.COMPLETED)
                .max(Comparator.comparing(SwapTransaction::getStartTime));

        if (lastSwapWithThisBattery.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "batteryId", batteryId,
                "batteryModel", battery.getModel(),
                "status", battery.getStatus(),
                "currentStation", battery.getCurrentStation() != null ? battery.getCurrentStation().getName() : "N/A",
                "isBeingUsed", false,
                "message", "Pin này chưa được sử dụng hoặc đang ở trạm"
            ));
        }

        SwapTransaction swap = lastSwapWithThisBattery.get();
        Vehicle vehicle = swap.getVehicle();

        return ResponseEntity.ok(Map.of(
            "batteryId", batteryId,
            "batteryModel", battery.getModel(),
            "chargeLevel", battery.getChargeLevel(),
            "status", battery.getStatus(),
            "isBeingUsed", true,
            "vehicle", Map.of(
                "vehicleId", vehicle.getId(),
                "model", vehicle.getModel(),
                "plateNumber", vehicle.getPlateNumber(),
                "driver", Map.of(
                    "driverId", vehicle.getDriver().getId(),
                    "fullName", vehicle.getDriver().getFullName(),
                    "email", vehicle.getDriver().getEmail()
                )
            ),
            "lastSwap", Map.of(
                "transactionId", swap.getId(),
                "swapTime", swap.getStartTime(),
                "stationName", swap.getStation().getName()
            )
        ));
    }
}
