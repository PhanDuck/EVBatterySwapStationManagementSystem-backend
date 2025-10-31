package com.evbs.BackEndEvBs.controller;


import com.evbs.BackEndEvBs.model.response.DashboardResponse;
import com.evbs.BackEndEvBs.service.DashBoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "api")
@Tag(name = "Dashboard", description = "API quản lý dashboard và thống kê")
public class DashBoardController {

    private final DashBoardService dashBoardService;

    /**
     * Lấy toàn bộ dữ liệu dashboard (ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy toàn bộ dữ liệu dashboard",
            description = "Lấy tất cả thống kê và dữ liệu dashboard (ADMIN/STAFF only)")
    public ResponseEntity<DashboardResponse> getDashboardData() {
        DashboardResponse response = dashBoardService.getDashboardData();
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy tổng quan chung
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tổng quan chung",
            description = "Lấy các chỉ số tổng quan của hệ thống")
    public ResponseEntity<DashboardResponse.OverviewStats> getOverviewStats() {
        DashboardResponse.OverviewStats stats = dashBoardService.getOverviewStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy thống kê doanh thu
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy thống kê doanh thu",
            description = "Lấy thống kê doanh thu theo các khoảng thời gian")
    public ResponseEntity<DashboardResponse.RevenueStats> getRevenueStats() {
        DashboardResponse.RevenueStats stats = dashBoardService.getRevenueStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy thống kê giao dịch đổi pin
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy thống kê giao dịch",
            description = "Lấy thống kê về các giao dịch đổi pin")
    public ResponseEntity<DashboardResponse.TransactionStats> getTransactionStats() {
        DashboardResponse.TransactionStats stats = dashBoardService.getTransactionStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy thống kê người dùng
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy thống kê người dùng",
            description = "Lấy thống kê về người dùng trong hệ thống")
    public ResponseEntity<DashboardResponse.UserStats> getUserStats() {
        DashboardResponse.UserStats stats = dashBoardService.getUserStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy thống kê trạm
     */
    @GetMapping("/stations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy thống kê trạm",
            description = "Lấy thống kê về các trạm đổi pin")
    public ResponseEntity<DashboardResponse.StationStats> getStationStats() {
        DashboardResponse.StationStats stats = dashBoardService.getStationStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy thống kê pin
     */
    @GetMapping("/batteries")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy thống kê pin",
            description = "Lấy thống kê về pin trong hệ thống")
    public ResponseEntity<DashboardResponse.BatteryStats> getBatteryStats() {
        DashboardResponse.BatteryStats stats = dashBoardService.getBatteryStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy danh sách giao dịch gần đây
     */
    @GetMapping("/recent-transactions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy giao dịch gần đây",
            description = "Lấy danh sách giao dịch gần đây nhất")
    public ResponseEntity<List<DashboardResponse.RecentTransaction>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        List<DashboardResponse.RecentTransaction> transactions = dashBoardService.getRecentTransactions(limit);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Lấy danh sách trạm hoạt động tốt nhất
     */
    @GetMapping("/top-stations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy top trạm hoạt động tốt nhất",
            description = "Lấy danh sách các trạm có hiệu suất tốt nhất")
    public ResponseEntity<List<DashboardResponse.TopStation>> getTopStations(
            @RequestParam(defaultValue = "5") int limit) {
        List<DashboardResponse.TopStation> stations = dashBoardService.getTopStations(limit);
        return ResponseEntity.ok(stations);
    }
}
