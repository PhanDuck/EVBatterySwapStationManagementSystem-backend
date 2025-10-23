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

        // 3. Tạo Stations (cần BatteryTypes)
        List<Station> stations = createStations(batteryTypes);
        stationRepository.saveAll(stations);
        log.info("Created {} stations", stations.size());

        // 4. Tạo Service Packages
        List<ServicePackage> packages = createServicePackages();
        servicePackageRepository.saveAll(packages);
        log.info("Created {} service packages", packages.size());

        // 5. Tạo Batteries (cần BatteryTypes)
        List<Battery> batteries = createBatteries(stations, batteryTypes);
        batteryRepository.saveAll(batteries);
        log.info("Created {} batteries", batteries.size());

        // 5. Tạo Vehicles (cần batteries để lấy batteryType)
        List<Vehicle> vehicles = createVehicles(users, batteries);
        vehicleRepository.saveAll(vehicles);
        log.info("Created {} vehicles", vehicles.size());

        // 6. Tạo Driver Subscriptions
        List<DriverSubscription> subscriptions = createDriverSubscriptions(users, packages);
        driverSubscriptionRepository.saveAll(subscriptions);
        log.info("Created {} subscriptions", subscriptions.size());

        // 7. Tạo Staff Station Assignments (Staff được assign vào stations)
        List<StaffStationAssignment> assignments = createStaffStationAssignments(users, stations);
        staffStationAssignmentRepository.saveAll(assignments);
        log.info("Created {} staff station assignments", assignments.size());

        // 8. Tạo Station Inventory
        List<StationInventory> inventory = createStationInventory(stations, batteries);
        stationInventoryRepository.saveAll(inventory);
        log.info("Created {} station inventory records", inventory.size());

        // 9. Tạo Bookings
        List<Booking> bookings = createBookings(users, vehicles, stations, batteries);
        bookingRepository.saveAll(bookings);
        log.info("Created {} bookings", bookings.size());
        
        // 9b. Reserve batteries cho PENDING bookings
        reserveBatteriesForPendingBookings(bookings, batteries);
        batteryRepository.saveAll(batteries);
        log.info("Reserved batteries for {} PENDING bookings", 
            bookings.stream().filter(b -> b.getStatus() == Booking.Status.PENDING).count());

        // 10. Tạo Swap Transactions
        List<SwapTransaction> transactions = createSwapTransactions(users, vehicles, stations, batteries);
        swapTransactionRepository.saveAll(transactions);
        log.info("Created {} swap transactions", transactions.size());

        // 11. Tạo Payments
        List<Payment> payments = createPayments(transactions, subscriptions);
        paymentRepository.saveAll(payments);
        log.info("Created {} payments", payments.size());

        // 12. Tạo Support Tickets
        List<SupportTicket> tickets = createSupportTickets(users, stations);
        supportTicketRepository.saveAll(tickets);
        log.info("Created {} support tickets", tickets.size());

        // 13. Tạo Ticket Responses
        List<TicketResponse> responses = createTicketResponses(tickets, users);
        ticketResponseRepository.saveAll(responses);
        log.info("Created {} ticket responses", responses.size());
    }

    private List<User> createUsers() {
        // Sử dụng password encoder thực tế để encode "password123"
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
            
            // Driver users
            createUser("Nguyễn Văn An", "team89a6@gmail.com", "0903000001", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
            createUser("Trần Thị Bình", "driver2@gmail.com", "0903000002", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
            createUser("Lê Văn Cường", "driver3@gmail.com", "0903000003", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
            createUser("Phạm Thị Dung", "driver4@gmail.com", "0903000004", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
            createUser("Hoàng Văn Em", "driver5@gmail.com", "0903000005", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE),
            createUser("Vũ Thị Phượng", "driver6@gmail.com", "0903000006", encodedPassword, User.Role.DRIVER, User.Status.INACTIVE),
            createUser("Lê Phước Trí", "lptri22051@gmail.com", "0774560933", encodedPassword, User.Role.DRIVER, User.Status.ACTIVE)
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
        // CHI TAO TRAM O TP.HCM
        return List.of(
            createStation("Trạm Đổi Pin Quận 1", "123 Đường Nguyễn Huệ, Quận 1, TP.HCM", "TP.HCM", "Quận 1", 20, "0901234567", 10.7769, 106.7009, Station.Status.ACTIVE, batteryTypes.get(0)),
            createStation("Trạm Đổi Pin Quận 3", "456 Đường Lê Văn Sỹ, Quận 3, TP.HCM", "TP.HCM", "Quận 3", 20, "0902345678", 10.7867, 106.6837, Station.Status.ACTIVE, batteryTypes.get(1)),
            createStation("Trạm Đổi Pin Quận 7", "789 Đường Nguyễn Thị Thập, Quận 7, TP.HCM", "TP.HCM", "Quận 7", 20, "0903456789", 10.7307, 106.7218, Station.Status.ACTIVE, batteryTypes.get(2)),
            createStation("Trạm Đổi Pin Bình Thạnh", "101 Đường Xô Viết Nghệ Tĩnh, Quận Bình Thạnh, TP.HCM", "TP.HCM", "Quận Bình Thạnh", 20, "0904567890", 10.8015, 106.7181, Station.Status.ACTIVE, batteryTypes.get(0)),
            createStation("Trạm Đổi Pin Thủ Đức", "202 Đường Võ Văn Ngân, TP Thủ Đức, TP.HCM", "TP.HCM", "TP Thủ Đức", 20, "0905678901", 10.8494, 106.7719, Station.Status.ACTIVE, batteryTypes.get(1)),
            createStation("Trạm Đổi Pin Gò Vấp", "45 Đường Quang Trung, Gò Vấp, TP.HCM", "TP.HCM", "Gò Vấp", 20, "0906789012", 10.8376, 106.6731, Station.Status.ACTIVE, batteryTypes.get(2)),
            createStation("Trạm Đổi Pin Tân Bình", "88 Đường Cộng Hòa, Tân Bình, TP.HCM", "TP.HCM", "Tân Bình", 20, "0907890123", 10.7992, 106.6530, Station.Status.ACTIVE, batteryTypes.get(0)),
            createStation("Trạm Đổi Pin Phú Nhuận", "156 Đường Phan Đăng Lưu, Phú Nhuận, TP.HCM", "TP.HCM", "Phú Nhuận", 20, "0908901234", 10.7980, 106.6831, Station.Status.ACTIVE, batteryTypes.get(1))
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
        List<Battery> batteries = new java.util.ArrayList<>();
        
        // Dùng BatteryTypes đã được save vào DB
        BatteryType type48V = batteryTypes.get(0);  // 48V-20Ah
        BatteryType type60V = batteryTypes.get(1);  // 60V-30Ah
        BatteryType type72V = batteryTypes.get(2);  // 72V-40Ah
        
        // ==================================================
        // PIN O TRAM (currentStation != null)
        // ==================================================
        
        // Trạm 1 (Quận 1) - 48V - 14 pin (còn 6 chỗ trống để nhận pin)
        for (int i = 0; i < 14; i++) {
            Battery.Status status;
            BigDecimal health;
            BigDecimal chargeLevel;
            
            // Pin 0-1: MAINTENANCE (health < 70%)
            if (i < 2) {
                status = Battery.Status.MAINTENANCE;
                health = new BigDecimal(55 + (Math.random() * 15)); // 55-70%
                chargeLevel = BigDecimal.valueOf(30 + (Math.random() * 50)); // 30-80%
            }
            // Pin 2-10: AVAILABLE (health >= 85%)
            else if (i < 11) {
                status = Battery.Status.AVAILABLE;
                health = new BigDecimal(85 + (Math.random() * 15)); // 85-100%
                chargeLevel = BigDecimal.valueOf(100.0);
            }
            // Pin 11-13: CHARGING (health >= 85%)
            else {
                status = Battery.Status.CHARGING;
                health = new BigDecimal(85 + (Math.random() * 15)); // 85-100%
                chargeLevel = BigDecimal.valueOf(20 + (Math.random() * 70)); // 20-90%
            }
            
            batteries.add(createBatteryAtStation("VinFast 48V-20Ah", new BigDecimal("1.44"), health, chargeLevel, status, stations.get(0), type48V));
        }
        
        // Trạm 2 (Quận 3) - 60V - 14 pin
        for (int i = 0; i < 14; i++) {
            Battery.Status status;
            BigDecimal health;
            BigDecimal chargeLevel;
            
            if (i < 1) {
                status = Battery.Status.MAINTENANCE;
                health = new BigDecimal(55 + (Math.random() * 15)); // 55-70%
                chargeLevel = BigDecimal.valueOf(30 + (Math.random() * 50));
            } else if (i < 11) {
                status = Battery.Status.AVAILABLE;
                health = new BigDecimal(85 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(100.0);
            } else {
                status = Battery.Status.CHARGING;
                health = new BigDecimal(85 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(20 + (Math.random() * 70));
            }
            
            batteries.add(createBatteryAtStation("Yadea 60V-30Ah", new BigDecimal("1.92"), health, chargeLevel, status, stations.get(1), type60V));
        }
        
        // Trạm 3 (Quận 7) - 72V - 14 pin
        for (int i = 0; i < 14; i++) {
            Battery.Status status;
            BigDecimal health;
            BigDecimal chargeLevel;
            
            if (i < 3) {
                status = Battery.Status.MAINTENANCE;
                health = new BigDecimal(55 + (Math.random() * 15)); // 55-70%
                chargeLevel = BigDecimal.valueOf(30 + (Math.random() * 50));
            } else if (i < 10) {
                status = Battery.Status.AVAILABLE;
                health = new BigDecimal(85 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(100.0);
            } else {
                status = Battery.Status.CHARGING;
                health = new BigDecimal(85 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(20 + (Math.random() * 70));
            }
            
            batteries.add(createBatteryAtStation("Pega 72V-40Ah", new BigDecimal("2.02"), health, chargeLevel, status, stations.get(2), type72V));
        }
        
        // Trạm 4 (Bình Thạnh) - 48V - 14 pin
        for (int i = 0; i < 14; i++) {
            Battery.Status status;
            BigDecimal health;
            BigDecimal chargeLevel;
            
            if (i < 2) {
                status = Battery.Status.MAINTENANCE;
                health = new BigDecimal(55 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(30 + (Math.random() * 50));
            } else if (i < 11) {
                status = Battery.Status.AVAILABLE;
                health = new BigDecimal(85 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(100.0);
            } else {
                status = Battery.Status.CHARGING;
                health = new BigDecimal(85 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(20 + (Math.random() * 70));
            }
            
            batteries.add(createBatteryAtStation("Dibao 48V-20Ah", new BigDecimal("2.10"), health, chargeLevel, status, stations.get(3), type48V));
        }
        
        // Trạm 5 (Thủ Đức) - 60V - 14 pin
        for (int i = 0; i < 14; i++) {
            Battery.Status status = i < 11 ? Battery.Status.AVAILABLE : Battery.Status.CHARGING;
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15)); // Trạm này không có pin bảo trì
            BigDecimal chargeLevel = status == Battery.Status.AVAILABLE ? 
                BigDecimal.valueOf(100.0) : BigDecimal.valueOf(20 + (Math.random() * 70));
            batteries.add(createBatteryAtStation("VinFast 60V-30Ah", new BigDecimal("2.16"), health, chargeLevel, status, stations.get(4), type60V));
        }
        
        // Trạm 6 (Gò Vấp) - 72V - 14 pin
        for (int i = 0; i < 14; i++) {
            Battery.Status status;
            BigDecimal health;
            BigDecimal chargeLevel;
            
            if (i < 1) {
                status = Battery.Status.MAINTENANCE;
                health = new BigDecimal(55 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(30 + (Math.random() * 50));
            } else if (i < 10) {
                status = Battery.Status.AVAILABLE;
                health = new BigDecimal(85 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(100.0);
            } else {
                status = Battery.Status.CHARGING;
                health = new BigDecimal(85 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(20 + (Math.random() * 70));
            }
            
            batteries.add(createBatteryAtStation("VinFast 72V-40Ah", new BigDecimal("1.68"), health, chargeLevel, status, stations.get(5), type72V));
        }
        
        // Trạm 7 (Tân Bình) - 48V - 14 pin
        for (int i = 0; i < 14; i++) {
            Battery.Status status = i < 10 ? Battery.Status.AVAILABLE : Battery.Status.CHARGING;
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15)); // Trạm này không có pin bảo trì
            BigDecimal chargeLevel = status == Battery.Status.AVAILABLE ? 
                BigDecimal.valueOf(100.0) : BigDecimal.valueOf(20 + (Math.random() * 70));
            batteries.add(createBatteryAtStation("Yadea 48V-20Ah", new BigDecimal("2.52"), health, chargeLevel, status, stations.get(6), type48V));
        }
        
        // Trạm 8 (Phú Nhuận) - 60V - 14 pin
        for (int i = 0; i < 14; i++) {
            Battery.Status status;
            BigDecimal health;
            BigDecimal chargeLevel;
            
            if (i < 2) {
                status = Battery.Status.MAINTENANCE;
                health = new BigDecimal(55 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(30 + (Math.random() * 50));
            } else if (i < 10) {
                status = Battery.Status.AVAILABLE;
                health = new BigDecimal(85 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(100.0);
            } else {
                status = Battery.Status.CHARGING;
                health = new BigDecimal(85 + (Math.random() * 15));
                chargeLevel = BigDecimal.valueOf(20 + (Math.random() * 70));
            }
            
            batteries.add(createBatteryAtStation("Pega 60V-30Ah", new BigDecimal("1.80"), health, chargeLevel, status, stations.get(7), type60V));
        }
        
        // ==================================================
        // PIN O KHO (currentStation = null)
        // ==================================================
        
        // 1. PIN BINH THUONG O KHO (status = AVAILABLE, health >= 90%)
        // Đây là pin dự trữ tốt để thay thế pin bảo trì
        String[] brands = {"VinFast", "Yadea", "Pega", "Dibao"};
        
        // 5 pin 48V-20Ah trong kho
        for (int i = 0; i < 5; i++) {
            String model = brands[i % brands.length] + " 48V-20Ah";
            BigDecimal health = new BigDecimal(90 + (Math.random() * 10)); // 90-100%
            batteries.add(createUnassignedBattery(model, new BigDecimal("1.44"), health, Battery.Status.AVAILABLE, type48V));
        }
        
        // 5 pin 60V-30Ah trong kho
        for (int i = 0; i < 5; i++) {
            String model = brands[i % brands.length] + " 60V-30Ah";
            BigDecimal health = new BigDecimal(90 + (Math.random() * 10)); // 90-100%
            batteries.add(createUnassignedBattery(model, new BigDecimal("1.92"), health, Battery.Status.AVAILABLE, type60V));
        }
        
        // 5 pin 72V-40Ah trong kho
        for (int i = 0; i < 5; i++) {
            String model = brands[i % brands.length] + " 72V-40Ah";
            BigDecimal health = new BigDecimal(90 + (Math.random() * 10)); // 90-100%
            batteries.add(createUnassignedBattery(model, new BigDecimal("2.02"), health, Battery.Status.AVAILABLE, type72V));
        }
        
        // 2. PIN DANG BAO TRI O KHO (status = MAINTENANCE, health < 70%)
        // Đây là pin đã bị thay ra từ trạm, đang chờ sửa chữa/bảo dưỡng
        
        // 3 pin 48V đang bảo trì
        for (int i = 0; i < 3; i++) {
            String model = brands[i % brands.length] + " 48V-20Ah";
            BigDecimal health = new BigDecimal(50 + (Math.random() * 20)); // 50-70%
            batteries.add(createUnassignedBattery(model, new BigDecimal("1.44"), health, Battery.Status.MAINTENANCE, type48V));
        }
        
        // 3 pin 60V đang bảo trì
        for (int i = 0; i < 3; i++) {
            String model = brands[i % brands.length] + " 60V-30Ah";
            BigDecimal health = new BigDecimal(50 + (Math.random() * 20)); // 50-70%
            batteries.add(createUnassignedBattery(model, new BigDecimal("1.92"), health, Battery.Status.MAINTENANCE, type60V));
        }
        
        // 2 pin 72V đang bảo trì
        for (int i = 0; i < 2; i++) {
            String model = brands[i % brands.length] + " 72V-40Ah";
            BigDecimal health = new BigDecimal(50 + (Math.random() * 20)); // 50-70%
            batteries.add(createUnassignedBattery(model, new BigDecimal("2.02"), health, Battery.Status.MAINTENANCE, type72V));
        }
        
        // ==================================================
        // PIN DANG DUOC TAI XE MUON DI (currentStation = null, status = IN_USE)
        // ==================================================
        // Pin đang ở ngoài, không thuộc trạm nào, không trong kho
        
        // 3 pin 48V đang được dùng
        for (int i = 0; i < 3; i++) {
            String model = brands[i % brands.length] + " 48V-20Ah";
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15)); // 85-100%
            batteries.add(createUnassignedBattery(model, new BigDecimal("1.44"), health, Battery.Status.IN_USE, type48V));
        }
        
        // 2 pin 60V đang được dùng
        for (int i = 0; i < 2; i++) {
            String model = brands[i % brands.length] + " 60V-30Ah";
            BigDecimal health = new BigDecimal(85 + (Math.random() * 15)); // 85-100%
            batteries.add(createUnassignedBattery(model, new BigDecimal("1.92"), health, Battery.Status.IN_USE, type60V));
        }
        
        return batteries;
    }

    // ============================================
    // HELPER METHODS - TAO BATTERY
    // ============================================
    
    /**
     * TAO PIN THUOC TRAM (currentStation != null)
     * Pin này ở TRẠM, không ở kho, không ở vehicle
     */
    private Battery createBatteryAtStation(String model, BigDecimal capacity, BigDecimal stateOfHealth, 
                                          BigDecimal chargeLevel, Battery.Status status, 
                                          Station station, BatteryType batteryType) {
        Battery battery = new Battery();
        battery.setModel(model);
        battery.setCapacity(capacity);
        battery.setStateOfHealth(stateOfHealth);
        battery.setChargeLevel(chargeLevel);
        battery.setStatus(status);
        battery.setCurrentStation(station); // Pin at station
        battery.setBatteryType(batteryType);
        battery.setManufactureDate(LocalDate.now().minusMonths((long) (Math.random() * 12)));
        battery.setUsageCount((int) (Math.random() * 100));
        return battery;
    }

    /**
     * Create battery in warehouse or borrowed by driver (currentStation = null)
     * - If status = AVAILABLE/CHARGING/MAINTENANCE -> Battery in warehouse (will have StationInventory)
     * - If status = IN_USE -> Battery borrowed by driver (on vehicle)
     */
    private Battery createUnassignedBattery(String model, BigDecimal capacity, BigDecimal stateOfHealth, Battery.Status status, BatteryType batteryType) {
        Battery battery = new Battery();
        battery.setModel(model);
        battery.setCapacity(capacity);
        battery.setStateOfHealth(stateOfHealth);
        battery.setChargeLevel(BigDecimal.valueOf(100.0));
        battery.setStatus(status);
        battery.setCurrentStation(null); // Not at any station (warehouse or borrowed)
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
        
        // Lấy 5 pin cuối cùng (135-139) cho xe (sẽ trở thành IN_USE)
        // Các pin này chưa được assign vào trạm nào (currentStation = null)
        int baseIndex = batteries.size() - 5; // 140 - 5 = 135
        
        return List.of(
            createVehicle("VFMOTO2024001234", "59-A1 123.45", "VinFast Klara A2", drivers.get(0), type1, batteries.get(baseIndex)),
            createVehicle("VFMOTO2024001235", "51-F1 456.78", "VinFast Impes", drivers.get(1), type2, batteries.get(baseIndex + 1)),
            createVehicle("YADEA2024001236", "59-B2 789.01", "Yadea Xmen Neo", drivers.get(2), type1, batteries.get(baseIndex + 2)),
            createVehicle("PEGA2024001237", "51-G1 234.56", "Pega NewTech", drivers.get(3), type2, batteries.get(baseIndex + 3)),
            createVehicle("DIBAO2024001238", "59-C3 567.89", "Dibao Angelina", drivers.get(4), type1, batteries.get(baseIndex + 4))
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
        
        // Update battery status when mounted on vehicle
        if (currentBattery != null) {
            currentBattery.setStatus(Battery.Status.IN_USE);
            currentBattery.setCurrentStation(null); // Battery on vehicle, not at any station
            // IN_USE batteries are also not in StationInventory
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
            
            // Staff 3 (index 2) - Phụ trách 3 trạm còn lại ở TP.HCM
            createStaffStationAssignment(staff.get(2), stations.get(5), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Gò Vấp
            createStaffStationAssignment(staff.get(2), stations.get(6), LocalDateTime.of(2025, 10, 1, 8, 0)),  // Tân Bình
            createStaffStationAssignment(staff.get(2), stations.get(7), LocalDateTime.of(2025, 10, 1, 8, 0))   // Phú Nhuận
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
        
        // IMPORTANT RULE: 1 BATTERY IN ONLY 1 LOCATION
        // - Battery at STATION (currentStation != null) -> NOT in StationInventory
        // - Battery on VEHICLE (status = IN_USE) -> NOT in StationInventory
        // - Battery in WAREHOUSE (currentStation = null, status != IN_USE) -> IN StationInventory
        
        // StationInventory = Central Warehouse
        // Only create for batteries in warehouse (currentStation = NULL and status != IN_USE)
        for (Battery battery : batteries) {
            // Skip: Battery at station
            if (battery.getCurrentStation() != null) {
                continue; // Battery at station -> not in warehouse
            }
            
            // Skip: Battery on vehicle
            if (battery.getStatus() == Battery.Status.IN_USE) {
                continue; // Battery on vehicle -> not in warehouse
            }
            
            // Only include: Batteries in warehouse (currentStation = null, status != IN_USE)
            StationInventory.Status status;
            
            // Map Battery.Status -> StationInventory.Status
            switch (battery.getStatus()) {
                case AVAILABLE:
                    status = StationInventory.Status.AVAILABLE;
                    break;
                case CHARGING:
                    status = StationInventory.Status.AVAILABLE; // Battery charging in warehouse
                    break;
                case MAINTENANCE:
                    status = StationInventory.Status.MAINTENANCE;
                    break;
                case PENDING:
                case DAMAGED:
                    // PENDING/DAMAGED batteries should not be in warehouse
                    continue; // Skip
                default:
                    status = StationInventory.Status.AVAILABLE;
            }
            
            // Tạo inventory cho pin trong kho
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

    private List<Booking> createBookings(List<User> users, List<Vehicle> vehicles, List<Station> stations, List<Battery> batteries) {
        List<User> drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).toList();
        List<User> staff = users.stream().filter(u -> u.getRole() == User.Role.STAFF).toList();

        // Tạo booking 24/7 để thể hiện trạm hoạt động liên tục
        LocalDateTime now = LocalDateTime.now();
        
        return List.of(

            // PENDING bookings - Driver đã đặt, hệ thống đã reserve pin, chờ driver đến
            createBooking(drivers.get(1), vehicles.get(1), stations.get(1), now.plusHours(4), Booking.Status.PENDING, null),           
            createBooking(drivers.get(4), vehicles.get(4), stations.get(4), now.plusHours(8), Booking.Status.PENDING, null),
            
            // COMPLETED bookings - Đã hoàn thành swap
            createBooking(drivers.get(0), vehicles.get(0), stations.get(0), now.minusHours(24), Booking.Status.COMPLETED, staff.get(0)),
            createBooking(drivers.get(1), vehicles.get(1), stations.get(1), now.minusHours(48), Booking.Status.COMPLETED, staff.get(1)),
            
            // CANCELLED bookings - Driver hủy hoặc quá hạn
            createBooking(drivers.get(2), vehicles.get(2), stations.get(5), now.minusHours(12), Booking.Status.CANCELLED, null)
        );
    }
    
    /**
     * Reserve batteries cho các booking PENDING
     * - Tìm battery AVAILABLE tại station của booking
     * - Kiểm tra: SOH >= 70%, chargeLevel >= 80%, đúng loại pin
     * - Set battery.status = PENDING
     * - Set battery.reservedForBooking = booking
     * - Set battery.reservationExpiry = bookingTime + 3 hours
     */
    private void reserveBatteriesForPendingBookings(List<Booking> bookings, List<Battery> batteries) {
        for (Booking booking : bookings) {
            if (booking.getStatus() != Booking.Status.PENDING) continue;
            
            // Tìm battery AVAILABLE tại station này với đúng loại pin
            BatteryType requiredType = booking.getVehicle().getBatteryType();
            Battery availableBattery = batteries.stream()
                .filter(b -> b.getStatus() == Battery.Status.AVAILABLE)
                .filter(b -> b.getCurrentStation() != null && b.getCurrentStation().getId().equals(booking.getStation().getId()))
                .filter(b -> b.getBatteryType().getId().equals(requiredType.getId()))
                .filter(b -> b.getStateOfHealth().compareTo(new BigDecimal("70")) >= 0)  // Must have SOH >= 70%
                .filter(b -> b.getChargeLevel().compareTo(new BigDecimal("80")) >= 0)     // Pin phải đủ sạc >= 80%
                .findFirst()
                .orElse(null);
            
            if (availableBattery != null) {
                // Reserve battery cho booking này
                availableBattery.setStatus(Battery.Status.PENDING);
                availableBattery.setReservedForBooking(booking);
                availableBattery.setReservationExpiry(booking.getBookingTime().plusHours(3));  // Hết hạn sau 3 giờ
                
                log.info("Reserved battery {} (Type: {}, SOH: {}%, Charge: {}%) for booking at {} - Expiry: {}", 
                    availableBattery.getId(), 
                    availableBattery.getBatteryType().getName(),
                    availableBattery.getStateOfHealth(),
                    availableBattery.getChargeLevel(),
                    booking.getStation().getName(),
                    availableBattery.getReservationExpiry());
            } else {
                log.warn("No available battery (SOH >= 70%, Charge >= 80%) found for PENDING booking at {}", 
                    booking.getStation().getName());
            }
        }
    }

    private Booking createBooking(User driver, Vehicle vehicle, Station station, LocalDateTime bookingTime, Booking.Status status, User confirmedBy) {
        Booking booking = new Booking();
        booking.setDriver(driver);
        booking.setVehicle(vehicle);
        booking.setStation(station);
        booking.setBookingTime(bookingTime);
        
        // Tạo confirmation code cho tất cả bookings (trừ CANCELLED)
        booking.setConfirmationCode(status != Booking.Status.CANCELLED ? generateConfirmationCode() : null);
        
        booking.setStatus(status);
        booking.setConfirmedBy(confirmedBy);  // Chỉ CONFIRMED bookings mới có confirmedBy
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
        LocalDateTime now = LocalDateTime.now();
        
        return List.of(
            // COMPLETED - Successful swap (has subscription, FREE)
            createSwapTransaction(drivers.get(0), vehicles.get(0), stations.get(0), staff.get(0), 
                batteries.get(135), batteries.get(0), now.minusHours(24), 
                now.minusHours(24).plusMinutes(3), BigDecimal.ZERO, SwapTransaction.Status.COMPLETED),
                
            createSwapTransaction(drivers.get(1), vehicles.get(1), stations.get(1), staff.get(1), 
                batteries.get(136), batteries.get(14), now.minusHours(48), 
                now.minusHours(48).plusMinutes(2), BigDecimal.ZERO, SwapTransaction.Status.COMPLETED),
                
            // COMPLETED - First time swap (no old battery)
            createSwapTransaction(drivers.get(2), vehicles.get(2), stations.get(2), staff.get(0), 
                null, batteries.get(28), now.minusHours(72), 
                now.minusHours(72).plusMinutes(2), BigDecimal.ZERO, SwapTransaction.Status.COMPLETED),
                
            // COMPLETED - Multiple swaps in one day
            createSwapTransaction(drivers.get(3), vehicles.get(3), stations.get(3), staff.get(1), 
                batteries.get(137), batteries.get(42), now.minusHours(12), 
                now.minusHours(12).plusMinutes(3), BigDecimal.ZERO, SwapTransaction.Status.COMPLETED),
                
            createSwapTransaction(drivers.get(3), vehicles.get(3), stations.get(5), staff.get(0), 
                batteries.get(42), batteries.get(70), now.minusHours(6), 
                now.minusHours(6).plusMinutes(2), BigDecimal.ZERO, SwapTransaction.Status.COMPLETED),
                
            // IN_PROGRESS - Swap in progress
            createSwapTransaction(drivers.get(4), vehicles.get(4), stations.get(4), staff.get(1), 
                batteries.get(138), batteries.get(56), now.minusMinutes(5), 
                null, BigDecimal.ZERO, SwapTransaction.Status.IN_PROGRESS),
                
            // CANCELLED - Cancelled (battery unsuitable, driver didn't show up, etc.)
            createSwapTransaction(drivers.get(2), vehicles.get(2), stations.get(6), staff.get(0), 
                null, null, now.minusHours(3), 
                now.minusHours(3).plusMinutes(1), BigDecimal.ZERO, SwapTransaction.Status.CANCELLED)
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
        
        // Save snapshot of battery info at swap time
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
        
        // Update vehicle current battery after swap
        // If status = COMPLETED, mount new battery (swapIn) to vehicle
        // Logic: swapOut = old battery removed, swapIn = new battery mounted
        if (status == SwapTransaction.Status.COMPLETED && swapIn != null) {
            vehicle.setCurrentBattery(swapIn);  // Must be new battery (swapIn), not old battery (swapOut)
        }
        
        return transaction;
    }

    private List<Payment> createPayments(List<SwapTransaction> transactions, List<DriverSubscription> subscriptions) {
        // Payment only for SUBSCRIPTION, no payment for swap transaction
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