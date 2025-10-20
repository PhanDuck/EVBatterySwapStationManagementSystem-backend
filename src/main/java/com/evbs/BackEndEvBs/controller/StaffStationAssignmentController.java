package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.StaffStationAssignment;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.model.request.StaffStationAssignmentRequest;
import com.evbs.BackEndEvBs.service.StaffStationAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff-station-assignment")
@SecurityRequirement(name = "api")
@Tag(name = "Staff-Station Assignment", description = "APIs for admin to manage staff assignments to stations")
public class StaffStationAssignmentController {

    @Autowired
    private StaffStationAssignmentService assignmentService;

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * POST /api/staff-station-assignment : Assign staff to station (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign staff to station",
            description = "Admin assigns a staff member to manage a station. Maximum 5 stations per staff.")
    public ResponseEntity<StaffStationAssignment> assignStaffToStation(@Valid @RequestBody StaffStationAssignmentRequest request) {
        StaffStationAssignment assignment = assignmentService.assignStaffToStation(request);
        return new ResponseEntity<>(assignment, HttpStatus.CREATED);
    }

    /**
     * DELETE /api/staff-station-assignment/staff/{staffId}/station/{stationId} : Unassign staff from station (Admin only)
     */
    @DeleteMapping("/staff/{staffId}/station/{stationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unassign staff from station",
            description = "Admin removes a staff member's access to manage a station")
    public ResponseEntity<Void> unassignStaffFromStation(@PathVariable Long staffId, @PathVariable Long stationId) {
        assignmentService.unassignStaffFromStation(staffId, stationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/staff-station-assignment : Get all assignments (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all staff-station assignments",
            description = "Admin views all staff-station assignments in the system")
    public ResponseEntity<List<StaffStationAssignment>> getAllAssignments() {
        List<StaffStationAssignment> assignments = assignmentService.getAllAssignments();
        return ResponseEntity.ok(assignments);
    }

    /**
     * GET /api/staff-station-assignment/staff/{staffId}/stations : Get stations by staff (Admin only)
     */
    @GetMapping("/staff/{staffId}/stations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get stations assigned to a staff",
            description = "Admin views all stations managed by a specific staff member")
    public ResponseEntity<List<Station>> getStationsByStaff(@PathVariable Long staffId) {
        List<Station> stations = assignmentService.getStationsByStaff(staffId);
        return ResponseEntity.ok(stations);
    }

    /**
     * GET /api/staff-station-assignment/station/{stationId}/staff : Get staff by station (Admin only)
     */
    @GetMapping("/station/{stationId}/staff")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get staff assigned to a station",
            description = "Admin views all staff members managing a specific station")
    public ResponseEntity<List<User>> getStaffByStation(@PathVariable Long stationId) {
        List<User> staff = assignmentService.getStaffByStation(stationId);
        return ResponseEntity.ok(staff);
    }

    /**
     * GET /api/staff-station-assignment/staff/{staffId}/assignments : Get assignments by staff (Admin only)
     */
    @GetMapping("/staff/{staffId}/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all assignments of a staff",
            description = "Admin views detailed assignment records for a specific staff member")
    public ResponseEntity<List<StaffStationAssignment>> getAssignmentsByStaff(@PathVariable Long staffId) {
        List<StaffStationAssignment> assignments = assignmentService.getAssignmentsByStaff(staffId);
        return ResponseEntity.ok(assignments);
    }

    // ==================== STAFF ENDPOINTS ====================

    /**
     * GET /api/staff-station-assignment/my-stations : Get my assigned stations (Staff only)
     */
    @GetMapping("/my-stations")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Get my assigned stations",
            description = "Staff views all stations they are assigned to manage")
    public ResponseEntity<List<Station>> getMyAssignedStations() {
        List<Station> stations = assignmentService.getMyAssignedStations();
        return ResponseEntity.ok(stations);
    }
}
