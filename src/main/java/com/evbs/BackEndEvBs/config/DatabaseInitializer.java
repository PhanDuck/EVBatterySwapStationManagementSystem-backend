package com.evbs.BackEndEvBs.config;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StationRepository stationRepository;
    private final BatteryRepository batteryRepository;
    private final BatteryTypeRepository batteryTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final DriverSubscriptionRepository driverSubscriptionRepository;
    private final StationInventoryRepository stationInventoryRepository;
    private final StaffStationAssignmentRepository staffStationAssignmentRepository;  // ⭐ THÊM
    private final BookingRepository bookingRepository;
    private final SwapTransactionRepository swapTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final BatteryHistoryRepository batteryHistoryRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketResponseRepository ticketResponseRepository;

    public DatabaseInitializer(UserRepository userRepository, StationRepository stationRepository, 
                              BatteryRepository batteryRepository, BatteryTypeRepository batteryTypeRepository,
                              VehicleRepository vehicleRepository,
                              ServicePackageRepository servicePackageRepository, 
                              DriverSubscriptionRepository driverSubscriptionRepository,
                              StationInventoryRepository stationInventoryRepository, 
                              StaffStationAssignmentRepository staffStationAssignmentRepository,  // ⭐ THÊM
                              BookingRepository bookingRepository,
                              SwapTransactionRepository swapTransactionRepository, PaymentRepository paymentRepository,
                              BatteryHistoryRepository batteryHistoryRepository, SupportTicketRepository supportTicketRepository,
                              TicketResponseRepository ticketResponseRepository) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.batteryRepository = batteryRepository;
        this.batteryTypeRepository = batteryTypeRepository;
        this.vehicleRepository = vehicleRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.driverSubscriptionRepository = driverSubscriptionRepository;
        this.stationInventoryRepository = stationInventoryRepository;
        this.staffStationAssignmentRepository = staffStationAssignmentRepository;  // ⭐ THÊM
        this.bookingRepository = bookingRepository;
        this.swapTransactionRepository = swapTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.batteryHistoryRepository = batteryHistoryRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.ticketResponseRepository = ticketResponseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Initializing database with sample data...");
            createSampleData();
            log.info("Database initialized successfully!");
        } else {
            log.info("Database already contains data, skipping initialization.");
        }
    }

    private void createSampleData() {
        // 1. Tạo Users
        List<User> users = createUsers();
        userRepository.saveAll(users);
        log.info("Created {} users", users.size());

        // 2. Tạo BatteryTypes TRƯỚC (vì Station và Battery cần)
        List<BatteryType> batteryTypes = createBatteryTypes();
        batteryTypeRepository.saveAll(batteryTypes);
        log.info("✓ Đã tạo {} battery types", batteryTypes.size());

        // 3. Tạo Stations (cần BatteryTypes)
        List<Station> stations = createStations(batteryTypes);
        stationRepository.saveAll(stations);
        log.info("✓ Đã tạo {} stations", stations.size());

        // 4. Tạo Service Packages
        List<ServicePackage> packages = createServicePackages();
        servicePackageRepository.saveAll(packages);
        log.info("✓ Đã tạo {} service packages", packages.size());

        // 5. Tạo Batteries (cần BatteryTypes)
        List<Battery> batteries = createBatteries(stations, batteryTypes);
        batteryRepository.saveAll(batteries);
        log.info("✓ Đã tạo {} batteries", batteries.size());

        // 5. Tạo Vehicles (cần batteries để lấy batteryType)
        List<Vehicle> vehicles = createVehicles(users, batteries);
        vehicleRepository.saveAll(vehicles);
        log.info("✓ Đã tạo {} vehicles", vehicles.size());

        // 6. Tạo Driver Subscriptions
        List<DriverSubscription> subscriptions = createDriverSubscriptions(users, packages);
        driverSubscriptionRepository.saveAll(subscriptions);
        log.info("✓ Đã tạo {} subscriptions", subscriptions.size());

        // 7. Tạo Staff Station Assignments (Staff được assign vào stations)
        List<StaffStationAssignment> assignments = createStaffStationAssignments(users, stations);
        staffStationAssignmentRepository.saveAll(assignments);
        log.info("✓ Đã tạo {} staff station assignments", assignments.size());

        // 8. Tạo Station Inventory
        List<StationInventory> inventory = createStationInventory(stations, batteries);
        stationInventoryRepository.saveAll(inventory);
        log.info("✓ Đã tạo {} station inventory records", inventory.size());

        // 9. Tạo Bookings
        List<Booking> bookings = createBookings(users, vehicles, stations);
        bookingRepository.saveAll(bookings);
        log.info("✓ Đã tạo {} bookings", bookings.size());

        // 10. Tạo Swap Transactions
        List<SwapTransaction> transactions = createSwapTransactions(users, vehicles, stations, batteries);
        swapTransactionRepository.saveAll(transactions);
        log.info("✓ Đã tạo {} swap transactions", transactions.size());

        // 11. Tạo Payments
        List<Payment> payments = createPayments(transactions, subscriptions);
        paymentRepository.saveAll(payments);
        log.info("✓ Đã tạo {} payments", payments.size());

        // 12. Tạo Battery History
        List<BatteryHistory> history = createBatteryHistory(batteries, users, stations, vehicles);
        batteryHistoryRepository.saveAll(history);
        log.info("✓ Đã tạo {} battery history records", history.size());

        // 13. Tạo Support Tickets
        List<SupportTicket> tickets = createSupportTickets(users, stations);
        supportTicketRepository.saveAll(tickets);
        log.info("✓ Đã tạo {} support tickets", tickets.size());

        // 14. Tạo Ticket Responses
        List<TicketResponse> responses = createTicketResponses(tickets, users);
        ticketResponseRepository.saveAll(responses);
        log.info("✓ Đã tạo {} ticket responses", responses.size());
    }

    private List<User> createUsers() {
        // Sử dụng password encoder thực tế để encode "password123"
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode("password123");
        
        return List.of(
            // Admin users
            createUser("System Admin", "admin@evbs.com", "0901000001", encodedPassword, User.Role.ADMIN, User.Status.ACTIVE),
            createUser("Super Admin", "superadmin@evbs.com", "0901000002", encodedPassword, User.Role.ADMIN, User.Status.ACTIVE),
            
            // Staff users
            createUser("Nguyễn Văn Staff", "staff1@evbs.com", "0902000001", encodedPassword, User.Role.STAFF, User.Status.ACTIVE),
            createUser("Trần Thị Staff", "staff2@evbs.com", "0902000002", encodedPassword, User.Role.STAFF, User.Status.ACTIVE),
            createUser("Lê Văn Staff", "staff3@evbs.com", "0902000003", encodedPassword, User.Role.STAFF, User.Status.ACTIVE),
            
            // Driver users
            createUser("Nguyễn Văn An", "driver1@gmail.com", "0903000001", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
            createUser("Trần Thị Bình", "driver2@gmail.com", "0903000002", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
            createUser("Lê Văn Cường", "driver3@gmail.com", "0903000003", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
            createUser("Phạm Thị Dung", "driver4@gmail.com", "0903000004", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
            createUser("Hoàng Văn Em", "driver5@gmail.com", "0903000005", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
            createUser("Vũ Thị Phượng", "driver6@gmail.com", "0903000006", encodedPassword, User.Role.DRIVER, User.Status.INACTIVE),
            createUser("Đào Văn Giang", "driver7@gmail.com", "0903000007", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE)
        );
    }

    private User createUser(String fullName, String email, String phone, String password, User.Role role, User.Status status) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPasswordHash(password);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private List<BatteryType> createBatteryTypes() {
        BatteryType type48V = new BatteryType();
        type48V.setName("Standard 48V-20Ah");
        type48V.setVoltage(48.0);
        type48V.setCapacity(20.0);
        type48V.setWeight(15.5);
        type48V.setDimensions("35x25x20 cm");
        type48V.setDescription("Pin tiêu chuẩn cho xe máy điện");
        
        BatteryType type60V = new BatteryType();
        type60V.setName("Premium 60V-30Ah");
        type60V.setVoltage(60.0);
        type60V.setCapacity(30.0);
        type60V.setWeight(22.0);
        type60V.setDimensions("40x30x25 cm");
        type60V.setDescription("Pin cao cấp cho xe máy điện công suất lớn");
        
        BatteryType type72V = new BatteryType();
        type72V.setName("Heavy Duty 72V-40Ah");
        type72V.setVoltage(72.0);
        type72V.setCapacity(40.0);
        type72V.setWeight(30.0);
        type72V.setDimensions("45x35x30 cm");
        type72V.setDescription("Pin công nghiệp cho xe tải điện");
        
        return List.of(type48V, type60V, type72V);
    }

    private List<Station> createStations(List<BatteryType> batteryTypes) {
        // Distribute battery types across stations
        return List.of(
            // Trạm ở TP.HCM
            createStation("Trạm Đổi Pin Quận 1", "123 Đường Nguyễn Huệ, Quận 1, TP.HCM", "TP.HCM", "Quận 1", 15, "0901234567", 10.7769, 106.7009, Station.Status.ACTIVE, batteryTypes.get(0)),
            createStation("Trạm Đổi Pin Quận 3", "456 Đường Lê Văn Sỹ, Quận 3, TP.HCM", "TP.HCM", "Quận 3", 15, "0902345678", 10.7867, 106.6837, Station.Status.ACTIVE, batteryTypes.get(1)),
            createStation("Trạm Đổi Pin Quận 7", "789 Đường Nguyễn Thị Thập, Quận 7, TP.HCM", "TP.HCM", "Quận 7", 15, "0903456789", 10.7307, 106.7218, Station.Status.ACTIVE, batteryTypes.get(2)),
            createStation("Trạm Đổi Pin Bình Thạnh", "101 Đường Xô Viết Nghệ Tĩnh, Quận Bình Thạnh, TP.HCM", "TP.HCM", "Quận Bình Thạnh", 15, "0904567890", 10.8015, 106.7181, Station.Status.ACTIVE, batteryTypes.get(0)),
            createStation("Trạm Đổi Pin Thủ Đức", "202 Đường Võ Văn Ngân, TP Thủ Đức, TP.HCM", "TP.HCM", "TP Thủ Đức", 15, "0905678901", 10.8494, 106.7719, Station.Status.ACTIVE, batteryTypes.get(1)),
            
            // Trạm ở Hà Nội - TẤT CẢ ĐỀU CAPACITY = 15
            createStation("Trạm Đổi Pin Hoàn Kiếm", "25 Phố Hàng Khay, Hoàn Kiếm, Hà Nội", "Hà Nội", "Hoàn Kiếm", 15, "0311112222", 21.0285, 105.8542, Station.Status.ACTIVE, batteryTypes.get(2)),
            createStation("Trạm Đổi Pin Cầu Giấy", "88 Đường Cầu Giấy, Cầu Giấy, Hà Nội", "Hà Nội", "Cầu Giấy", 15, "0333334444", 21.0314, 105.7969, Station.Status.ACTIVE, batteryTypes.get(0)),
            createStation("Trạm Đổi Pin Ba Đình", "156 Phố Nguyễn Thái Học, Ba Đình, Hà Nội", "Hà Nội", "Ba Đình", 15, "0355556666", 21.0364, 105.8325, Station.Status.ACTIVE, batteryTypes.get(1)),
            createStation("Trạm Đổi Pin Đống Đa", "45 Phố Láng, Đống Đa, Hà Nội", "Hà Nội", "Đống Đa", 15, "0377778888", 21.0136, 105.8270, Station.Status.ACTIVE, batteryTypes.get(2)),
            createStation("Trạm Đổi Pin Long Biên", "222 Đường Nguyễn Văn Cừ, Long Biên, Hà Nội", "Hà Nội", "Long Biên", 15, "0399990000", 21.0358, 105.8842, Station.Status.ACTIVE, batteryTypes.get(0))
        );
    }

    private Station createStation(String name, String location, String city, String district, int capacity, String contactInfo, double lat, double lng, Station.Status status, BatteryType batteryType) {
        Station station = new Station();
        station.setName(name);
        station.setLocation(location);
        station.setCity(city);
        station.setDistrict(district);
        station.setCapacity(capacity);
        station.setContactInfo(contactInfo);
        station.setLatitude(lat);
        station.setLongitude(lng);
        station.setStatus(status);
        station.setBatteryType(batteryType);  // ⭐ QUAN TRỌNG
        return station;
    }

    private List<ServicePackage> createServicePackages() {
        return List.of(
            createServicePackage("Gói Sinh Viên", "Gói dành cho sinh viên với 15 lần đổi pin mỗi tháng", new BigDecimal("200000.00"), 30, 15),
            createServicePackage("Gói Cơ Bản", "Gói dịch vụ cơ bản với 30 lần đổi pin mỗi tháng", new BigDecimal("350000.00"), 30, 30),
            createServicePackage("Gói Tiêu Chuẩn", "Gói tiêu chuẩn với 60 lần đổi pin và hỗ trợ ưu tiên", new BigDecimal("600000.00"), 30, 60),
            createServicePackage("Gói Cao Cấp", "Gói cao cấp với 200 lần đổi pin và hỗ trợ 24/7", new BigDecimal("900000.00"), 30, 200)
        );
    }

    private ServicePackage createServicePackage(String name, String desc, BigDecimal price, int duration, int maxSwaps) {
        ServicePackage pkg = new ServicePackage();
        pkg.setName(name);
        pkg.setDescription(desc);
        pkg.setPrice(price);
        pkg.setDuration(duration);
        pkg.setMaxSwaps(maxSwaps);
        return pkg;
    }

    private List<Battery> createBatteries(List<Station> stations, List<BatteryType> batteryTypes) {
        List<Battery> batteries = new java.util.ArrayList<>();
        
        // Dùng BatteryTypes đã được save vào DB
        BatteryType type48V = batteryTypes.get(0);  // 48V-20Ah
        BatteryType type60V = batteryTypes.get(1);  // 60V-30Ah
        BatteryType type72V = batteryTypes.get(2);  // 72V-40Ah
        
        // Trạm 1 (Quận 1) - 48V - 15 pin
        for (int i = 0; i < 15; i++) {
            Battery.Status status = i < 10 ? Battery.Status.AVAILABLE : (i < 13 ? Battery.Status.CHARGING : Battery.Status.IN_USE);
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15)); // 85-100%
            batteries.add(createBattery("VinFast 48V-20Ah", new BigDecimal("1.44"), health, status, stations.get(0), type48V));
        }
        
        // Trạm 2 (Quận 3) - 60V - 15 pin  
        for (int i = 0; i < 15; i++) {
            Battery.Status status = i < 11 ? Battery.Status.AVAILABLE : (i < 14 ? Battery.Status.CHARGING : Battery.Status.IN_USE);
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createBattery("Yadea 60V-30Ah", new BigDecimal("1.92"), health, status, stations.get(1), type60V));
        }
        
        // Trạm 3 (Quận 7) - 72V - 15 pin
        for (int i = 0; i < 15; i++) {
            Battery.Status status = i < 9 ? Battery.Status.AVAILABLE : (i < 13 ? Battery.Status.CHARGING : Battery.Status.IN_USE);
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createBattery("Pega 72V-40Ah", new BigDecimal("2.02"), health, status, stations.get(2), type72V));
        }
        
        // Trạm 4 (Bình Thạnh) - 48V - 15 pin
        for (int i = 0; i < 15; i++) {
            Battery.Status status = i < 12 ? Battery.Status.AVAILABLE : (i < 14 ? Battery.Status.CHARGING : Battery.Status.MAINTENANCE);
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createBattery("Dibao 48V-20Ah", new BigDecimal("2.10"), health, status, stations.get(3), type48V));
        }
        
        // Trạm 5 (Thủ Đức) - 60V - 15 pin
        for (int i = 0; i < 15; i++) {
            Battery.Status status = i < 11 ? Battery.Status.AVAILABLE : (i < 13 ? Battery.Status.CHARGING : Battery.Status.IN_USE);
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createBattery("VinFast 60V-30Ah", new BigDecimal("2.16"), health, status, stations.get(4), type60V));
        }
        
        // Trạm 6 (Hoàn Kiếm) - 72V - 15 pin
        for (int i = 0; i < 15; i++) {
            Battery.Status status = i < 10 ? Battery.Status.AVAILABLE : (i < 13 ? Battery.Status.CHARGING : Battery.Status.IN_USE);
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createBattery("VinFast 72V-40Ah", new BigDecimal("1.68"), health, status, stations.get(5), type72V));
        }
        
        // Trạm 7 (Cầu Giấy) - 48V - 15 pin
        for (int i = 0; i < 15; i++) {
            Battery.Status status = i < 10 ? Battery.Status.AVAILABLE : (i < 13 ? Battery.Status.CHARGING : Battery.Status.IN_USE);
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createBattery("Yadea 48V-20Ah", new BigDecimal("2.52"), health, status, stations.get(6), type48V));
        }
        
        // Trạm 8 (Ba Đình) - 60V - 15 pin
        for (int i = 0; i < 15; i++) {
            Battery.Status status = i < 10 ? Battery.Status.AVAILABLE : (i < 13 ? Battery.Status.CHARGING : Battery.Status.IN_USE);
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createBattery("Pega 60V-30Ah", new BigDecimal("1.80"), health, status, stations.get(7), type60V));
        }
        
        // Trạm 9 (Đống Đa) - 72V - 15 pin
        for (int i = 0; i < 15; i++) {
            Battery.Status status = i < 10 ? Battery.Status.AVAILABLE : (i < 13 ? Battery.Status.CHARGING : Battery.Status.IN_USE);
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createBattery("Dibao 72V-40Ah", new BigDecimal("1.20"), health, status, stations.get(8), type72V));
        }
        
        // Trạm 10 (Long Biên) - 48V - 15 pin
        for (int i = 0; i < 15; i++) {
            Battery.Status status = i < 10 ? Battery.Status.AVAILABLE : (i < 13 ? Battery.Status.CHARGING : Battery.Status.IN_USE);
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createBattery("VinFast 48V-20Ah", new BigDecimal("1.06"), health, status, stations.get(9), type48V));
        }
        
        // Tạo thêm 5 pin không được gán vào trạm nào để test chức năng thêm pin
        for (int i = 0; i < 5; i++) {
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createUnassignedBattery("Available Battery " + (i + 1), new BigDecimal("2.00"), health, Battery.Status.AVAILABLE, type48V));
        }
        
        // Tạo thêm 5 pin đang được sử dụng bên ngoài (currentStation = null, status = IN_USE)
        for (int i = 0; i < 5; i++) {
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15));
            batteries.add(createUnassignedBattery("In-Use Battery " + (i + 1), new BigDecimal("2.00"), health, Battery.Status.IN_USE, type60V));
        }
        
        return batteries;
    }

    private Battery createBattery(String model, BigDecimal capacity, BigDecimal stateOfHealth, Battery.Status status, Station station, BatteryType batteryType) {
        Battery battery = new Battery();
        battery.setModel(model);
        battery.setCapacity(capacity);
        battery.setStateOfHealth(stateOfHealth);
        battery.setChargeLevel(status == Battery.Status.AVAILABLE ? BigDecimal.valueOf(100.0) : BigDecimal.valueOf(50.0));
        battery.setStatus(status);
        battery.setCurrentStation(station);
        battery.setBatteryType(batteryType);
        battery.setManufactureDate(LocalDate.now().minusMonths((long) (Math.random() * 12)));
        battery.setUsageCount((int) (Math.random() * 100));
        return battery;
    }

    private Battery createUnassignedBattery(String model, BigDecimal capacity, BigDecimal stateOfHealth, Battery.Status status, BatteryType batteryType) {
        Battery battery = new Battery();
        battery.setModel(model);
        battery.setCapacity(capacity);
        battery.setStateOfHealth(stateOfHealth);
        battery.setChargeLevel(BigDecimal.valueOf(100.0));
        battery.setStatus(status);
        battery.setCurrentStation(null); // Không gán vào trạm nào
        battery.setBatteryType(batteryType);
        battery.setManufactureDate(LocalDate.now().minusMonths((long) (Math.random() * 6)));
        battery.setUsageCount(0);
        return battery;
    }

    private List<Vehicle> createVehicles(List<User> users, List<Battery> batteries) {
        List<User> drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).toList();
        
        // Lấy batteryType từ battery đã tạo
        BatteryType type1 = batteries.get(0).getBatteryType();
        BatteryType type2 = batteries.get(15).getBatteryType();
        
        return List.of(
            createVehicle("VFMOTO2024001234", "59-A1 123.45", "VinFast Klara A2", drivers.get(0), type1, batteries.get(145)),
            createVehicle("VFMOTO2024001235", "51-F1 456.78", "VinFast Impes", drivers.get(1), type2, batteries.get(146)),
            createVehicle("YADEA2024001236", "59-B2 789.01", "Yadea Xmen Neo", drivers.get(2), type1, batteries.get(147)),
            createVehicle("PEGA2024001237", "51-G1 234.56", "Pega NewTech", drivers.get(3), type2, batteries.get(148)),
            createVehicle("DIBAO2024001238", "59-C3 567.89", "Dibao Angelina", drivers.get(4), type1, batteries.get(149))
        );
    }

    private Vehicle createVehicle(String vin, String plateNumber, String model, User driver, BatteryType batteryType, Battery currentBattery) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vin);
        vehicle.setPlateNumber(plateNumber);
        vehicle.setModel(model);
        vehicle.setDriver(driver);
        vehicle.setBatteryType(batteryType);
        vehicle.setCurrentBattery(currentBattery);
        
        // Cập nhật battery status
        if (currentBattery != null) {
            currentBattery.setStatus(Battery.Status.IN_USE);
            currentBattery.setCurrentStation(null); // Pin đang trên xe, không ở trạm
        }
        
        return vehicle;
    }

    private List<DriverSubscription> createDriverSubscriptions(List<User> users, List<ServicePackage> packages) {
        List<User> drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).toList();
        return List.of(
            createDriverSubscription(drivers.get(0), packages.get(1), LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 30), 25, DriverSubscription.Status.ACTIVE),
            createDriverSubscription(drivers.get(1), packages.get(0), LocalDate.of(2025, 10, 5), LocalDate.of(2025, 11, 4), 12, DriverSubscription.Status.ACTIVE),
            createDriverSubscription(drivers.get(2), packages.get(2), LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30), 0, DriverSubscription.Status.EXPIRED),
            createDriverSubscription(drivers.get(3), packages.get(1), LocalDate.of(2025, 10, 10), LocalDate.of(2025, 11, 9), 28, DriverSubscription.Status.ACTIVE),
            createDriverSubscription(drivers.get(4), packages.get(3), LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 30), 195, DriverSubscription.Status.ACTIVE)
        );
    }

    private DriverSubscription createDriverSubscription(User driver, ServicePackage pkg, LocalDate start, LocalDate end, int remainingSwaps, DriverSubscription.Status status) {
        DriverSubscription subscription = new DriverSubscription();
        subscription.setDriver(driver);
        subscription.setServicePackage(pkg);
        subscription.setStartDate(start);
        subscription.setEndDate(end);
        subscription.setRemainingSwaps(remainingSwaps);
        subscription.setStatus(status);
        return subscription;
    }

    private List<StaffStationAssignment> createStaffStationAssignments(List<User> users, List<Station> stations) {
        List<User> staff = users.stream().filter(u -> u.getRole() == User.Role.STAFF).toList();
        
        // Assign mỗi staff vào nhiều stations
        return List.of(
            // Staff 1 (index 0) - Phụ trách 3 trạm ở TP.HCM
            createStaffStationAssignment(staff.get(0), stations.get(0), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Quận 1
            createStaffStationAssignment(staff.get(0), stations.get(1), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Quận 3
            createStaffStationAssignment(staff.get(0), stations.get(2), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Quận 7
            
            // Staff 2 (index 1) - Phụ trách 3 trạm ở TP.HCM
            createStaffStationAssignment(staff.get(1), stations.get(2), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Quận 7 (overlap với staff1)
            createStaffStationAssignment(staff.get(1), stations.get(3), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Bình Thạnh
            createStaffStationAssignment(staff.get(1), stations.get(4), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Thủ Đức
            
            // Staff 3 (index 2) - Phụ trách 5 trạm ở Hà Nội
            createStaffStationAssignment(staff.get(2), stations.get(5), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Hoàn Kiếm
            createStaffStationAssignment(staff.get(2), stations.get(6), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Cầu Giấy
            createStaffStationAssignment(staff.get(2), stations.get(7), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Ba Đình
            createStaffStationAssignment(staff.get(2), stations.get(8), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Đống Đa
            createStaffStationAssignment(staff.get(2), stations.get(9), LocalDateTime.of(2025, 10, 1, 8, 0))   // Long Biên
        );
    }

    private StaffStationAssignment createStaffStationAssignment(User staff, Station station, LocalDateTime assignedAt) {
        StaffStationAssignment assignment = new StaffStationAssignment();
        assignment.setStaff(staff);
        assignment.setStation(station);
        assignment.setAssignedAt(assignedAt);
        return assignment;
    }

    private List<StationInventory> createStationInventory(List<Station> stations, List<Battery> batteries) {
        List<StationInventory> inventory = new java.util.ArrayList<>();
        
        // Tạo inventory cho TẤT CẢ batteries có currentStation
        for (Battery battery : batteries) {
            if (battery.getCurrentStation() != null) {
                StationInventory.Status status;
                
                // Map Battery.Status -> StationInventory.Status
                switch (battery.getStatus()) {
                    case AVAILABLE:
                        status = StationInventory.Status.AVAILABLE;
                        break;
                    case CHARGING:
                        status = StationInventory.Status.AVAILABLE; // Pin đang sạc vẫn available
                        break;
                    case MAINTENANCE:
                        status = StationInventory.Status.MAINTENANCE;
                        break;
                    case IN_USE:
                        status = StationInventory.Status.RESERVED; // Pin đang dùng = reserved
                        break;
                    default:
                        status = StationInventory.Status.AVAILABLE;
                }
                
                inventory.add(createStationInventory(battery.getCurrentStation(), battery, status));
            }
        }
        
        return inventory;
    }

    private StationInventory createStationInventory(Station station, Battery battery, StationInventory.Status status) {
        StationInventory inventory = new StationInventory();
        inventory.setStation(station);
        inventory.setBattery(battery);
        inventory.setStatus(status);
        inventory.setLastUpdate(LocalDateTime.now());
        return inventory;
    }

    private List<Booking> createBookings(List<User> users, List<Vehicle> vehicles, List<Station> stations) {
        List<User> drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).toList();
        List<User> staff = users.stream().filter(u -> u.getRole() == User.Role.STAFF).toList();

        // Tạo booking 24/7 để thể hiện trạm hoạt động liên tục
        LocalDateTime baseTime = LocalDateTime.of(2025, 10, 11, 6, 0); // 6:00 AM hôm nay
        
        return List.of(
            createBooking(drivers.get(0), vehicles.get(0), stations.get(0), baseTime.plusHours(2), Booking.Status.CONFIRMED, staff.get(0)),    // 8:00 AM - Sáng sớm
            createBooking(drivers.get(1), vehicles.get(1), stations.get(1), baseTime.plusHours(16), Booking.Status.PENDING, null),           // 10:00 PM - Tối muộn
            createBooking(drivers.get(2), vehicles.get(2), stations.get(2), baseTime.plusHours(6), Booking.Status.CONFIRMED, staff.get(1)),   // 12:00 PM - Trưa
            createBooking(drivers.get(3), vehicles.get(3), stations.get(3), baseTime.plusHours(20), Booking.Status.CONFIRMED, staff.get(1)),  // 2:00 AM - Đêm khuya
            createBooking(drivers.get(4), vehicles.get(4), stations.get(4), baseTime.plusHours(10), Booking.Status.PENDING, null)            // 4:00 PM - Chiều
        );
    }

    private Booking createBooking(User driver, Vehicle vehicle, Station station, LocalDateTime bookingTime, Booking.Status status, User confirmedBy) {
        Booking booking = new Booking();
        booking.setDriver(driver);
        booking.setVehicle(vehicle);
        booking.setStation(station);
        booking.setBookingTime(bookingTime);
        booking.setConfirmationCode(status == Booking.Status.CONFIRMED ? generateConfirmationCode() : null);
        booking.setStatus(status);
        booking.setConfirmedBy(confirmedBy);  // ⭐ LƯU AI ĐÃ CONFIRM
        return booking;
    }
    
    private String generateConfirmationCode() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String numbers = "0123456789";
        StringBuilder code = new StringBuilder();
        
        // 3 chữ cái
        for (int i = 0; i < 3; i++) {
            code.append(letters.charAt((int)(Math.random() * letters.length())));
        }
        
        // 3 số
        for (int i = 0; i < 3; i++) {
            code.append(numbers.charAt((int)(Math.random() * numbers.length())));
        }
        
        return code.toString();
    }

    private List<SwapTransaction> createSwapTransactions(List<User> users, List<Vehicle> vehicles, List<Station> stations, List<Battery> batteries) {
        List<User> drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).toList();
        List<User> staff = users.stream().filter(u -> u.getRole() == User.Role.STAFF).toList();
        
        // ✅ Swap MIỄN PHÍ với subscription → cost = 0
        return List.of(
            createSwapTransaction(drivers.get(0), vehicles.get(0), stations.get(0), staff.get(0), 
                batteries.get(5), batteries.get(0), LocalDateTime.now().minusHours(2), 
                LocalDateTime.now().minusHours(2).plusMinutes(3), BigDecimal.ZERO, SwapTransaction.Status.COMPLETED),
            createSwapTransaction(drivers.get(1), vehicles.get(1), stations.get(1), staff.get(1), 
                null, batteries.get(2), LocalDateTime.now().minusHours(1), 
                LocalDateTime.now().minusHours(1).plusMinutes(2), BigDecimal.ZERO, SwapTransaction.Status.COMPLETED)
        );
    }

    private SwapTransaction createSwapTransaction(User driver, Vehicle vehicle, Station station, User staff,
                                                Battery swapOut, Battery swapIn, LocalDateTime start, LocalDateTime end,
                                                BigDecimal cost, SwapTransaction.Status status) {
        SwapTransaction transaction = new SwapTransaction();
        transaction.setDriver(driver);
        transaction.setVehicle(vehicle);
        transaction.setStation(station);
        transaction.setStaff(staff);
        transaction.setSwapOutBattery(swapOut);
        transaction.setSwapInBattery(swapIn);
        transaction.setStartTime(start);
        transaction.setEndTime(end);
        transaction.setCost(cost);
        transaction.setStatus(status);
        
        // ⭐ LƯU SNAPSHOT thông tin pin tại thời điểm swap
        if (swapOut != null) {
            transaction.setSwapOutBatteryModel(swapOut.getModel());
            transaction.setSwapOutBatteryChargeLevel(swapOut.getChargeLevel());
            transaction.setSwapOutBatteryHealth(swapOut.getStateOfHealth());
        }
        
        if (swapIn != null) {
            transaction.setSwapInBatteryModel(swapIn.getModel());
            transaction.setSwapInBatteryChargeLevel(swapIn.getChargeLevel());
            transaction.setSwapInBatteryHealth(swapIn.getStateOfHealth());
        }
        
        // ⭐ CẬP NHẬT VEHICLE.CURRENTBATTERY sau khi swap
        // Nếu status = COMPLETED → gắn pin mới lên xe
        if (status == SwapTransaction.Status.COMPLETED && swapOut != null) {
            vehicle.setCurrentBattery(swapOut);
            vehicleRepository.save(vehicle);
        }
        
        return transaction;
    }

    private List<Payment> createPayments(List<SwapTransaction> transactions, List<DriverSubscription> subscriptions) {
        // ✅ Payment chỉ cho SUBSCRIPTION, không có payment cho swap transaction
        return List.of(
            // Payment cho subscription của driver1 (Gói Cơ Bản - 350k)
            createPayment(null, subscriptions.get(0), new BigDecimal("350000.00"), "MOMO", 
                          LocalDateTime.of(2025, 10, 1, 9, 30), Payment.Status.COMPLETED),
            
            // Payment cho subscription của driver2 (Gói Sinh Viên - 200k)
            createPayment(null, subscriptions.get(1), new BigDecimal("200000.00"), "MOMO", 
                          LocalDateTime.of(2025, 10, 5, 14, 15), Payment.Status.COMPLETED),
            
            // Payment cho subscription EXPIRED của driver3 (Gói Tiêu Chuẩn - 600k) - đã thanh toán trước đó
            createPayment(null, subscriptions.get(2), new BigDecimal("600000.00"), "MOMO", 
                          LocalDateTime.of(2025, 9, 1, 10, 0), Payment.Status.COMPLETED),
            
            // Payment cho subscription của driver4 (Gói Cơ Bản - 350k)
            createPayment(null, subscriptions.get(3), new BigDecimal("350000.00"), "MOMO", 
                          LocalDateTime.of(2025, 10, 10, 10, 0), Payment.Status.COMPLETED),
            
            // Payment cho subscription của driver5 (Gói Cao Cấp - 900k)
            createPayment(null, subscriptions.get(4), new BigDecimal("900000.00"), "MOMO", 
                          LocalDateTime.of(2025, 10, 1, 11, 45), Payment.Status.COMPLETED)
        );
    }

    private Payment createPayment(SwapTransaction transaction, DriverSubscription subscription, BigDecimal amount,
                                String method, LocalDateTime date, Payment.Status status) {
        Payment payment = new Payment();
        payment.setTransaction(transaction);
        payment.setSubscription(subscription);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setPaymentDate(date);
        payment.setStatus(status);
        return payment;
    }

    private List<BatteryHistory> createBatteryHistory(List<Battery> batteries, List<User> users, List<Station> stations, List<Vehicle> vehicles) {
        List<User> staff = users.stream().filter(u -> u.getRole() == User.Role.STAFF).toList();
        List<BatteryHistory> history = new java.util.ArrayList<>();
        
        // Event cho transaction 1 (driver0, vehicle0, station0)
        Battery swapOut1 = batteries.get(5);
        Battery swapIn1 = batteries.get(0);
        
        history.add(createBatteryHistory(swapOut1, "SwapOut", LocalDateTime.now().minusHours(2), 
                                        stations.get(0), vehicles.get(0), staff.get(0)));
        history.add(createBatteryHistory(swapIn1, "SwapIn", LocalDateTime.now().minusHours(2), 
                                        stations.get(0), vehicles.get(0), staff.get(0)));
        history.add(createBatteryHistory(swapIn1, "Charging", LocalDateTime.now().minusHours(2).plusMinutes(5), 
                                        stations.get(0), null, staff.get(0)));
        
        // Event cho transaction 2 (driver1, vehicle1, station1)
        Battery swapIn2 = batteries.get(2);
        
        history.add(createBatteryHistory(swapIn2, "SwapIn", LocalDateTime.now().minusHours(1), 
                                        stations.get(1), vehicles.get(1), staff.get(1)));
        history.add(createBatteryHistory(swapIn2, "Charging", LocalDateTime.now().minusHours(1).plusMinutes(5), 
                                        stations.get(1), null, staff.get(1)));
        
        // Event bảo trì
        history.add(createBatteryHistory(batteries.get(8), "Maintenance", LocalDateTime.now().minusHours(4), 
                                        stations.get(4), null, staff.get(2)));
        
        return history;
    }

    private BatteryHistory createBatteryHistory(Battery battery, String eventType, LocalDateTime eventTime,
                                              Station station, Vehicle vehicle, User staff) {
        BatteryHistory history = new BatteryHistory();
        history.setBattery(battery);
        history.setEventType(eventType);
        history.setEventTime(eventTime);
        history.setRelatedStation(station);
        history.setRelatedVehicle(vehicle);
        history.setStaff(staff);
        return history;
    }

    private List<SupportTicket> createSupportTickets(List<User> users, List<Station> stations) {
        List<User> drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).toList();
        
        return List.of(
            createSupportTicket(drivers.get(0), stations.get(0), "Pin yếu sau đổi", "Pin tôi vừa đổi chỉ chạy được 40km thay vì 60km như bình thường", SupportTicket.Status.OPEN),
            createSupportTicket(drivers.get(1), stations.get(1), "Trạm đổi pin bị kẹt", "Máy đổi pin tại trạm Quận 3 không nhả pin ra được", SupportTicket.Status.IN_PROGRESS),
            createSupportTicket(drivers.get(2), null, "App không kết nối", "Ứng dụng báo lỗi kết nối khi tôi cố gắng đặt lịch đổi pin", SupportTicket.Status.RESOLVED)
        );
    }

    private SupportTicket createSupportTicket(User driver, Station station, String subject, String description, SupportTicket.Status status) {
        SupportTicket ticket = new SupportTicket();
        ticket.setDriver(driver);
        ticket.setStation(station);
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setStatus(status);
        ticket.setCreatedAt(LocalDateTime.now().minusHours(2));
        return ticket;
    }

    private List<TicketResponse> createTicketResponses(List<SupportTicket> tickets, List<User> users) {
        List<User> staff = users.stream().filter(u -> u.getRole() == User.Role.STAFF).toList();
        
        return List.of(
            createTicketResponse(tickets.get(1), staff.get(0), "Chúng tôi đã tiếp nhận yêu cầu và sẽ cử kỹ thuật viên đến kiểm tra trong vòng 2 giờ."),
            createTicketResponse(tickets.get(2), staff.get(1), "Lỗi đã được khắc phục trong bản cập nhật mới. Vui lòng cập nhật ứng dụng.")
        );
    }

    private TicketResponse createTicketResponse(SupportTicket ticket, User staff, String message) {
        TicketResponse response = new TicketResponse();
        response.setTicket(ticket);
        response.setStaff(staff);
        response.setMessage(message);
        response.setResponseTime(LocalDateTime.now().minusHours(1));
        return response;
    }


}