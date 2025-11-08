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
     * Lấy toàn bộ dữ liệu dashboard
     */
    public DashboardResponse getDashboardData() {
        return DashboardResponse.builder()
                .overview(getOverviewStats())
                .revenue(getRevenueStats())
                .transactions(getTransactionStats())
                .users(getUserStats())
                .stations(getStationStats())
                .batteries(getBatteryStats())
                .recentTransactions(getRecentTransactions(10))
                .topStations(getTopStations(5))
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
     * Thống kê doanh thu
     */
    private DashboardResponse.RevenueStats getRevenueStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime weekStart = todayStart.minusDays(6);
        LocalDateTime monthStart = now.withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime yearStart = now.withDayOfYear(1).with(LocalTime.MIN);

        BigDecimal totalRevenue = paymentRepository.sumTotalRevenue();
        BigDecimal todayRevenue = paymentRepository.sumRevenueByDateRange(todayStart, now);
        BigDecimal weekRevenue = paymentRepository.sumRevenueByDateRange(weekStart, now);
        BigDecimal monthRevenue = paymentRepository.sumRevenueByDateRange(monthStart, now);
        BigDecimal yearRevenue = paymentRepository.sumRevenueByDateRange(yearStart, now);

        // Giá trị giao dịch trung bình
        Long transactionCount = swapTransactionRepository.count();
        BigDecimal averageTransactionValue = BigDecimal.ZERO;
        if (transactionCount > 0 && totalRevenue != null) {
            averageTransactionValue = totalRevenue.divide(
                    BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP
            );
        }

        // Doanh thu 7 ngày gần nhất
        List<DashboardResponse.DailyRevenue> dailyRevenues = getDailyRevenues(7);

        // Doanh thu 12 tháng gần nhất
        List<DashboardResponse.MonthlyRevenue> monthlyRevenues = getMonthlyRevenues(12);

        return DashboardResponse.RevenueStats.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .weekRevenue(weekRevenue != null ? weekRevenue : BigDecimal.ZERO)
                .monthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .yearRevenue(yearRevenue != null ? yearRevenue : BigDecimal.ZERO)
                .averageTransactionValue(averageTransactionValue)
                .dailyRevenues(dailyRevenues)
                .monthlyRevenues(monthlyRevenues)
                .build();
    }

    /**
     * Thống kê giao dịch
     */
    private DashboardResponse.TransactionStats getTransactionStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime weekStart = todayStart.minusDays(6);
        LocalDateTime monthStart = now.withDayOfMonth(1).with(LocalTime.MIN);

        Long totalTransactions = swapTransactionRepository.count();
        Long completedTransactions = swapTransactionRepository.countByStatus(SwapTransaction.Status.COMPLETED);
        Long pendingTransactions = swapTransactionRepository.countByStatus(SwapTransaction.Status.PENDING_PAYMENT);
        Long cancelledTransactions = swapTransactionRepository.countByStatus(SwapTransaction.Status.CANCELLED);

        Long todayTransactions = swapTransactionRepository.countByStartTimeBetween(todayStart, now);
        Long weekTransactions = swapTransactionRepository.countByStartTimeBetween(weekStart, now);
        Long monthTransactions = swapTransactionRepository.countByStartTimeBetween(monthStart, now);

        // Thời gian đổi pin trung bình (phút)
        Double averageSwapTime = swapTransactionRepository.calculateAverageSwapTime();
        if (averageSwapTime == null) averageSwapTime = 0.0;

        // Giao dịch theo giờ (hôm nay)
        List<DashboardResponse.HourlyTransaction> hourlyTransactions = getHourlyTransactions();

        return DashboardResponse.TransactionStats.builder()
                .totalTransactions(totalTransactions)
                .completedTransactions(completedTransactions)
                .pendingTransactions(pendingTransactions)
                .cancelledTransactions(cancelledTransactions)
                .todayTransactions(todayTransactions)
                .weekTransactions(weekTransactions)
                .monthTransactions(monthTransactions)
                .averageSwapTime(averageSwapTime)
                .hourlyTransactions(hourlyTransactions)
                .build();
    }

    /**
     * Thống kê người dùng
     */
    private DashboardResponse.UserStats getUserStats() {
        Long totalUsers = userRepository.count();
        Long totalDrivers = userRepository.countByRole(User.Role.DRIVER);
        Long totalStaff = userRepository.countByRole(User.Role.STAFF);
        Long totalAdmins = userRepository.countByRole(User.Role.ADMIN);
        Long activeUsers = userRepository.countByStatus(User.Status.ACTIVE);

        // Placeholder cho các thống kê mới (cần thêm trường createdAt trong entity)
        Long newUsersToday = 0L;
        Long newUsersWeek = 0L;
        Long newUsersMonth = 0L;
        Long activeSubscriptions = 0L; // Tạm thời set 0, sẽ cần query từ DriverSubscriptionRepository

        return DashboardResponse.UserStats.builder()
                .totalUsers(totalUsers)
                .totalDrivers(totalDrivers)
                .totalStaff(totalStaff)
                .totalAdmins(totalAdmins)
                .activeUsers(activeUsers)
                .newUsersToday(newUsersToday)
                .newUsersWeek(newUsersWeek)
                .newUsersMonth(newUsersMonth)
                .activeSubscriptions(activeSubscriptions)
                .build();
    }

    /**
     * Thống kê trạm
     */
    private DashboardResponse.StationStats getStationStats() {
        Long totalStations = stationRepository.count();
        Long activeStations = stationRepository.countByStatus(Station.Status.ACTIVE);
        Long inactiveStations = totalStations - activeStations;

        // Tổng số khe pin
        Long totalBatterySlots = stationRepository.sumTotalCapacity();
        if (totalBatterySlots == null) totalBatterySlots = 0L;

        // Số khe đang được sử dụng
        Long usedSlots = batteryRepository.countByCurrentStationIsNotNull();

        // Khe còn trống
        Long availableBatterySlots = totalBatterySlots - usedSlots;

        // Tỷ lệ sử dụng trung bình
        Double averageUtilization = 0.0;
        if (totalBatterySlots > 0) {
            averageUtilization = (usedSlots * 100.0) / totalBatterySlots;
        }

        // Tỷ lệ sử dụng từng trạm
        List<DashboardResponse.StationUtilization> stationUtilizations = getStationUtilizations();

        return DashboardResponse.StationStats.builder()
                .totalStations(totalStations)
                .activeStations(activeStations)
                .inactiveStations(inactiveStations)
                .totalBatterySlots(totalBatterySlots)
                .availableBatterySlots(availableBatterySlots)
                .averageUtilization(averageUtilization)
                .stationUtilizations(stationUtilizations)
                .build();
    }

    /**
     * Thống kê pin
     */
    private DashboardResponse.BatteryStats getBatteryStats() {
        Long totalBatteries = batteryRepository.count();
        Long availableBatteries = batteryRepository.countByStatus(Battery.Status.AVAILABLE);
        Long chargingBatteries = batteryRepository.countByStatus(Battery.Status.CHARGING);
        Long maintenanceBatteries = batteryRepository.countByStatus(Battery.Status.MAINTENANCE);
        Long damagedBatteries = batteryRepository.countByStatus(Battery.Status.RETIRED);

        // Mức sạc trung bình
        Double averageChargeLevel = batteryRepository.calculateAverageChargeLevel();
        if (averageChargeLevel == null) averageChargeLevel = 0.0;

        // Tình trạng sức khỏe trung bình
        Double averageHealthLevel = batteryRepository.calculateAverageHealthLevel();
        if (averageHealthLevel == null) averageHealthLevel = 0.0;

        // Phân bố loại pin
        List<DashboardResponse.BatteryTypeDistribution> batteryTypeDistributions = getBatteryTypeDistributions();

        return DashboardResponse.BatteryStats.builder()
                .totalBatteries(totalBatteries)
                .availableBatteries(availableBatteries)
                .chargingBatteries(chargingBatteries)
                .maintenanceBatteries(maintenanceBatteries)
                .damagedBatteries(damagedBatteries)
                .averageChargeLevel(averageChargeLevel)
                .averageHealthLevel(averageHealthLevel)
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

    /**
     * Lấy danh sách trạm hoạt động tốt nhất
     */
    public List<DashboardResponse.TopStation> getTopStations(int limit) {
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);

        List<Object[]> results = swapTransactionRepository.findTopStationsByTransactionCount(monthStart);

        return results.stream()
                .limit(limit)
                .map(r -> {
                    Station station = (Station) r[0];
                    Long transactionCount = (Long) r[1];
                    BigDecimal revenue = (BigDecimal) r[2];

                    // Tính tỷ lệ sử dụng
                    int usedSlots = station.getCurrentBatteryCount();
                    double utilizationRate = 0.0;
                    if (station.getCapacity() != null && station.getCapacity() > 0) {
                        utilizationRate = (usedSlots * 100.0) / station.getCapacity();
                    }

                    return DashboardResponse.TopStation.builder()
                            .stationId(station.getId())
                            .stationName(station.getName())
                            .location(station.getLocation())
                            .transactionCount(transactionCount)
                            .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                            .utilizationRate(utilizationRate)
                            .build();
                })
                .collect(Collectors.toList());
    }

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

    /**
     * Lấy doanh thu theo ngày (n ngày gần nhất)
     */
    private List<DashboardResponse.DailyRevenue> getDailyRevenues(int days) {
        List<DashboardResponse.DailyRevenue> dailyRevenues = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            BigDecimal revenue = paymentRepository.sumRevenueByDateRange(dayStart, dayEnd);
            Long transactionCount = swapTransactionRepository.countByStartTimeBetween(dayStart, dayEnd);

            dailyRevenues.add(DashboardResponse.DailyRevenue.builder()
                    .date(date.format(formatter))
                    .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                    .transactionCount(transactionCount)
                    .build());
        }

        return dailyRevenues;
    }

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

    /**
     * Lấy giao dịch theo giờ (hôm nay)
     */
    private List<DashboardResponse.HourlyTransaction> getHourlyTransactions() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime now = LocalDateTime.now();

        List<Object[]> results = swapTransactionRepository.countTransactionsByHour(todayStart, now);

        // Tạo map từ kết quả - SQL Server trả về Integer cho COUNT và DATEPART
        Map<Integer, Long> hourMap = new HashMap<>();
        for (Object[] r : results) {
            Integer hour = ((Number) r[0]).intValue();
            Long count = ((Number) r[1]).longValue(); // Convert Integer to Long
            hourMap.put(hour, count);
        }

        // Tạo danh sách đầy đủ 24 giờ
        List<DashboardResponse.HourlyTransaction> hourlyTransactions = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyTransactions.add(DashboardResponse.HourlyTransaction.builder()
                    .hour(hour)
                    .count(hourMap.getOrDefault(hour, 0L))
                    .build());
        }

        return hourlyTransactions;
    }

    /**
     * Lấy tỷ lệ sử dụng từng trạm
     */
    private List<DashboardResponse.StationUtilization> getStationUtilizations() {
        List<Station> stations = stationRepository.findAll();

        return stations.stream()
                .map(station -> {
                    int totalSlots = station.getCapacity() != null ? station.getCapacity() : 0;
                    int usedSlots = station.getCurrentBatteryCount();
                    double utilizationRate = 0.0;

                    if (totalSlots > 0) {
                        utilizationRate = (usedSlots * 100.0) / totalSlots;
                    }

                    return DashboardResponse.StationUtilization.builder()
                            .stationId(station.getId())
                            .stationName(station.getName())
                            .totalSlots(totalSlots)
                            .usedSlots(usedSlots)
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
