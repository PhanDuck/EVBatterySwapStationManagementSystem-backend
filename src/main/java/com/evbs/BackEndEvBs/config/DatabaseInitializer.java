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
import java.util.ArrayList;
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
    private final StaffStationAssignmentRepository staffStationAssignmentRepository;
    private final BookingRepository bookingRepository;
    private final SwapTransactionRepository swapTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final TicketResponseRepository ticketResponseRepository;

    public DatabaseInitializer(UserRepository userRepository, StationRepository stationRepository,
                               BatteryRepository batteryRepository, BatteryTypeRepository batteryTypeRepository,
                               VehicleRepository vehicleRepository,
                               ServicePackageRepository servicePackageRepository,
                               DriverSubscriptionRepository driverSubscriptionRepository,
                               StationInventoryRepository stationInventoryRepository,
                               StaffStationAssignmentRepository staffStationAssignmentRepository,
                               BookingRepository bookingRepository,
                               SwapTransactionRepository swapTransactionRepository, PaymentRepository paymentRepository,
                               SupportTicketRepository supportTicketRepository,
                               TicketResponseRepository ticketResponseRepository) {
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
        this.batteryRepository = batteryRepository;
        this.batteryTypeRepository = batteryTypeRepository;
        this.vehicleRepository = vehicleRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.driverSubscriptionRepository = driverSubscriptionRepository;
        this.stationInventoryRepository = stationInventoryRepository;
        this.staffStationAssignmentRepository = staffStationAssignmentRepository;
        this.bookingRepository = bookingRepository;
        this.swapTransactionRepository = swapTransactionRepository;
        this.paymentRepository = paymentRepository;
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
        log.info("Created {} battery types", batteryTypes.size());

        // 3. Tạo Stations (cần BatteryTypes) - 10 TRẠM
        List<Station> stations = createStations(batteryTypes);
        stationRepository.saveAll(stations);
        log.info("Created {} stations", stations.size());

        // 4. Tạo Service Packages
        List<ServicePackage> packages = createServicePackages();
        servicePackageRepository.saveAll(packages);
        log.info("Created {} service packages", packages.size());

        // 5. Tạo Batteries (cần BatteryTypes và Stations)
        List<Battery> batteries = createBatteries(stations, batteryTypes);
        batteryRepository.saveAll(batteries);
        log.info("Created {} batteries", batteries.size());

        // 6. Tạo Vehicles (cần drivers và batteries) - ĐẢM BẢO CURRENT BATTERY ĐƯỢC GẮN
        List<Vehicle> vehicles = createVehicles(users, batteries);
        vehicleRepository.saveAll(vehicles);

        // UPDATE LẠI BATTERY STATUS SAU KHI GẮN VÀO XE
        updateBatteriesAfterVehicleAssignment(vehicles);
        log.info("Created {} vehicles with current batteries", vehicles.size());

        // 7. Tạo Driver Subscriptions
        List<DriverSubscription> subscriptions = createDriverSubscriptions(users, packages);
        driverSubscriptionRepository.saveAll(subscriptions);
        log.info("Created {} subscriptions", subscriptions.size());

        // 8. Tạo Staff Station Assignments
        List<StaffStationAssignment> assignments = createStaffStationAssignments(users, stations);
        staffStationAssignmentRepository.saveAll(assignments);
        log.info("Created {} staff station assignments", assignments.size());

        // 9. Tạo Station Inventory (chỉ pin trong kho)
        List<StationInventory> inventory = createStationInventory(batteries);
        stationInventoryRepository.saveAll(inventory);
        log.info("Created {} station inventory records", inventory.size());

        // 10. KHÔNG tạo Bookings (đã xóa theo yêu cầu)
        log.info("Skipped booking creation as requested");

        // 11. Tạo Swap Transactions (lịch sử)
        List<SwapTransaction> transactions = createSwapTransactions(users, vehicles, stations, batteries);
        swapTransactionRepository.saveAll(transactions);
        log.info("Created {} swap transactions", transactions.size());

        // 12. Tạo Payments
        List<Payment> payments = createPayments(subscriptions);
        paymentRepository.saveAll(payments);
        log.info("Created {} payments", payments.size());

        // 13. Tạo Support Tickets
        List<SupportTicket> tickets = createSupportTickets(users, stations);
        supportTicketRepository.saveAll(tickets);
        log.info("Created {} support tickets", tickets.size());

        // 14. Tạo Ticket Responses
        List<TicketResponse> responses = createTicketResponses(tickets, users);
        ticketResponseRepository.saveAll(responses);
        log.info("Created {} ticket responses", responses.size());
    }

    private List<User> createUsers() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode("password123");

        return List.of(
                // Admin users
                createUser("Vinh Admin", "vinhmtse180031@fpt.edu.vn", "0901000001", encodedPassword, User.Role.ADMIN, User.Status.ACTIVE),
                createUser("Trí Admin", "lephuoctri2205@gmail.com", "0901000002", encodedPassword, User.Role.ADMIN, User.Status.ACTIVE),

                // Staff users
                createUser("Nguyễn Văn Staff", "vinhvip4508@gmail.com", "0902000001", encodedPassword, User.Role.STAFF, User.Status.ACTIVE),
                createUser("Trần Thị Staff", "staff2@evbs.com", "0902000002", encodedPassword, User.Role.STAFF, User.Status.ACTIVE),
                createUser("Lê Văn Staff", "staff3@evbs.com", "0902000003", encodedPassword, User.Role.STAFF, User.Status.ACTIVE),
                createUser("Phạm Văn Staff", "staff4@evbs.com", "0902000004", encodedPassword, User.Role.STAFF, User.Status.ACTIVE),

                // Driver users - TẠO 10 DRIVER
                createUser("Nguyễn Văn An", "driver1@gmail.com", "0903000001", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
                createUser("Trần Thị Bình", "driver2@gmail.com", "0903000002", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
                createUser("Lê Văn Cường", "driver3@gmail.com", "0903000003", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
                createUser("Phạm Thị Dung", "driver4@gmail.com", "0903000004", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
                createUser("Hoàng Văn Em", "driver5@gmail.com", "0903000005", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
                createUser("Vũ Thị Phượng", "driver6@gmail.com", "0903000006", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
                createUser("Lê Phước Trí", "driver7@gmail.com", "0903000007", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
                createUser("Nguyễn Thị Hương", "driver8@gmail.com", "0903000008", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
                createUser("Trần Văn Hùng", "driver9@gmail.com", "0903000009", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
                createUser("Phạm Văn Đức", "driver10@gmail.com", "0903000010", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE)
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
        // TẠO 10 TRẠM Ở TP.HCM
        return List.of(
                createStation("Trạm Đổi Pin Quận 1", "123 Đường Nguyễn Huệ, Quận 1, TP.HCM", "TP.HCM", "Quận 1", 20, "0901234567", 10.7769, 106.7009, Station.Status.ACTIVE, batteryTypes.get(0)),
                createStation("Trạm Đổi Pin Quận 3", "456 Đường Lê Văn Sỹ, Quận 3, TP.HCM", "TP.HCM", "Quận 3", 20, "0902345678", 10.7867, 106.6837, Station.Status.ACTIVE, batteryTypes.get(1)),
                createStation("Trạm Đổi Pin Quận 7", "789 Đường Nguyễn Thị Thập, Quận 7, TP.HCM", "TP.HCM", "Quận 7", 20, "0903456789", 10.7307, 106.7218, Station.Status.ACTIVE, batteryTypes.get(2)),
                createStation("Trạm Đổi Pin Bình Thạnh", "101 Đường Xô Viết Nghệ Tĩnh, Quận Bình Thạnh, TP.HCM", "TP.HCM", "Quận Bình Thạnh", 20, "0904567890", 10.8015, 106.7181, Station.Status.ACTIVE, batteryTypes.get(0)),
                createStation("Trạm Đổi Pin Thủ Đức", "202 Đường Võ Văn Ngân, TP Thủ Đức, TP.HCM", "TP.HCM", "TP Thủ Đức", 20, "0905678901", 10.8494, 106.7719, Station.Status.ACTIVE, batteryTypes.get(1)),
                createStation("Trạm Đổi Pin Gò Vấp", "45 Đường Quang Trung, Gò Vấp, TP.HCM", "TP.HCM", "Gò Vấp", 20, "0906789012", 10.8376, 106.6731, Station.Status.ACTIVE, batteryTypes.get(2)),
                createStation("Trạm Đổi Pin Tân Bình", "88 Đường Cộng Hòa, Tân Bình, TP.HCM", "TP.HCM", "Tân Bình", 20, "0907890123", 10.7992, 106.6530, Station.Status.ACTIVE, batteryTypes.get(0)),
                createStation("Trạm Đổi Pin Phú Nhuận", "156 Đường Phan Đăng Lưu, Phú Nhuận, TP.HCM", "TP.HCM", "Phú Nhuận", 20, "0908901234", 10.7980, 106.6831, Station.Status.ACTIVE, batteryTypes.get(1)),
                createStation("Trạm Đổi Pin Quận 10", "222 Đường 3/2, Quận 10, TP.HCM", "TP.HCM", "Quận 10", 20, "0909012345", 10.7679, 106.6663, Station.Status.ACTIVE, batteryTypes.get(2)),
                createStation("Trạm Đổi Pin Quận 5", "333 Đường Nguyễn Trãi, Quận 5, TP.HCM", "TP.HCM", "Quận 5", 20, "0910123456", 10.7540, 106.6674, Station.Status.ACTIVE, batteryTypes.get(0))
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
        station.setBatteryType(batteryType);
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
        List<Battery> batteries = new ArrayList<>();
        String[] brands = {"VinFast", "Yadea", "Pega", "Dibao"};

        // ==================================================
        // 1. PIN Ở TRẠM (currentStation != null, status = AVAILABLE/CHARGING/MAINTENANCE)
        // ==================================================

        // Mỗi trạm có 10 pin
        for (int stationIndex = 0; stationIndex < stations.size(); stationIndex++) {
            Station station = stations.get(stationIndex);
            BatteryType batteryType = station.getBatteryType();

            for (int i = 0; i < 10; i++) {
                Battery.Status status;
                if (i < 2 && stationIndex % 3 == 0) {
                    status = Battery.Status.MAINTENANCE; // Một số trạm có pin bảo trì
                } else if (i < 8) {
                    status = Battery.Status.AVAILABLE;
                } else {
                    status = Battery.Status.CHARGING;
                }

                BigDecimal health = status == Battery.Status.MAINTENANCE ?
                        new BigDecimal("" + (55 + (Math.random() * 15))) : // 55-70%
                        new BigDecimal("" + (85 + (Math.random() * 15)));  // 85-100%
                BigDecimal chargeLevel = status == Battery.Status.AVAILABLE ?
                        new BigDecimal("100.0") : new BigDecimal("" + (20 + (Math.random() * 70)));

                String model = brands[(stationIndex + i) % brands.length] + " " +
                        batteryType.getVoltage().intValue() + "V-" + batteryType.getCapacity().intValue() + "Ah";

                batteries.add(createBatteryAtStation(
                        model, new BigDecimal(batteryType.getCapacity().toString()), health, chargeLevel, status,
                        station, batteryType
                ));
            }
        }

        // ==================================================
        // 2. PIN TRONG KHO (currentStation = null, status = AVAILABLE/MAINTENANCE)
        // ==================================================

        // 30 pin trong kho (10 mỗi loại)
        for (int i = 0; i < 30; i++) {
            BatteryType batteryType = batteryTypes.get(i % 3);
            Battery.Status status = i % 5 == 0 ? Battery.Status.MAINTENANCE : Battery.Status.AVAILABLE;
            BigDecimal health = status == Battery.Status.MAINTENANCE ?
                    new BigDecimal("" + (50 + (Math.random() * 20))) : // 50-70%
                    new BigDecimal("" + (90 + (Math.random() * 10)));  // 90-100%

            String model = brands[i % brands.length] + " " +
                    batteryType.getVoltage().intValue() + "V-" + batteryType.getCapacity().intValue() + "Ah";

            batteries.add(createUnassignedBattery(
                    model, new BigDecimal(batteryType.getCapacity().toString()), health, status, batteryType
            ));
        }

        // ==================================================
        // 3. PIN ĐANG ĐƯỢC XE SỬ DỤNG (currentStation = null, status = IN_USE)
        // ==================================================

        // 10 pin IN_USE cho 10 xe
        for (int i = 0; i < 10; i++) {
            BatteryType batteryType = batteryTypes.get(i % 3);
            BigDecimal health = new BigDecimal("" + (70 + (Math.random() * 25))); // 70-95%
            BigDecimal chargeLevel = new BigDecimal("" + (20 + (Math.random() * 60))); // 20-80%

            String model = brands[i % brands.length] + " " +
                    batteryType.getVoltage().intValue() + "V-" + batteryType.getCapacity().intValue() + "Ah";

            batteries.add(createUnassignedBattery(
                    model, new BigDecimal(batteryType.getCapacity().toString()), health, Battery.Status.IN_USE, batteryType
            ));
        }

        return batteries;
    }

    private Battery createBatteryAtStation(String model, BigDecimal capacity, BigDecimal stateOfHealth,
                                           BigDecimal chargeLevel, Battery.Status status,
                                           Station station, BatteryType batteryType) {
        Battery battery = new Battery();
        battery.setModel(model);
        battery.setCapacity(capacity);
        battery.setStateOfHealth(stateOfHealth);
        battery.setChargeLevel(chargeLevel);
        battery.setStatus(status);
        battery.setCurrentStation(station); // Pin ở trạm
        battery.setBatteryType(batteryType);
        battery.setManufactureDate(LocalDate.now().minusMonths((long) (Math.random() * 12)));
        battery.setUsageCount((int) (Math.random() * 50));
        return battery;
    }

    private Battery createUnassignedBattery(String model, BigDecimal capacity, BigDecimal stateOfHealth,
                                            Battery.Status status, BatteryType batteryType) {
        Battery battery = new Battery();
        battery.setModel(model);
        battery.setCapacity(capacity);
        battery.setStateOfHealth(stateOfHealth);

        // Charge level logic:
        // - IN_USE: random 20-80% (đang được sử dụng)
        // - AVAILABLE: 100% (trong kho, sẵn sàng sử dụng)
        // - MAINTENANCE: random 30-80% (đang bảo trì)
        if (status == Battery.Status.IN_USE) {
            battery.setChargeLevel(new BigDecimal("" + (20 + (Math.random() * 60))));
        } else if (status == Battery.Status.AVAILABLE) {
            battery.setChargeLevel(new BigDecimal("100.0"));
        } else {
            battery.setChargeLevel(new BigDecimal("" + (30 + (Math.random() * 50))));
        }

        battery.setStatus(status);
        battery.setCurrentStation(null); // Không ở trạm nào
        battery.setBatteryType(batteryType);
        battery.setManufactureDate(LocalDate.now().minusMonths((long) (Math.random() * 6)));
        battery.setUsageCount(status == Battery.Status.IN_USE ? (int) (Math.random() * 100) : 0);
        return battery;
    }

    private List<Vehicle> createVehicles(List<User> users, List<Battery> batteries) {
        List<User> drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).toList();
        List<Vehicle> vehicles = new ArrayList<>();

        // Lấy các pin IN_USE từ batteries (10 pin cuối cùng)
        List<Battery> inUseBatteries = batteries.stream()
                .filter(b -> b.getStatus() == Battery.Status.IN_USE)
                .toList();

        // Tạo 10 xe với pin IN_USE
        for (int i = 0; i < 10; i++) {
            User driver = drivers.get(i);
            Battery currentBattery = inUseBatteries.get(i);
            BatteryType batteryType = currentBattery.getBatteryType();

            Vehicle vehicle = createVehicle(
                    "VFMOTO2024001" + (1000 + i),
                    "51" + (char)('A' + (i / 10)) + (char)('A' + (i % 10)) + "" + (10000 + i),
                    getVehicleModel(batteryType, i),
                    driver,
                    batteryType,
                    currentBattery
            );

            vehicles.add(vehicle);
        }

        return vehicles;
    }

    private String getVehicleModel(BatteryType batteryType, int index) {
        String[] models48V = {"VinFast Klara A1", "Yadea Xmen Neo", "Pega Angel", "Dibao M5"};
        String[] models60V = {"VinFast Impes", "Yadea G5", "Pega NewTech", "Dibao X7"};
        String[] models72V = {"VinFast Evo", "Yadea T5", "Pega Heavy", "Dibao Truck"};

        double voltage = batteryType.getVoltage();
        if (voltage == 48.0) {
            return models48V[index % models48V.length];
        } else if (voltage == 60.0) {
            return models60V[index % models60V.length];
        } else {
            return models72V[index % models72V.length];
        }
    }

    private Vehicle createVehicle(String vin, String plateNumber, String model, User driver, BatteryType batteryType, Battery currentBattery) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vin);
        vehicle.setPlateNumber(plateNumber);
        vehicle.setModel(model);
        vehicle.setDriver(driver);
        vehicle.setBatteryType(batteryType);
        vehicle.setCurrentBattery(currentBattery); // GẮN PIN VÀO XE

        return vehicle;
    }

    // QUAN TRỌNG: Update battery status sau khi gắn vào xe
    private void updateBatteriesAfterVehicleAssignment(List<Vehicle> vehicles) {
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getCurrentBattery() != null) {
                Battery battery = vehicle.getCurrentBattery();
                battery.setStatus(Battery.Status.IN_USE);
                battery.setCurrentStation(null); // Pin trên xe, không ở trạm
                batteryRepository.save(battery);
            }
        }
    }

    private List<DriverSubscription> createDriverSubscriptions(List<User> users, List<ServicePackage> packages) {
        List<User> drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).toList();
        List<DriverSubscription> subscriptions = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            ServicePackage pkg = packages.get(i % packages.size());
            int remainingSwaps = pkg.getMaxSwaps() - (int)(Math.random() * 5);
            DriverSubscription.Status status = i == 2 ? DriverSubscription.Status.EXPIRED : DriverSubscription.Status.ACTIVE;

            subscriptions.add(createDriverSubscription(
                    drivers.get(i), pkg,
                    LocalDate.now().minusDays(5 + i),
                    LocalDate.now().plusDays(25 - i),
                    remainingSwaps, status
            ));
        }

        return subscriptions;
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
        List<StaffStationAssignment> assignments = new ArrayList<>();

        // Mỗi staff phụ trách 2-3 trạm
        assignments.add(createStaffStationAssignment(staff.get(0), stations.get(0), LocalDateTime.now().minusDays(30)));
        assignments.add(createStaffStationAssignment(staff.get(0), stations.get(1), LocalDateTime.now().minusDays(30)));
        assignments.add(createStaffStationAssignment(staff.get(0), stations.get(2), LocalDateTime.now().minusDays(30)));

        assignments.add(createStaffStationAssignment(staff.get(1), stations.get(3), LocalDateTime.now().minusDays(30)));
        assignments.add(createStaffStationAssignment(staff.get(1), stations.get(4), LocalDateTime.now().minusDays(30)));

        assignments.add(createStaffStationAssignment(staff.get(2), stations.get(5), LocalDateTime.now().minusDays(30)));
        assignments.add(createStaffStationAssignment(staff.get(2), stations.get(6), LocalDateTime.now().minusDays(30)));

        assignments.add(createStaffStationAssignment(staff.get(3), stations.get(7), LocalDateTime.now().minusDays(30)));
        assignments.add(createStaffStationAssignment(staff.get(3), stations.get(8), LocalDateTime.now().minusDays(30)));
        assignments.add(createStaffStationAssignment(staff.get(3), stations.get(9), LocalDateTime.now().minusDays(30)));

        return assignments;
    }

    private StaffStationAssignment createStaffStationAssignment(User staff, Station station, LocalDateTime assignedAt) {
        StaffStationAssignment assignment = new StaffStationAssignment();
        assignment.setStaff(staff);
        assignment.setStation(station);
        assignment.setAssignedAt(assignedAt);
        return assignment;
    }

    private List<StationInventory> createStationInventory(List<Battery> batteries) {
        List<StationInventory> inventory = new ArrayList<>();

        // CHỈ tạo inventory cho pin trong kho (currentStation = null, status != IN_USE)
        for (Battery battery : batteries) {
            // Bỏ qua pin ở trạm
            if (battery.getCurrentStation() != null) {
                continue;
            }

            // Bỏ qua pin đang được xe sử dụng
            if (battery.getStatus() == Battery.Status.IN_USE) {
                continue;
            }

            // Chỉ thêm pin trong kho (warehouse)
            StationInventory.Status status;
            switch (battery.getStatus()) {
                case AVAILABLE:
                    status = StationInventory.Status.AVAILABLE;
                    break;
                case MAINTENANCE:
                    status = StationInventory.Status.MAINTENANCE;
                    break;
                default:
                    continue; // Bỏ qua các status khác
            }

            inventory.add(createStationInventory(battery, status));
        }

        return inventory;
    }

    private StationInventory createStationInventory(Battery battery, StationInventory.Status status) {
        StationInventory inventory = new StationInventory();
        inventory.setBattery(battery);
        inventory.setStatus(status);
        inventory.setLastUpdate(LocalDateTime.now());
        return inventory;
    }

    private List<SwapTransaction> createSwapTransactions(List<User> users, List<Vehicle> vehicles, List<Station> stations, List<Battery> batteries) {
        List<User> drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).toList();
        List<User> staff = users.stream().filter(u -> u.getRole() == User.Role.STAFF).toList();
        LocalDateTime now = LocalDateTime.now();

        // Lấy các pin AVAILABLE từ trạm để làm pin mới
        List<Battery> availableBatteries = batteries.stream()
                .filter(b -> b.getStatus() == Battery.Status.AVAILABLE && b.getCurrentStation() != null)
                .toList();

        return List.of(
                createSwapTransaction(drivers.get(0), vehicles.get(0), stations.get(0), staff.get(0),
                        availableBatteries.get(0), vehicles.get(0).getCurrentBattery(),
                        now.minusHours(24), now.minusHours(24).plusMinutes(3),
                        BigDecimal.ZERO, SwapTransaction.Status.COMPLETED),

                createSwapTransaction(drivers.get(1), vehicles.get(1), stations.get(1), staff.get(1),
                        availableBatteries.get(1), vehicles.get(1).getCurrentBattery(),
                        now.minusHours(48), now.minusHours(48).plusMinutes(2),
                        BigDecimal.ZERO, SwapTransaction.Status.COMPLETED)
        );
    }

    private SwapTransaction createSwapTransaction(User driver, Vehicle vehicle, Station station, User staff,
                                                  Battery newBattery, Battery oldBattery, LocalDateTime start, LocalDateTime end,
                                                  BigDecimal cost, SwapTransaction.Status status) {
        SwapTransaction transaction = new SwapTransaction();
        transaction.setDriver(driver);
        transaction.setVehicle(vehicle);
        transaction.setStation(station);
        transaction.setStaff(staff);
        transaction.setSwapOutBattery(newBattery);  // Pin mới từ trạm
        transaction.setSwapInBattery(oldBattery);   // Pin cũ từ xe
        transaction.setStartTime(start);
        transaction.setEndTime(end);
        transaction.setCost(cost);
        transaction.setStatus(status);

        // Save snapshot
        if (newBattery != null) {
            transaction.setSwapOutBatteryModel(newBattery.getModel());
            transaction.setSwapOutBatteryChargeLevel(newBattery.getChargeLevel());
            transaction.setSwapOutBatteryHealth(newBattery.getStateOfHealth());
        }

        if (oldBattery != null) {
            transaction.setSwapInBatteryModel(oldBattery.getModel());
            transaction.setSwapInBatteryChargeLevel(oldBattery.getChargeLevel());
            transaction.setSwapInBatteryHealth(oldBattery.getStateOfHealth());
        }

        return transaction;
    }

    private List<Payment> createPayments(List<DriverSubscription> subscriptions) {
        List<Payment> payments = new ArrayList<>();

        for (int i = 0; i < subscriptions.size(); i++) {
            DriverSubscription subscription = subscriptions.get(i);
            payments.add(createPayment(
                    subscription,
                    subscription.getServicePackage().getPrice(),
                    "MOMO",
                    LocalDateTime.now().minusDays(5 + i),
                    Payment.Status.COMPLETED
            ));
        }

        return payments;
    }

    private Payment createPayment(DriverSubscription subscription, BigDecimal amount, String method, LocalDateTime date, Payment.Status status) {
        Payment payment = new Payment();
        payment.setSubscription(subscription);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setPaymentDate(date);
        payment.setStatus(status);
        return payment;
    }

    private List<SupportTicket> createSupportTickets(List<User> users, List<Station> stations) {
        List<User> drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).toList();

        return List.of(
                createSupportTicket(drivers.get(0), stations.get(0), "Pin yếu sau đổi", "Pin tôi vừa đổi chỉ chạy được 40km", SupportTicket.Status.OPEN),
                createSupportTicket(drivers.get(1), stations.get(1), "Trạm đổi pin bị kẹt", "Máy đổi pin tại trạm Quận 3 không nhả pin ra được", SupportTicket.Status.IN_PROGRESS)
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
                createTicketResponse(tickets.get(1), staff.get(0), "Chúng tôi đã tiếp nhận yêu cầu và sẽ cử kỹ thuật viên đến kiểm tra trong vòng 2 giờ.")
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