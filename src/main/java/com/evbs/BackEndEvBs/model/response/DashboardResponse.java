package com.evbs.BackEndEvBs.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    // Tổng quan chung
    private OverviewStats overview;

    // Thống kê doanh thu
    private RevenueStats revenue;

    // Thống kê giao dịch đổi pin
    private TransactionStats transactions;

    // Thống kê người dùng
    private UserStats users;

    // Thống kê trạm
    private StationStats stations;

    // Thống kê pin
    private BatteryStats batteries;

    // Giao dịch gần đây
    private List<RecentTransaction> recentTransactions;

    // Trạm hoạt động tốt nhất
    private List<TopStation> topStations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewStats {
        private Long totalRevenue;              // Tổng doanh thu
        private Long totalTransactions;         // Tổng số giao dịch đổi pin
        private Long totalUsers;                // Tổng số người dùng
        private Long totalActiveStations;       // Tổng số trạm đang hoạt động
        private Long todayRevenue;              // Doanh thu hôm nay
        private Long todayTransactions;         // Số giao dịch hôm nay
        private BigDecimal revenueGrowth;       // Tăng trưởng doanh thu (%)
        private BigDecimal transactionGrowth;   // Tăng trưởng giao dịch (%)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueStats {
        private BigDecimal totalRevenue;           // Tổng doanh thu
        private BigDecimal todayRevenue;           // Doanh thu hôm nay
        private BigDecimal weekRevenue;            // Doanh thu tuần này
        private BigDecimal monthRevenue;           // Doanh thu tháng này
        private BigDecimal yearRevenue;            // Doanh thu năm nay
        private BigDecimal averageTransactionValue; // Giá trị giao dịch trung bình
        private List<DailyRevenue> dailyRevenues;  // Doanh thu theo ngày (7 ngày gần nhất)
        private List<MonthlyRevenue> monthlyRevenues; // Doanh thu theo tháng (12 tháng gần nhất)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionStats {
        private Long totalTransactions;         // Tổng số giao dịch
        private Long completedTransactions;     // Giao dịch hoàn thành
        private Long pendingTransactions;       // Giao dịch đang chờ
        private Long cancelledTransactions;     // Giao dịch bị hủy
        private Long todayTransactions;         // Giao dịch hôm nay
        private Long weekTransactions;          // Giao dịch tuần này
        private Long monthTransactions;         // Giao dịch tháng này
        private Double averageSwapTime;         // Thời gian đổi pin trung bình (phút)
        private List<HourlyTransaction> hourlyTransactions; // Giao dịch theo giờ
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private Long totalUsers;                // Tổng số người dùng
        private Long totalDrivers;              // Tổng số tài xế
        private Long totalStaff;                // Tổng số nhân viên
        private Long totalAdmins;               // Tổng số admin
        private Long activeUsers;               // Người dùng đang hoạt động
        private Long newUsersToday;             // Người dùng mới hôm nay
        private Long newUsersWeek;              // Người dùng mới tuần này
        private Long newUsersMonth;             // Người dùng mới tháng này
        private Long activeSubscriptions;       // Số gói đăng ký đang hoạt động
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StationStats {
        private Long totalStations;             // Tổng số trạm
        private Long activeStations;            // Trạm đang hoạt động
        private Long inactiveStations;          // Trạm không hoạt động
        private Long totalBatterySlots;         // Tổng số khe pin
        private Long availableBatterySlots;     // Khe pin còn trống
        private Double averageUtilization;      // Tỷ lệ sử dụng trung bình (%)
        private List<StationUtilization> stationUtilizations; // Tỷ lệ sử dụng từng trạm
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatteryStats {
        private Long totalBatteries;            // Tổng số pin
        private Long availableBatteries;        // Pin sẵn sàng
        private Long chargingBatteries;         // Pin đang sạc
        private Long maintenanceBatteries;      // Pin đang bảo trì
        private Long damagedBatteries;          // Pin bị hỏng
        private Double averageChargeLevel;      // Mức sạc trung bình (%)
        private Double averageHealthLevel;      // Tình trạng sức khỏe trung bình (%)
        private List<BatteryTypeDistribution> batteryTypeDistributions; // Phân bố loại pin
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private String date;                    // Ngày (yyyy-MM-dd)
        private BigDecimal revenue;             // Doanh thu
        private Long transactionCount;          // Số giao dịch
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;                   // Tháng (yyyy-MM)
        private BigDecimal revenue;             // Doanh thu
        private Long transactionCount;          // Số giao dịch
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyTransaction {
        private Integer hour;                   // Giờ (0-23)
        private Long count;                     // Số giao dịch
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTransaction {
        private Long transactionId;
        private String driverName;
        private String stationName;
        private String vehicleLicensePlate;
        private BigDecimal amount;
        private String status;
        private LocalDateTime transactionTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopStation {
        private Long stationId;
        private String stationName;
        private String location;
        private Long transactionCount;
        private BigDecimal revenue;
        private Double utilizationRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StationUtilization {
        private Long stationId;
        private String stationName;
        private Integer totalSlots;
        private Integer usedSlots;
        private Double utilizationRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatteryTypeDistribution {
        private String batteryType;
        private Long count;
        private Long available;
        private Long charging;
        private Long maintenance;
    }
}
