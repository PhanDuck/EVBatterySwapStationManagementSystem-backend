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
}
