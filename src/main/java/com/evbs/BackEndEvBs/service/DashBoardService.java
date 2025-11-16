package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.model.response.DashboardResponse;
import com.evbs.BackEndEvBs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashBoardService {

    private final SwapTransactionRepository swapTransactionRepository;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final BatteryRepository batteryRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Lấy toàn bộ dữ liệu dashboard - RÚT GỌN CHỈ CÁI CẦN THIẾT
     */
    public DashboardResponse getDashboardData() {
        return DashboardResponse.builder()
                .overview(getOverviewStats())
                .revenue(getRevenueStatsSimplified())
                .users(getUserStatsSimplified())
                .stations(getStationStatsSimplified())
                .batteries(getBatteryStatsSimplified())
                .recentTransactions(getRecentTransactions(10))
                .build();
    }

    /**
     * Tổng quan chung
     */
    private DashboardResponse.OverviewStats getOverviewStats() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);

        // Tổng doanh thu
        BigDecimal totalRevenue = paymentRepository.sumTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // Doanh thu hôm nay
        BigDecimal todayRevenue = paymentRepository.sumRevenueByDateRange(todayStart, LocalDateTime.now());
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        // Doanh thu hôm qua
        BigDecimal yesterdayRevenue = paymentRepository.sumRevenueByDateRange(yesterdayStart, todayStart);
        if (yesterdayRevenue == null) yesterdayRevenue = BigDecimal.ZERO;

        // Tổng số giao dịch
        Long totalTransactions = swapTransactionRepository.count();

        // Giao dịch hôm nay
        Long todayTransactions = swapTransactionRepository.countByStartTimeBetween(todayStart, LocalDateTime.now());

        // Giao dịch hôm qua
        Long yesterdayTransactions = swapTransactionRepository.countByStartTimeBetween(yesterdayStart, todayStart);

        // Tổng số người dùng
        Long totalUsers = userRepository.count();

        // Trạm đang hoạt động
        Long activeStations = stationRepository.countByStatus(Station.Status.ACTIVE);

        // Tính tăng trưởng doanh thu
        BigDecimal revenueGrowth = calculateGrowthRate(todayRevenue, yesterdayRevenue);

        // Tính tăng trưởng giao dịch
        BigDecimal transactionGrowth = calculateGrowthRate(
                BigDecimal.valueOf(todayTransactions),
                BigDecimal.valueOf(yesterdayTransactions)
        );

        return DashboardResponse.OverviewStats.builder()
                .totalRevenue(totalRevenue.longValue())
                .totalTransactions(totalTransactions)
                .totalUsers(totalUsers)
                .totalActiveStations(activeStations)
                .todayRevenue(todayRevenue.longValue())
                .todayTransactions(todayTransactions)
                .revenueGrowth(revenueGrowth)
                .transactionGrowth(transactionGrowth)
                .build();
    }

    /**
     * Thống kê doanh thu - RÚT GỌN (CHỈ DÙNG monthlyRevenues)
     */
    private DashboardResponse.RevenueStats getRevenueStatsSimplified() {
        // Chỉ lấy doanh thu theo tháng (12 tháng gần nhất) để vẽ biểu đồ
        List<DashboardResponse.MonthlyRevenue> monthlyRevenues = getMonthlyRevenues(12);

        return DashboardResponse.RevenueStats.builder()
                .monthlyRevenues(monthlyRevenues)
                .build();
    }

    // XÓA getTransactionStats() vì không cần thiết cho frontend

    /**
     * Thống kê người dùng - RÚT GỌN (CHỈ totalUsers)
     */
    private DashboardResponse.UserStats getUserStatsSimplified() {
        Long totalUsers = userRepository.count();

        return DashboardResponse.UserStats.builder()
                .totalUsers(totalUsers)
                .build();
    }

    /**
     * Thống kê trạm - RÚT GỌN (CHỈ totalStations và stationUtilizations)
     */
    private DashboardResponse.StationStats getStationStatsSimplified() {
        Long totalStations = stationRepository.count();

        // Tỷ lệ sử dụng từng trạm (SỬA công thức: số booking của trạm / tổng booking * 100)
        List<DashboardResponse.StationUtilization> stationUtilizations = getStationUtilizationsNew();

        return DashboardResponse.StationStats.builder()
                .totalStations(totalStations)
                .stationUtilizations(stationUtilizations)
                .build();
    }

    /**
     * Thống kê pin - RÚT GỌN (CHỈ totalBatteries và batteryTypeDistributions)
     */
    private DashboardResponse.BatteryStats getBatteryStatsSimplified() {
        Long totalBatteries = batteryRepository.count();

        // Phân bố loại pin (để vẽ Pie Chart)
        List<DashboardResponse.BatteryTypeDistribution> batteryTypeDistributions = getBatteryTypeDistributions();

        return DashboardResponse.BatteryStats.builder()
                .totalBatteries(totalBatteries)
                .batteryTypeDistributions(batteryTypeDistributions)
                .build();
    }

    /**
     * Lấy danh sách giao dịch gần đây
     */
    public List<DashboardResponse.RecentTransaction> getRecentTransactions(int limit) {
        // Sử dụng Spring Data JPA method name query
        List<SwapTransaction> transactions = swapTransactionRepository.findTop10ByOrderByStartTimeDesc();

        return transactions.stream()
                .limit(limit)
                .map(t -> DashboardResponse.RecentTransaction.builder()
                        .transactionId(t.getId())
                        .driverName(t.getDriver() != null ? t.getDriver().getFullName() : "N/A")
                        .stationName(t.getStation() != null ? t.getStation().getName() : "N/A")
                        .vehicleLicensePlate(t.getVehicle() != null ? t.getVehicle().getPlateNumber() : "N/A")
                        .amount(t.getCost() != null ? t.getCost() : BigDecimal.ZERO)
                        .status(t.getStatus().toString())
                        .transactionTime(t.getStartTime())
                        .build())
                .collect(Collectors.toList());
    }

    // XÓA getTopStations() vì không cần thiết cho frontend

    // ============ PRIVATE HELPER METHODS ============

    /**
     * Tính tỷ lệ tăng trưởng (%)
     */
    private BigDecimal calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal difference = current.subtract(previous);
        return difference.divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // XÓA getDailyRevenues() vì không cần thiết cho frontend

    /**
     * Lấy doanh thu theo tháng (n tháng gần nhất)
     */
    private List<DashboardResponse.MonthlyRevenue> getMonthlyRevenues(int months) {
        List<DashboardResponse.MonthlyRevenue> monthlyRevenues = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = LocalDate.now().minusMonths(i);
            LocalDateTime monthStart = LocalDateTime.of(monthDate.withDayOfMonth(1), LocalTime.MIN);
            LocalDateTime monthEnd = LocalDateTime.of(
                    monthDate.withDayOfMonth(monthDate.lengthOfMonth()),
                    LocalTime.MAX
            );

            BigDecimal revenue = paymentRepository.sumRevenueByDateRange(monthStart, monthEnd);
            Long transactionCount = swapTransactionRepository.countByStartTimeBetween(monthStart, monthEnd);

            monthlyRevenues.add(DashboardResponse.MonthlyRevenue.builder()
                    .month(monthDate.format(formatter))
                    .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                    .transactionCount(transactionCount)
                    .build());
        }

        return monthlyRevenues;
    }

    // XÓA getHourlyTransactions() vì không cần thiết cho frontend

    /**
     * Lấy tỷ lệ sử dụng từng trạm - CÔNG THỨC MỚI
     * Tỷ lệ = (Số booking của trạm / Tổng số booking) * 100
     */
    private List<DashboardResponse.StationUtilization> getStationUtilizationsNew() {
        List<Station> stations = stationRepository.findAll();
        
        // Đếm tổng số booking của tất cả trạm
        Long totalBookings = swapTransactionRepository.count();
        
        return stations.stream()
                .map(station -> {
                    // Đếm số booking của trạm này
                    Long stationBookings = swapTransactionRepository.countByStationId(station.getId());
                    
                    // Tính tỷ lệ: (Booking trạm / Tổng booking) * 100
                    double utilizationRate = 0.0;
                    if (totalBookings != null && totalBookings > 0) {
                        utilizationRate = (stationBookings * 100.0) / totalBookings;
                        // Làm tròn 2 chữ số thập phân cho đẹp
                        utilizationRate = Math.round(utilizationRate * 100.0) / 100.0;
                    }

                    return DashboardResponse.StationUtilization.builder()
                            .stationId(station.getId())
                            .stationName(station.getName())
                            .totalSlots(stationBookings.intValue())  // Số booking của trạm
                            .usedSlots(totalBookings.intValue())     // Tổng booking
                            .utilizationRate(utilizationRate)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy phân bố loại pin
     */
    private List<DashboardResponse.BatteryTypeDistribution> getBatteryTypeDistributions() {
        List<Object[]> results = batteryRepository.countBatteriesByType();

        return results.stream()
                .map(r -> {
                    String batteryType = (String) r[0];
                    // SQL Server COUNT trả về Integer, cần convert sang Long
                    Long total = ((Number) r[1]).longValue();
                    Long available = ((Number) r[2]).longValue();
                    Long charging = ((Number) r[3]).longValue();
                    Long maintenance = ((Number) r[4]).longValue();

                    return DashboardResponse.BatteryTypeDistribution.builder()
                            .batteryType(batteryType)
                            .count(total)
                            .available(available)
                            .charging(charging)
                            .maintenance(maintenance)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
