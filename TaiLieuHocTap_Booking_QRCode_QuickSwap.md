# TÀI LIỆU HỌC TẬP: BOOKING, QR CODE, QUICK SWAP

## Tổng Quan Dự Án
Hệ thống quản lý trạm đổi pin xe điện (EV Battery Swap Station Management System) - Backend API sử dụng Spring Boot.

---

## 1. CÁC THỨ VIỆN SỬ DỤNG

### 1.1. Thư Viện Core (pom.xml)

#### **Spring Boot Framework**
```xml
<!-- Spring Boot Starter Parent - Version 3.5.6 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.6</version>
</parent>
```

**Giải thích**:
- `<parent>`: Kế thừa configuration từ Spring Boot Parent
- `groupId`: Tổ chức sở hữu thư viện (org.springframework.boot)
- `artifactId`: Tên thư viện (spring-boot-starter-parent)
- `version`: Phiên bản sử dụng (3.5.6)

**Mục đích**: 
- Framework chính để xây dựng ứng dụng Spring Boot
- Quản lý version của tất cả dependencies
- Cung cấp default configuration

**Tính năng cung cấp**: 
- Dependency injection (IoC Container)
- REST API framework
- Transaction management
- Auto-configuration

#### **Spring Data JPA**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**Giải thích**:
- JPA (Java Persistence API): Chuẩn để làm việc với database
- Hibernate: Implementation mặc định của JPA
- Spring Data: Wrapper giúp code ngắn gọn hơn

**Mục đích**: 
- Quản lý database operations
- ORM (Object-Relational Mapping): Chuyển đổi giữa Java object và database table
- Tự động generate SQL queries

**Cách sử dụng trong code**:
```java
// 1. Định nghĩa Repository Interface
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // Spring tự động implement method này
    Optional<Booking> findByConfirmationCode(String code);
}

// 2. Sử dụng trong Service
@Autowired
private BookingRepository bookingRepository;

// Lưu vào database
Booking savedBooking = bookingRepository.save(booking);

// Tìm theo ID
Booking booking = bookingRepository.findById(id)
    .orElseThrow(() -> new NotFoundException("Không tìm thấy"));

// Tìm theo custom method
Optional<Booking> booking = bookingRepository.findByConfirmationCode("ABC123");
```

**Annotation quan trọng**: 
- `@Transactional`: Đảm bảo transaction ACID
- `@Repository`: Đánh dấu lớp truy cập database
- `@Entity`: Đánh dấu class map với table

**Sử dụng trong**: BookingService, QuickSwapService, tất cả Service layers

#### **Spring Security**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```
- **Mục đích**: Xác thực người dùng, phân quyền
- **Sử dụng trong**: AuthenticationService
- **Annotation**: `@PreAuthorize`, `@SecurityRequirement`

#### **Spring Validation**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
- **Mục đích**: Validate dữ liệu input
- **Annotation**: `@Valid`, `@NotNull`, `@Email`

---

### 1.2. Thư Viện QR Code Generation ⭐

#### **ZXing (Zebra Crossing) - Google**
```xml
<!-- QR Code Core -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.4</version>
</dependency>

<!-- QR Code Image Writer -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.4</version>
</dependency>
```

**Giải thích chi tiết**:
- **ZXing** = "Zebra Crossing" (vạch kẻ đường ngựa vằn)
- Open-source library của Google
- Hỗ trợ nhiều loại barcode (QR Code, EAN, UPC, Code 128...)
- Version 3.5.4 là stable version

**Chức năng từng artifact**:

**1. `core` (com.google.zxing:core)**:
- Chứa logic cốt lõi để encode/decode barcode
- Các class chính:
  - `QRCodeWriter`: Class để tạo QR code
  - `BarcodeFormat`: Enum định nghĩa các loại barcode
  - `BitMatrix`: Ma trận 2D chứa điểm ảnh QR code
  - `EncodeHintType`: Config khi encode (error correction level, charset...)

**2. `javase` (com.google.zxing:javase)**:
- Extension cho Java Standard Edition
- Chứa utilities để làm việc với images
- Các class chính:
  - `MatrixToImageWriter`: Chuyển BitMatrix thành BufferedImage hoặc file
  - `MatrixToImageConfig`: Config màu sắc, format ảnh

**Cách sử dụng step-by-step**:

```java
// BƯỚC 1: Import các class cần thiết
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

// BƯỚC 2: Chuẩn bị dữ liệu cần encode
String data = "http://103.200.20.190:5173/quick-swap?stationId=1";

// BƯỚC 3: Tạo QRCodeWriter instance
QRCodeWriter qrCodeWriter = new QRCodeWriter();

// BƯỚC 4: Encode dữ liệu thành BitMatrix
BitMatrix bitMatrix = qrCodeWriter.encode(
    data,                    // Dữ liệu cần encode
    BarcodeFormat.QR_CODE,   // Loại barcode (QR_CODE)
    300,                     // Chiều rộng (pixels)
    300                      // Chiều cao (pixels)
);

// BƯỚC 5: Chuyển BitMatrix thành ảnh PNG
ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
MatrixToImageWriter.writeToStream(
    bitMatrix,               // Ma trận QR
    "PNG",                   // Format ảnh (PNG, JPG, GIF...)
    outputStream             // Nơi lưu output
);

// BƯỚC 6: Lấy byte array để trả về API
byte[] qrCodeImage = outputStream.toByteArray();
```

**Sử dụng trong**: `QRCodeService.java`

**Ưu điểm ZXing**:
- ✅ Miễn phí, open-source (Apache License 2.0)
- ✅ Được Google maintain
- ✅ Hỗ trợ đa dạng barcode formats
- ✅ Performance tốt
- ✅ Dễ tích hợp vào Spring Boot

---

### 1.3. Thư Viện Email

#### **Spring Mail**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
    <version>4.0.0-M3</version>
</dependency>
```

#### **Thymeleaf Template Engine**
```xml
<dependency>
    <groupId>org.thymeleaf</groupId>
    <artifactId>thymeleaf</artifactId>
    <version>3.1.3.RELEASE</version>
</dependency>
```
- **Mục đích**: Render email HTML từ template
- **Template**: `booking-confirmed.html`, `swap-success-email.html`

---

### 1.4. Thư Viện Khác

#### **Lombok**
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```
- **Annotation**: `@Data`, `@RequiredArgsConstructor`, `@Slf4j`
- **Giảm boilerplate code**: Tự động tạo getter/setter/constructor

#### **JWT (JSON Web Token)**
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
```
- **Mục đích**: Xác thực API bằng token

#### **Swagger/OpenAPI**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
</dependency>
```
- **Mục đích**: Tự động tạo API documentation

---

## 2. PHẦN BOOKING SERVICE

### 2.1. Annotation và Dependency Injection

```java
@Service                    // Đánh dấu đây là Service layer
@RequiredArgsConstructor    // Lombok tự tạo constructor với final fields
public class BookingService {
    
    @Autowired
    private final BookingRepository bookingRepository;
    
    @Autowired
    private final VehicleRepository vehicleRepository;
    
    @Autowired
    private final BatteryRepository batteryRepository;
    
    @Autowired
    private EmailService emailService;
}
```

**Giải thích chi tiết từng annotation**:

#### **1. @Service**
```java
@Service
public class BookingService { }
```
- **Mục đích**: Đánh dấu class này là Service layer trong kiến trúc 3-layer (Controller → Service → Repository)
- **Cách hoạt động**: 
  - Spring scan và tạo bean của class này khi application start
  - Bean được quản lý bởi IoC Container
  - Có thể inject vào các class khác
- **Stereotype annotation**: `@Service` là một loại `@Component` chuyên dụng

#### **2. @RequiredArgsConstructor** (Lombok)
```java
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
}
```

**Lombok tự động generate constructor**:
```java
// Code Lombok tự động tạo ra (không cần viết):
public BookingService(
    BookingRepository bookingRepository,
    VehicleRepository vehicleRepository
) {
    this.bookingRepository = bookingRepository;
    this.vehicleRepository = vehicleRepository;
}
```

**Lợi ích**:
- Giảm code boilerplate
- Constructor injection (best practice trong Spring)
- Chỉ tạo constructor cho `final` fields

#### **3. @Autowired**
```java
@Autowired
private final BookingRepository bookingRepository;
```

**Cách hoạt động Dependency Injection**:
```
1. Spring khởi động application
2. Tìm tất cả @Service, @Repository, @Component
3. Tạo instances (beans)
4. Khi tạo BookingService:
   - Thấy constructor cần BookingRepository
   - Tìm bean BookingRepository đã tạo
   - Inject vào constructor
   - Hoàn thành khởi tạo BookingService
```

**So sánh 3 cách Dependency Injection**:

```java
// 1. Field Injection (KHÔNG NÊN DÙNG)
@Autowired
private BookingRepository bookingRepository;

// 2. Setter Injection
private BookingRepository bookingRepository;
@Autowired
public void setBookingRepository(BookingRepository repo) {
    this.bookingRepository = repo;
}

// 3. Constructor Injection (BEST PRACTICE) ⭐
private final BookingRepository bookingRepository;
@Autowired // Có thể bỏ @Autowired từ Spring 4.3+
public BookingService(BookingRepository bookingRepository) {
    this.bookingRepository = bookingRepository;
}
```

**Tại sao Constructor Injection tốt nhất?**
- ✅ Immutable (dùng `final`)
- ✅ Bắt buộc có dependency (không null)
- ✅ Dễ test (có thể tạo instance mà không cần Spring)
- ✅ Tránh circular dependency

#### **4. Kết hợp @RequiredArgsConstructor + @Autowired**
```java
@Service
@RequiredArgsConstructor  // Lombok tạo constructor
public class BookingService {
    
    // final → Lombok tạo constructor parameter
    @Autowired
    private final BookingRepository bookingRepository;
    
    @Autowired
    private final VehicleRepository vehicleRepository;
    
    // Không final → Không có trong constructor
    @Autowired
    private EmailService emailService;  // Setter injection
}
```

**Code tương đương không dùng Lombok**:
```java
@Service
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private EmailService emailService;
    
    // Constructor injection cho final fields
    @Autowired
    public BookingService(
        BookingRepository bookingRepository,
        VehicleRepository vehicleRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
    }
    
    // Setter injection cho non-final field
    @Autowired
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
}
```

---

### 2.2. Transaction Management

```java
@Transactional
public Booking createBooking(BookingRequest request) {
    // Logic tạo booking
}
```

**Giải thích chi tiết @Transactional**:

#### **1. Transaction là gì?**
Transaction là một nhóm các thao tác database phải thực hiện theo nguyên tắc **ACID**:
- **A**tomicity (Tính nguyên tử): Tất cả hoặc không có gì
- **C**onsistency (Tính nhất quán): Dữ liệu luôn hợp lệ
- **I**solation (Tính cô lập): Các transaction không ảnh hưởng lẫn nhau
- **D**urability (Tính bền vững): Sau khi commit, dữ liệu không mất

#### **2. Cách @Transactional hoạt động**

```java
@Transactional
public Booking createBooking(BookingRequest request) {
    // BƯỚC 1: Reserve pin
    battery.setStatus(Battery.Status.PENDING);
    batteryRepository.save(battery);  // SQL UPDATE
    
    // BƯỚC 2: Tạo booking
    Booking booking = new Booking();
    bookingRepository.save(booking);  // SQL INSERT
    
    // BƯỚC 3: Trừ lượt swap
    subscription.setRemainingSwaps(remaining - 1);
    subscriptionRepository.save(subscription);  // SQL UPDATE
    
    // BƯỚC 4: Gửi email
    emailService.sendEmail(booking);
    
    return booking;
}
```

**Luồng thực thi**:
```
1. Spring tạo PROXY bao quanh method createBooking()
2. PROXY bắt đầu transaction:
   - BEGIN TRANSACTION
3. Thực thi code trong method:
   - batteryRepository.save()      → Chưa commit
   - bookingRepository.save()      → Chưa commit  
   - subscriptionRepository.save() → Chưa commit
4. Nếu tất cả thành công:
   - COMMIT TRANSACTION
   - Tất cả thay đổi được lưu vào database
5. Nếu có exception ném ra:
   - ROLLBACK TRANSACTION
   - Tất cả thay đổi bị hủy bỏ
   - Database về trạng thái ban đầu
```

#### **3. Ví dụ thực tế**

**Trường hợp THÀNH CÔNG**:
```java
@Transactional
public Booking createBooking(BookingRequest request) {
    battery.setStatus(PENDING);
    batteryRepository.save(battery);     // ✅ Saved
    
    booking.setStatus(CONFIRMED);
    bookingRepository.save(booking);     // ✅ Saved
    
    subscription.setRemainingSwaps(9);
    subscriptionRepository.save(subscription); // ✅ Saved
    
    return booking; // → COMMIT: Tất cả changes được lưu
}
```

**Trường hợp CÓ LỖI**:
```java
@Transactional
public Booking createBooking(BookingRequest request) {
    battery.setStatus(PENDING);
    batteryRepository.save(battery);     // ✅ Executed (chưa commit)
    
    booking.setStatus(CONFIRMED);
    bookingRepository.save(booking);     // ✅ Executed (chưa commit)
    
    // ❌ LỖI XẢY RA
    throw new RuntimeException("Network error!");
    
    subscription.setRemainingSwaps(9);
    subscriptionRepository.save(subscription); // ⛔ Không chạy đến đây
    
    return booking; // ⛔ Không chạy đến đây
}
// → ROLLBACK: Tất cả changes bị hủy bỏ
// Database trở về trạng thái ban đầu
```

#### **4. Các tùy chọn @Transactional**

```java
// 1. Read-only transaction (tối ưu performance)
@Transactional(readOnly = true)
public List<Booking> getAllBookings() {
    return bookingRepository.findAll();
}

// 2. Rollback cho specific exception
@Transactional(rollbackFor = Exception.class)
public Booking createBooking() { }

// 3. Không rollback cho specific exception
@Transactional(noRollbackFor = IllegalArgumentException.class)
public Booking createBooking() { }

// 4. Set timeout (seconds)
@Transactional(timeout = 30)
public Booking createBooking() { }

// 5. Set isolation level
@Transactional(isolation = Isolation.READ_COMMITTED)
public Booking createBooking() { }

// 6. Set propagation (cách transaction lồng nhau)
@Transactional(propagation = Propagation.REQUIRED)
public Booking createBooking() { }
```

#### **5. Propagation Levels**

```java
// REQUIRED (default): Dùng transaction hiện tại, nếu không có thì tạo mới
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    methodB(); // methodB dùng chung transaction với methodA
}

// REQUIRES_NEW: Luôn tạo transaction mới
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void methodB() {
    // Transaction riêng, không ảnh hưởng methodA
}

// NESTED: Tạo nested transaction (savepoint)
@Transactional(propagation = Propagation.NESTED)
public void methodC() {
    // Nếu fail, chỉ rollback methodC, không ảnh hưởng parent
}
```

#### **6. Lưu ý quan trọng ⚠️**

**@Transactional chỉ hoạt động khi**:
```java
// ✅ ĐÚNG: Method được gọi từ BÊN NGOÀI class
@Service
public class BookingService {
    @Autowired
    private VehicleService vehicleService;
    
    public void createBooking() {
        vehicleService.saveVehicle(); // ✅ Transaction hoạt động
    }
}

@Service
public class VehicleService {
    @Transactional
    public void saveVehicle() { }
}

// ❌ SAI: Method được gọi từ BÊN TRONG class (self-invocation)
@Service
public class BookingService {
    
    public void createBooking() {
        this.saveBooking(); // ❌ Transaction KHÔNG hoạt động
    }
    
    @Transactional
    private void saveBooking() { }
}
```

**Nguyên nhân**: Spring dùng Proxy pattern, proxy chỉ intercept external calls.

#### **7. Tại sao quan trọng trong Booking?**

```java
@Transactional
public Booking createBooking(BookingRequest request) {
    // Nếu KHÔNG có @Transactional:
    
    battery.setStatus(PENDING);
    batteryRepository.save(battery);  // ✅ COMMIT ngay
    
    booking.setStatus(CONFIRMED);
    bookingRepository.save(booking);  // ✅ COMMIT ngay
    
    // ❌ Lỗi ở đây
    throw new RuntimeException("Error!");
    
    subscription.setRemainingSwaps(9);
    subscriptionRepository.save(subscription); // ⛔ Không chạy
    
    // KẾT QUẢ: Pin đã PENDING, Booking đã tạo, nhưng lượt swap chưa trừ
    // → DỮ LIỆU KHÔNG NHẤT QUÁN ❌
}
```

```java
@Transactional  // ✅ CÓ @Transactional
public Booking createBooking(BookingRequest request) {
    battery.setStatus(PENDING);
    batteryRepository.save(battery);  // Chưa commit
    
    booking.setStatus(CONFIRMED);
    bookingRepository.save(booking);  // Chưa commit
    
    // ❌ Lỗi ở đây
    throw new RuntimeException("Error!");
    
    subscription.setRemainingSwaps(9);
    subscriptionRepository.save(subscription); // Không chạy
    
    // KẾT QUẢ: ROLLBACK tất cả
    // → Pin về AVAILABLE, Booking không tạo, lượt swap không đổi
    // → DỮ LIỆU NHẤT QUÁN ✅
}
```

---

### 2.3. Luồng Tạo Booking

```java
@Transactional
public Booking createBooking(BookingRequest request) {
    // BƯỚC 1: Kiểm tra subscription
    DriverSubscription activeSubscription = driverSubscriptionRepository
            .findActiveSubscriptionByDriver(currentUser, LocalDate.now())
            .orElse(null);
    
    if (activeSubscription == null) {
        throw new AuthenticationException("Chưa có gói dịch vụ!");
    }
    
    if (activeSubscription.getRemainingSwaps() <= 0) {
        throw new AuthenticationException("Gói đã hết lượt!");
    }
    
    // BƯỚC 2: Validate xe
    Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));
    
    if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
        throw new AuthenticationException("Xe không thuộc quyền sở hữu!");
    }
    
    // BƯỚC 3: Tự động set thời gian booking (3 tiếng sau)
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime bookingTime = now.plusHours(3);
    
    // BƯỚC 4: Tìm và reserve pin tốt nhất
    List<Battery> availableBatteries = batteryRepository.findAll()
            .stream()
            .filter(b -> b.getCurrentStation() != null
                    && b.getCurrentStation().getId().equals(station.getId())
                    && b.getBatteryType().getId().equals(requiredBatteryType.getId())
                    && b.getStatus() == Battery.Status.AVAILABLE
                    && b.getChargeLevel().compareTo(BigDecimal.valueOf(95)) >= 0
                    && b.getStateOfHealth().compareTo(BigDecimal.valueOf(70)) >= 0)
            .sorted((b1, b2) -> {
                // Sắp xếp theo health cao nhất, sau đó charge cao nhất
                int healthCompare = b2.getStateOfHealth().compareTo(b1.getStateOfHealth());
                if (healthCompare != 0) return healthCompare;
                return b2.getChargeLevel().compareTo(b1.getChargeLevel());
            })
            .toList();
    
    // BƯỚC 5: Reserve pin (đổi status thành PENDING)
    Battery reservedBattery = availableBatteries.get(0);
    reservedBattery.setStatus(Battery.Status.PENDING);
    reservedBattery.setReservationExpiry(bookingTime);
    batteryRepository.save(reservedBattery);
    
    // BƯỚC 6: Generate confirmation code
    String confirmationCode = ConfirmationCodeGenerator.generateUnique(
            10,
            code -> bookingRepository.findByConfirmationCode(code).isPresent()
    );
    
    // BƯỚC 7: Tạo booking với status CONFIRMED
    Booking booking = new Booking();
    booking.setConfirmationCode(confirmationCode);
    booking.setStatus(Booking.Status.CONFIRMED);
    booking.setReservedBattery(reservedBattery);
    Booking savedBooking = bookingRepository.save(booking);
    
    // BƯỚC 8: Trừ lượt swap từ subscription
    int currentRemaining = activeSubscription.getRemainingSwaps();
    activeSubscription.setRemainingSwaps(currentRemaining - 1);
    driverSubscriptionRepository.save(activeSubscription);
    
    // BƯỚC 9: Gửi email xác nhận
    sendBookingConfirmedEmail(savedBooking, booking.getConfirmedBy());
    
    return savedBooking;
}
```

---

### 2.4. Hủy Booking (Driver)

```java
@Transactional
public Booking cancelMyBooking(Long id) {
    // Chỉ cho phép hủy TRƯỚC 1 TIẾNG so với giờ booking
    long minutesUntilBooking = Duration.between(now, booking.getBookingTime()).toMinutes();
    
    if (minutesUntilBooking <= 60) {
        throw new AuthenticationException("Quá gần giờ đặt! Liên hệ staff.");
    }
    
    // Giải phóng pin reserved
    Battery battery = booking.getReservedBattery();
    battery.setStatus(Battery.Status.AVAILABLE);
    battery.setReservedForBooking(null);
    batteryRepository.save(battery);
    
    // HOÀN LẠI LƯỢT SWAP
    subscription.setRemainingSwaps(oldRemaining + 1);
    
    // Cập nhật status
    booking.setStatus(Booking.Status.CANCELLED);
    
    return bookingRepository.save(booking);
}
```

---

## 3. PHẦN QR CODE SERVICE ⭐

### 3.1. Import Các Class ZXing

```java
import com.google.zxing.BarcodeFormat;           // Định dạng QR_CODE
import com.google.zxing.WriterException;         // Exception khi tạo QR
import com.google.zxing.client.j2se.MatrixToImageWriter;  // Chuyển matrix → image
import com.google.zxing.common.BitMatrix;        // Ma trận điểm ảnh QR
import com.google.zxing.qrcode.QRCodeWriter;     // Writer để tạo QR
```

**Giải thích chi tiết từng class**:

#### **1. BarcodeFormat**
```java
import com.google.zxing.BarcodeFormat;

// Enum định nghĩa các loại barcode
public enum BarcodeFormat {
    QR_CODE,      // QR Code ⭐ (Dùng trong project)
    EAN_13,       // Mã vạch sản phẩm
    CODE_128,     // Mã vạch vận chuyển
    DATA_MATRIX,  // Data Matrix
    PDF_417,      // PDF417
    // ... và nhiều loại khác
}

// Cách sử dụng:
BarcodeFormat format = BarcodeFormat.QR_CODE;
```

**QR Code là gì?**
- QR = Quick Response (Phản hồi nhanh)
- Ma trận 2D (vuông)
- Chứa được nhiều dữ liệu (URL, text, số điện thoại...)
- Có thể quét bằng camera điện thoại

#### **2. WriterException**
```java
import com.google.zxing.WriterException;

// Exception được ném ra khi không thể tạo barcode
try {
    BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 300, 300);
} catch (WriterException e) {
    // Xử lý lỗi: Dữ liệu quá dài, format không hợp lệ...
    System.err.println("Không thể tạo QR code: " + e.getMessage());
}
```

**Các lý do gây WriterException**:
- Dữ liệu quá dài (> 4296 characters cho alphanumeric)
- Format không hợp lệ
- Kích thước QR code quá nhỏ cho dữ liệu

#### **3. BitMatrix**
```java
import com.google.zxing.common.BitMatrix;

// Ma trận 2D của điểm ảnh QR code
// VD: BitMatrix 5x5:
//   0 1 2 3 4  (cột)
// 0 █ █ □ █ █
// 1 █ □ □ □ █
// 2 □ □ █ □ □
// 3 █ □ □ □ █
// 4 █ █ □ █ █
// (hàng)

// true (1) = điểm đen (█)
// false (0) = điểm trắng (□)

BitMatrix bitMatrix = qrCodeWriter.encode("Hello", BarcodeFormat.QR_CODE, 300, 300);

// Lấy giá trị tại vị trí (x, y)
boolean isBlack = bitMatrix.get(10, 20);  // true = đen, false = trắng

// Kích thước
int width = bitMatrix.getWidth();   // 300
int height = bitMatrix.getHeight(); // 300
```

**Cấu trúc QR Code**:
```
┌─────────────────────────┐
│ ┌─┬─┬─┐       ┌─┬─┬─┐  │ ← Finder patterns (3 góc)
│ ├─┼─┼─┤       ├─┼─┼─┤  │
│ └─┴─┴─┘       └─┴─┴─┘  │
│                          │
│       [DATA AREA]        │ ← Dữ liệu encode
│                          │
│ ┌─┬─┬─┐                 │
│ ├─┼─┼─┤                 │
│ └─┴─┴─┘                 │
└─────────────────────────┘
```

#### **4. QRCodeWriter**
```java
import com.google.zxing.qrcode.QRCodeWriter;

// Class chính để tạo QR code
QRCodeWriter writer = new QRCodeWriter();

// Method quan trọng: encode()
BitMatrix bitMatrix = writer.encode(
    String contents,           // Dữ liệu cần encode
    BarcodeFormat format,      // Loại barcode (QR_CODE)
    int width,                 // Chiều rộng (pixels)
    int height                 // Chiều cao (pixels)
);

// Ví dụ cụ thể:
QRCodeWriter qrCodeWriter = new QRCodeWriter();
BitMatrix bitMatrix = qrCodeWriter.encode(
    "http://example.com/quick-swap?stationId=1",  // URL
    BarcodeFormat.QR_CODE,                         // QR Code
    500,                                           // 500px width
    500                                            // 500px height
);
```

**Các tham số encode()**:
- `contents`: Chuỗi cần encode (URL, text, số...)
- `format`: BarcodeFormat.QR_CODE
- `width`: Chiều rộng QR (pixels) - càng lớn càng rõ nét
- `height`: Chiều cao QR (pixels) - thường bằng width (hình vuông)

#### **5. MatrixToImageWriter**
```java
import com.google.zxing.client.j2se.MatrixToImageWriter;

// Class utility để chuyển BitMatrix thành ảnh thực tế

// Method 1: Ghi vào file
MatrixToImageWriter.writeToPath(
    BitMatrix matrix,      // Ma trận QR
    String format,         // "PNG", "JPG", "GIF"...
    Path path              // Đường dẫn file
);

// Method 2: Ghi vào Stream (dùng trong project) ⭐
ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
MatrixToImageWriter.writeToStream(
    BitMatrix matrix,      // Ma trận QR
    String format,         // "PNG"
    OutputStream stream    // Output stream
);

// Method 3: Tạo BufferedImage
BufferedImage image = MatrixToImageWriter.toBufferedImage(
    BitMatrix matrix       // Ma trận QR
);
```

**Ví dụ sử dụng trong project**:
```java
// BƯỚC 1: Tạo BitMatrix
QRCodeWriter qrCodeWriter = new QRCodeWriter();
BitMatrix bitMatrix = qrCodeWriter.encode(
    "http://103.200.20.190:5173/quick-swap?stationId=1",
    BarcodeFormat.QR_CODE,
    300,
    300
);

// BƯỚC 2: Chuyển BitMatrix thành byte array (PNG)
ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
MatrixToImageWriter.writeToStream(
    bitMatrix,          // Ma trận QR đã tạo
    "PNG",              // Format ảnh PNG
    outputStream        // Stream để lưu ảnh
);

// BƯỚC 3: Lấy byte array
byte[] qrCodeImageBytes = outputStream.toByteArray();

// BƯỚC 4: Trả về cho client
return qrCodeImageBytes;
```

**Tại sao dùng ByteArrayOutputStream?**
- ✅ Không cần tạo file vật lý trên server
- ✅ Trả về trực tiếp qua HTTP response
- ✅ Tiết kiệm bộ nhớ (stream)
- ✅ Dễ dàng chuyển thành byte[] để gửi qua network

---

### 3.2. Cách Tạo QR Code - PHÂN TÍCH CHI TIẾT

```java
public byte[] generateQRCode(Long stationId, String baseUrl, int width, int height) 
        throws WriterException, IOException {
    
    // BƯỚC 1: Tạo URL sẽ encode vào QR
    String qrContent = baseUrl + "/quick-swap?stationId=" + stationId;
    // VD: "http://103.200.20.190:5173/quick-swap?stationId=1"
    
    // BƯỚC 2: Khởi tạo QRCodeWriter (từ ZXing)
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    
    // BƯỚC 3: Encode URL thành BitMatrix
    // BitMatrix = Ma trận điểm ảnh của QR code (0 = trắng, 1 = đen)
    BitMatrix bitMatrix = qrCodeWriter.encode(
        qrContent,           // Nội dung encode vào QR
        BarcodeFormat.QR_CODE,  // Định dạng (QR_CODE)
        width,               // Chiều rộng (px)
        height               // Chiều cao (px)
    );
    
    // BƯỚC 4: Chuyển BitMatrix thành ảnh PNG
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(
        bitMatrix,           // Ma trận QR
        "PNG",               // Định dạng ảnh
        outputStream         // Output stream
    );
    
    // BƯỚC 5: Trả về byte array của ảnh PNG
    return outputStream.toByteArray();
}
```

#### **BƯỚC 1: Tạo URL Content**

```java
String qrContent = baseUrl + "/quick-swap?stationId=" + stationId;
```

**Ví dụ cụ thể**:
```java
// Input:
Long stationId = 1L;
String baseUrl = "http://103.200.20.190:5173";

// Process:
String qrContent = "http://103.200.20.190:5173" + "/quick-swap?stationId=" + "1";

// Output:
qrContent = "http://103.200.20.190:5173/quick-swap?stationId=1"
```

**Tại sao dùng URL này?**
- Khi driver quét QR code bằng điện thoại
- Camera app tự động mở browser với URL này
- Frontend nhận `stationId=1` từ query parameter
- Frontend gọi API `/api/quick-swap/preview?stationId=1`
- Hiển thị thông tin trạm và pin sẵn có

**URL Structure**:
```
http://103.200.20.190:5173/quick-swap?stationId=1
│                          │          │
│                          │          └─ Query parameter (station ID)
│                          └─ Route path
└─ Frontend base URL
```

#### **BƯỚC 2: Khởi Tạo QRCodeWriter**

```java
QRCodeWriter qrCodeWriter = new QRCodeWriter();
```

**Trong memory**:
```
┌─────────────────────────┐
│   QRCodeWriter Object   │
│                         │
│ - encoder               │ ← Encoder component
│ - hints                 │ ← Encoding hints (optional)
│ + encode()              │ ← Method để tạo QR
└─────────────────────────┘
```

**QRCodeWriter làm gì?**
1. Nhận input string
2. Chuyển thành binary data
3. Tính toán Reed-Solomon error correction
4. Tạo ma trận QR với finder patterns
5. Đặt data vào ma trận
6. Trả về BitMatrix

#### **BƯỚC 3: Encode thành BitMatrix**

```java
BitMatrix bitMatrix = qrCodeWriter.encode(
    qrContent,              // "http://103.200.20.190:5173/quick-swap?stationId=1"
    BarcodeFormat.QR_CODE,  // Loại: QR Code
    300,                    // Width: 300 pixels
    300                     // Height: 300 pixels
);
```

**Quá trình encode**:
```
1. Input String → Binary Data
   "http://..." → 01101000 01110100 01110100 01110000...

2. Add Error Correction (Reed-Solomon)
   01101000... → 01101000... + [error correction bits]
   
3. Create QR Structure
   ┌─────────────────────┐
   │ ┌─┐       ┌─┐      │ Finder patterns (3 góc)
   │ └─┘       └─┘      │
   │                    │
   │   [Encoded Data]   │ Data modules
   │                    │
   │ ┌─┐               │ Timing patterns
   │ └─┘               │
   └────────────────────┘

4. Convert to BitMatrix (300x300)
   Each pixel is a boolean:
   bitMatrix[0][0] = true   (black pixel)
   bitMatrix[0][1] = false  (white pixel)
   ...
   bitMatrix[299][299] = true
```

**BitMatrix structure**:
```java
// BitMatrix là mảng 2D boolean
boolean[][] internalArray = new boolean[300][300];

// Access pixel tại (x, y):
boolean isBlack = bitMatrix.get(x, y);

// Ví dụ QR Code 21x21 (tối thiểu):
  0 1 2 3 4 5 6 7 8 9 ...20
0 █ █ █ █ █ █ █ □ □ □ ...□
1 █ □ □ □ □ □ █ □ █ □ ...█
2 █ □ █ █ █ □ █ □ □ █ ...□
3 █ □ █ █ █ □ █ □ █ █ ...█
...
20 █ █ █ █ █ █ █ □ □ □ ...█

█ = true (black)
□ = false (white)
```

**Tham số width và height**:
```java
// width = height = 100 (nhỏ, có thể khó quét)
BitMatrix small = writer.encode(data, QR_CODE, 100, 100);

// width = height = 300 (vừa, dùng trong project)
BitMatrix medium = writer.encode(data, QR_CODE, 300, 300);

// width = height = 1000 (lớn, rõ nét, quét xa)
BitMatrix large = writer.encode(data, QR_CODE, 1000, 1000);
```

**Lưu ý**: Width và height nên bằng nhau (QR code hình vuông)

#### **BƯỚC 4: Chuyển BitMatrix thành PNG**

```java
ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
```

**ByteArrayOutputStream là gì?**
```java
// OutputStream ghi data vào memory (không phải file)
ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

// Khi write data:
outputStream.write(byteData);

// Data được lưu trong internal buffer (memory)
// Không tạo file vật lý trên disk
```

**MatrixToImageWriter.writeToStream() làm gì?**
```
1. Đọc BitMatrix (300x300 boolean array)
   
2. Tạo BufferedImage (300x300 pixels):
   for (int y = 0; y < 300; y++) {
       for (int x = 0; x < 300; x++) {
           boolean isBlack = bitMatrix.get(x, y);
           int color = isBlack ? 0xFF000000 : 0xFFFFFFFF;
           image.setRGB(x, y, color);
       }
   }
   
3. Encode BufferedImage thành PNG format:
   PNG Header: [89 50 4E 47 0D 0A 1A 0A]
   PNG Chunks: IHDR, IDAT (compressed image data), IEND
   
4. Write PNG bytes vào OutputStream:
   outputStream.write(pngBytes);
```

**Tại sao dùng PNG?**
- ✅ Lossless compression (không mất chất lượng)
- ✅ Hỗ trợ transparency
- ✅ Kích thước file nhỏ cho QR code (2 màu)
- ✅ Được hỗ trợ rộng rãi

**So sánh với JPG**:
```java
// PNG (dùng trong project) ⭐
MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
// → Sharp edges, perfect for QR code
// → File size: ~2-5 KB

// JPG (KHÔNG NÊN DÙNG cho QR code)
MatrixToImageWriter.writeToStream(bitMatrix, "JPG", outputStream);
// → Lossy compression → blurry edges
// → QR scanner có thể không đọc được
```

#### **BƯỚC 5: Trả về Byte Array**

```java
return outputStream.toByteArray();
```

**Chuyển đổi**:
```
ByteArrayOutputStream (internal buffer)
           ↓
    .toByteArray()
           ↓
      byte[] (PNG image)
           ↓
   Return to Controller
           ↓
   HTTP Response Body
           ↓
    Client (Browser/App)
```

**Byte array structure**:
```java
byte[] imageBytes = outputStream.toByteArray();

// imageBytes[0..7]:  PNG signature (89 50 4E 47 0D 0A 1A 0A)
// imageBytes[8..]:   PNG chunks (IHDR, IDAT, IEND)

// Example:
imageBytes = [
    (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,  // PNG signature
    // ... IHDR chunk
    // ... IDAT chunk (compressed image data)
    // ... IEND chunk
]

// Total size: typically 2-10 KB for 300x300 QR code
```

#### **Toàn Bộ Luồng Dữ Liệu**

```
INPUT:
stationId = 1
baseUrl = "http://103.200.20.190:5173"
width = 300
height = 300

↓

STEP 1: String Concatenation
"http://103.200.20.190:5173/quick-swap?stationId=1"

↓

STEP 2: Create QRCodeWriter
QRCodeWriter object in memory

↓

STEP 3: Encode to BitMatrix
boolean[300][300] array
█ █ █ █ █ █ █ □ □ ...
█ □ □ □ □ □ █ □ █ ...
...

↓

STEP 4: Convert to PNG
BufferedImage → PNG encoder → byte stream
[0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, ...]

↓

STEP 5: Return byte[]
OUTPUT: byte[2547] (PNG image)
```

#### **Error Handling**

```java
public byte[] generateQRCode(Long stationId, String baseUrl, int width, int height) 
        throws WriterException, IOException {
    
    try {
        String qrContent = baseUrl + "/quick-swap?stationId=" + stationId;
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
            qrContent, 
            BarcodeFormat.QR_CODE, 
            width, 
            height
        );
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        return outputStream.toByteArray();
        
    } catch (WriterException e) {
        // Không thể encode (data quá dài, format lỗi)
        log.error("Error encoding QR code: {}", e.getMessage());
        throw e;
        
    } catch (IOException e) {
        // Không thể ghi vào stream
        log.error("Error writing QR code to stream: {}", e.getMessage());
        throw e;
    }
}
```

---

### 3.3. Response QR Code về Client - PHÂN TÍCH CHI TIẾT

```java
// Controller
@GetMapping("/station/{stationId}")
public ResponseEntity<byte[]> generateStationQRCode(
        @PathVariable Long stationId,
        @RequestParam(defaultValue = "300") int width,
        @RequestParam(defaultValue = "300") int height
) {
    QRCodeService.QRCodeResponse response = qrCodeService.generateStationQRCode(
            stationId, frontendUrl, width, height
    );
    
    // Set HTTP headers để trả về ảnh PNG
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_PNG);
    headers.setContentDispositionFormData(
        "attachment",           // Download file
        response.getFileName()  // Tên file
    );
    
    return new ResponseEntity<>(response.getImageData(), headers, HttpStatus.OK);
}
```

#### **Phân Tích Từng Phần**

#### **1. @GetMapping và Path Variables**

```java
@GetMapping("/station/{stationId}")
public ResponseEntity<byte[]> generateStationQRCode(
        @PathVariable Long stationId,
        @RequestParam(defaultValue = "300") int width,
        @RequestParam(defaultValue = "300") int height
)
```

**URL Examples**:
```
GET /api/qr-code/station/1
    → stationId = 1, width = 300 (default), height = 300 (default)

GET /api/qr-code/station/5?width=500&height=500
    → stationId = 5, width = 500, height = 500

GET /api/qr-code/station/10?width=1000
    → stationId = 10, width = 1000, height = 300 (default)
```

**@PathVariable vs @RequestParam**:
```java
// @PathVariable: Bắt buộc, part of URL path
@PathVariable Long stationId
// URL: /station/1
//                ↑ stationId

// @RequestParam: Optional (có defaultValue), query parameter
@RequestParam(defaultValue = "300") int width
// URL: /station/1?width=500
//                 ↑ width parameter
```

#### **2. ResponseEntity<byte[]>**

```java
public ResponseEntity<byte[]> generateStationQRCode(...)
```

**ResponseEntity là gì?**
```java
// ResponseEntity = HTTP Response với full control
ResponseEntity<byte[]> response = new ResponseEntity<>(
    body,           // Response body (byte[] image data)
    headers,        // HTTP headers (Content-Type, Content-Disposition...)
    HttpStatus.OK   // HTTP status code (200)
);
```

**Structure của HTTP Response**:
```http
HTTP/1.1 200 OK                              ← Status Code
Content-Type: image/png                       ← Headers
Content-Disposition: attachment; filename="qr-code-station-1-Tram-A.png"
Content-Length: 2547

[Binary PNG Data]                             ← Body (byte[])
89 50 4E 47 0D 0A 1A 0A ...
```

**Tại sao dùng byte[]?**
```java
// ✅ Trả về binary data (image, PDF, video...)
ResponseEntity<byte[]> imageResponse;

// ❌ Không dùng String cho binary data
ResponseEntity<String> textResponse;  // Chỉ dùng cho text/JSON
```

#### **3. HTTP Headers**

```java
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.IMAGE_PNG);
headers.setContentDispositionFormData("attachment", response.getFileName());
```

**setContentType(MediaType.IMAGE_PNG)**:
```java
// Set header: Content-Type: image/png
headers.setContentType(MediaType.IMAGE_PNG);

// MediaType là MIME type:
MediaType.IMAGE_PNG      → "image/png"
MediaType.IMAGE_JPEG     → "image/jpeg"
MediaType.APPLICATION_JSON → "application/json"
MediaType.TEXT_HTML      → "text/html"
```

**Tại sao cần Content-Type?**
- Browser biết đây là ảnh PNG
- Hiển thị đúng format
- Không cần đoán dựa vào file extension

**setContentDispositionFormData("attachment", fileName)**:
```java
// Set header: Content-Disposition: attachment; filename="qr-code-station-1.png"
headers.setContentDispositionFormData("attachment", "qr-code-station-1.png");
```

**Content-Disposition options**:
```java
// 1. attachment: Download file (dùng trong project) ⭐
headers.setContentDispositionFormData("attachment", "qr-code.png");
// → Browser shows "Save As" dialog
// → User downloads file

// 2. inline: Display in browser
headers.setContentDispositionFormData("inline", "qr-code.png");
// → Browser displays image directly
// → No download prompt
```

**Ví dụ cụ thể**:
```java
// Input:
Long stationId = 1L;
Station station = { id: 1, name: "Tram Quan 1" }

// Process:
String fileName = "qr-code-station-1-Tram Quan 1.png";

// Output HTTP Header:
Content-Disposition: attachment; filename="qr-code-station-1-Tram Quan 1.png"

// Browser behavior:
// 1. Receive response
// 2. Show "Save As" dialog
// 3. Default filename: "qr-code-station-1-Tram Quan 1.png"
// 4. User clicks Save → File downloaded
```

#### **4. Return ResponseEntity**

```java
return new ResponseEntity<>(
    response.getImageData(),  // Body: byte[] PNG image
    headers,                  // Headers: Content-Type, Content-Disposition
    HttpStatus.OK             // Status: 200 OK
);
```

**ResponseEntity Constructor**:
```java
new ResponseEntity<>(
    T body,                   // Response body (generic type)
    HttpHeaders headers,      // HTTP headers
    HttpStatus status         // HTTP status code
);
```

**HTTP Status Codes**:
```java
HttpStatus.OK                    // 200 - Success
HttpStatus.CREATED               // 201 - Resource created
HttpStatus.BAD_REQUEST           // 400 - Invalid request
HttpStatus.NOT_FOUND             // 404 - Resource not found
HttpStatus.INTERNAL_SERVER_ERROR // 500 - Server error
```

#### **5. Toàn Bộ Luồng Request/Response**

```
CLIENT REQUEST:
GET /api/qr-code/station/1?width=500&height=500

↓

CONTROLLER:
@GetMapping("/station/{stationId}")
- stationId = 1
- width = 500
- height = 500

↓

SERVICE:
qrCodeService.generateStationQRCode(1, frontendUrl, 500, 500)
- Validate station exists
- Generate QR code
- Return QRCodeResponse { imageData, fileName, stationName }

↓

CONTROLLER (continue):
- Create HttpHeaders
- Set Content-Type: image/png
- Set Content-Disposition: attachment; filename="..."
- Create ResponseEntity with imageData + headers + status

↓

SERVER RESPONSE:
HTTP/1.1 200 OK
Content-Type: image/png
Content-Disposition: attachment; filename="qr-code-station-1-Tram-A.png"
Content-Length: 3456

[PNG Binary Data: 89 50 4E 47 0D 0A 1A 0A ...]

↓

CLIENT (Browser):
- Receive response
- Parse headers
- Show "Save As" dialog with default filename
- User saves file to disk
```

#### **6. QRCodeResponse DTO**

```java
@Data
public static class QRCodeResponse {
    private byte[] imageData;      // PNG image as byte array
    private String fileName;       // Suggested filename
    private String stationName;    // Station name for reference
    
    public QRCodeResponse(byte[] imageData, String fileName, String stationName) {
        this.imageData = imageData;
        this.fileName = fileName;
        this.stationName = stationName;
    }
}
```

**Tại sao cần DTO?**
```java
// ❌ Trả về nhiều giá trị riêng lẻ (khó maintain)
public byte[] getImageData() { }
public String getFileName() { }
public String getStationName() { }

// ✅ Trả về 1 object chứa tất cả (clean code)
public QRCodeResponse generateQRCode() {
    return new QRCodeResponse(imageData, fileName, stationName);
}
```

#### **7. Testing với Postman/Browser**

**Postman Request**:
```
GET http://localhost:8080/api/qr-code/station/1?width=500&height=500

Headers:
Authorization: Bearer <token>
```

**Postman Response**:
```
Status: 200 OK

Headers:
Content-Type: image/png
Content-Disposition: attachment; filename="qr-code-station-1-Tram-A.png"

Body: [Binary]
→ Click "Save Response" → Save as PNG file
```

**Browser Test**:
```
Navigate to: http://localhost:8080/api/qr-code/station/1

Result:
→ Browser prompts to download file
→ File name: "qr-code-station-1-Tram-A.png"
→ Open file: See QR code image
→ Scan with phone: Navigate to quick-swap page
```

#### **8. Alternative: Display Image Inline**

```java
// Nếu muốn hiển thị ảnh trong browser thay vì download:
@GetMapping("/station/{stationId}/preview")
public ResponseEntity<byte[]> previewStationQRCode(...) {
    QRCodeService.QRCodeResponse response = qrCodeService.generateStationQRCode(...);
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_PNG);
    // Không set Content-Disposition, hoặc dùng "inline"
    
    return new ResponseEntity<>(response.getImageData(), headers, HttpStatus.OK);
}

// Result: Browser displays image directly, no download prompt
```

**Use cases**:
- `/station/{id}` → Download file (for printing)
- `/station/{id}/preview` → Display inline (for preview)

---

### 3.4. Validation Trước Khi Tạo QR

```java
public QRCodeResponse generateStationQRCode(Long stationId, String baseUrl, int width, int height) 
        throws WriterException, IOException {
    
    // 1. Validation: Kiểm tra trạm có tồn tại không
    Station station = stationRepository.findById(stationId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));
    
    // 2. Tạo QR code
    byte[] qrCodeImage = generateQRCode(stationId, baseUrl, width, height);
    
    // 3. Tạo tên file dễ hiểu
    String fileName = "qr-code-station-" + stationId + "-" + station.getName() + ".png";
    
    // 4. Trả về DTO với metadata
    return new QRCodeResponse(qrCodeImage, fileName, station.getName());
}
```

---

## 4. PHẦN QUICK SWAP SERVICE

### 4.1. Luồng Preview Quick Swap

```java
@Transactional(readOnly = true)  // Read-only: không thay đổi database
public QuickSwapPreviewResponse previewQuickSwap(Long stationId, Long vehicleId) {
    // BƯỚC 1: Kiểm tra trạm
    Station station = stationRepository.findById(stationId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));
    
    // BƯỚC 2: Kiểm tra xe thuộc quyền sở hữu
    Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));
    
    if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
        throw new AuthenticationException("Xe không thuộc quyền sở hữu!");
    }
    
    // BƯỚC 3: Kiểm tra loại pin tương thích
    if (!vehicleBatteryType.getId().equals(stationBatteryType.getId())) {
        throw new IllegalStateException("Trạm không hỗ trợ loại pin của xe!");
    }
    
    // BƯỚC 4: Kiểm tra subscription và lượt swap
    DriverSubscription activeSubscription = driverSubscriptionRepository
            .findActiveSubscriptionByDriver(currentUser, LocalDate.now())
            .orElseThrow(() -> new IllegalStateException("Chưa có gói dịch vụ!"));
    
    if (activeSubscription.getRemainingSwaps() <= 0) {
        throw new IllegalStateException("Gói đã hết lượt!");
    }
    
    // BƯỚC 5: Tìm pin AVAILABLE tốt nhất tại trạm
    List<Battery> availableBatteries = batteryRepository.findAll()
            .stream()
            .filter(b -> b.getCurrentStation().getId().equals(station.getId())
                    && b.getBatteryType().getId().equals(vehicleBatteryType.getId())
                    && b.getStatus() == Battery.Status.AVAILABLE
                    && b.getChargeLevel().compareTo(BigDecimal.valueOf(95)) >= 0
                    && b.getStateOfHealth().compareTo(BigDecimal.valueOf(70)) >= 0)
            .sorted((b1, b2) -> {
                int healthCompare = b2.getStateOfHealth().compareTo(b1.getStateOfHealth());
                if (healthCompare != 0) return healthCompare;
                return b2.getChargeLevel().compareTo(b1.getChargeLevel());
            })
            .toList();
    
    Battery newBattery = availableBatteries.get(0);
    
    // Trả về thông tin pin MỚI sẽ đổi
    response.setNewBatteryId(newBattery.getId());
    response.setNewBatteryChargeLevel(newBattery.getChargeLevel());
    response.setNewBatteryHealth(newBattery.getStateOfHealth());
    
    return response;
}
```

---

### 4.2. Thực Hiện Quick Swap

```java
@Transactional
public SwapTransaction executeQuickSwap(QuickSwapRequest request) {
    // BƯỚC 1-4: Validation (trạm, xe, subscription...)
    
    // BƯỚC 5: Lấy pin cũ trên xe
    Battery swapInBattery = vehicle.getCurrentBattery();
    
    // BƯỚC 6: Lấy ĐÚNG pin đã chọn từ Preview
    Battery swapOutBattery = batteryRepository.findById(request.getBatteryId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy pin"));
    
    // BƯỚC 7: VALIDATE pin có thể đổi được không
    // 7.1. Pin phải ở trạm đúng
    if (!swapOutBattery.getCurrentStation().getId().equals(station.getId())) {
        throw new AuthenticationException("Pin không ở trạm này!");
    }
    
    // 7.2. Pin phải AVAILABLE
    if (swapOutBattery.getStatus() != Battery.Status.AVAILABLE) {
        throw new AuthenticationException("Pin không sẵn sàng!");
    }
    
    // 7.3. Pin phải đủ charge (≥ 95%)
    if (swapOutBattery.getChargeLevel().compareTo(BigDecimal.valueOf(95)) < 0) {
        throw new AuthenticationException("Pin chưa đủ sạc!");
    }
    
    // BƯỚC 8: Tạo swap transaction và LƯU SNAPSHOT
    SwapTransaction transaction = new SwapTransaction();
    transaction.setDriver(driver);
    transaction.setVehicle(vehicle);
    transaction.setStation(station);
    transaction.setSwapOutBattery(swapOutBattery);
    transaction.setSwapInBattery(swapInBattery);
    transaction.setStatus(SwapTransaction.Status.COMPLETED);
    
    // Lưu snapshot thông tin pin TRƯỚC KHI thay đổi
    transaction.setSwapOutBatteryChargeLevel(swapOutBattery.getChargeLevel());
    transaction.setSwapOutBatteryHealth(swapOutBattery.getStateOfHealth());
    
    SwapTransaction savedTransaction = swapTransactionRepository.save(transaction);
    
    // BƯỚC 9: SAU KHI LƯU SNAPSHOT → Giảm pin xuống (mô phỏng sử dụng)
    Random random = new Random();
    BigDecimal randomChargeLevel = BigDecimal.valueOf(10 + random.nextInt(40)); // 10-49%
    swapOutBattery.setChargeLevel(randomChargeLevel);
    batteryRepository.save(swapOutBattery);
    
    // BƯỚC 10: Xử lý hoàn chỉnh swap (đổi pin, trừ lượt, gửi email)
    handleQuickSwapCompletion(savedTransaction, activeSubscription);
    
    return savedTransaction;
}
```

---

### 4.3. Xử Lý Hoàn Chỉnh Swap

```java
private void handleQuickSwapCompletion(
        SwapTransaction transaction,
        DriverSubscription subscription
) {
    // 1. Xử lý đổi pin
    handleBatterySwap(transaction, transaction.getDriver());
    
    // 2. Trừ lượt swap
    int oldRemaining = subscription.getRemainingSwaps();
    subscription.setRemainingSwaps(oldRemaining - 1);
    
    // 3. Nếu hết lượt → EXPIRED
    if (subscription.getRemainingSwaps() <= 0) {
        subscription.setStatus(DriverSubscription.Status.EXPIRED);
    }
    
    driverSubscriptionRepository.save(subscription);
    
    // 4. Gửi email thông báo
    emailService.sendSwapSuccessEmail(transaction.getDriver(), transaction, subscription);
}
```

---

### 4.4. Logic Đổi Pin

```java
private void handleBatterySwap(SwapTransaction transaction, User staff) {
    Vehicle vehicle = transaction.getVehicle();
    
    // XỬ LÝ PIN MỚI (SWAP_OUT) - Lấy từ trạm đổi cho xe
    if (transaction.getSwapOutBattery() != null) {
        Battery swapOutBattery = transaction.getSwapOutBattery();
        
        swapOutBattery.setCurrentStation(null);      // Không còn ở trạm
        swapOutBattery.setStatus(Battery.Status.IN_USE);  // Đang sử dụng
        batteryRepository.save(swapOutBattery);
        
        // Giảm SOH sau khi sử dụng
        batteryHealthService.degradeSOHAfterUsage(swapOutBattery);
    }
    
    // XỬ LÝ PIN CŨ (SWAP_IN) - Pin từ xe về trạm
    if (transaction.getSwapInBattery() != null) {
        Battery swapInBattery = transaction.getSwapInBattery();
        
        swapInBattery.setCurrentStation(transaction.getStation());  // Về trạm
        
        // Kiểm tra health: Nếu < 70% → MAINTENANCE
        BigDecimal health = swapInBattery.getStateOfHealth();
        if (health.compareTo(BigDecimal.valueOf(70)) < 0) {
            swapInBattery.setStatus(Battery.Status.MAINTENANCE);
        } else {
            // Health tốt, kiểm tra charge level
            if (swapInBattery.getChargeLevel().compareTo(BigDecimal.valueOf(100)) < 0) {
                swapInBattery.setStatus(Battery.Status.CHARGING);  // Sạc pin
                swapInBattery.setLastChargedTime(LocalDateTime.now());
            } else {
                swapInBattery.setStatus(Battery.Status.AVAILABLE);  // Sẵn sàng
            }
        }
        
        batteryRepository.save(swapInBattery);
    }
    
    // CẬP NHẬT PIN HIỆN TẠI TRÊN XE
    vehicle.setCurrentBattery(transaction.getSwapOutBattery());
    vehicleRepository.save(vehicle);
}
```

---

## 5. SO SÁNH BOOKING vs QUICK SWAP

| Tiêu chí | Booking | Quick Swap |
|----------|---------|------------|
| **Đặt trước** | Có (3 tiếng trước) | Không (đổi ngay) |
| **Reserve pin** | Có (PENDING status) | Không (pin AVAILABLE) |
| **Confirmation code** | Có | Không |
| **Trừ lượt swap** | Khi tạo booking | Khi đổi pin |
| **Staff tham gia** | Có (khi swap) | Không (driver tự đổi) |
| **Validation** | Ít hơn | Nhiều hơn (real-time) |

---

## 6. CÁC ANNOTATION QUAN TRỌNG - GIẢI THÍCH CHI TIẾT

### 6.1. Spring Stereotype Annotations

#### **@Service - Đánh Dấu Service Layer**

```java
@Service
public class BookingService {
    // Business logic
}
```

**Giải thích**:
- `@Service` là một **stereotype annotation** của Spring
- Đánh dấu class này thuộc **Service Layer** trong kiến trúc 3-layer
- Spring sẽ tự động **scan** và tạo **bean** cho class này

**Cách hoạt động**:
```
1. Application khởi động
   ↓
2. Spring Component Scan
   - Quét tất cả package
   - Tìm các class có @Service, @Repository, @Component...
   ↓
3. Tạo Bean (Instance)
   BookingService bookingService = new BookingService(...);
   ↓
4. Lưu vào IoC Container
   - Spring quản lý lifecycle của bean
   - Có thể inject vào class khác
   ↓
5. Sẵn sàng sử dụng
```

**Ví dụ thực tế**:
```java
// File: BookingService.java
package com.evbs.BackEndEvBs.service;

@Service  // ← Spring sẽ tạo bean tên "bookingService"
public class BookingService {
    
    public Booking createBooking(BookingRequest request) {
        // Logic tạo booking
    }
}

// File: BookingController.java
@RestController
public class BookingController {
    
    @Autowired  // ← Spring tự động inject bean "bookingService"
    private BookingService bookingService;
    
    @PostMapping("/bookings")
    public Booking create(@RequestBody BookingRequest request) {
        return bookingService.createBooking(request);
        // ↑ Gọi được vì Spring đã inject bean
    }
}
```

**Bean Name**:
```java
// Mặc định: tên class viết thường chữ đầu
@Service
public class BookingService { }
// → Bean name: "bookingService"

// Custom bean name:
@Service("myBookingService")
public class BookingService { }
// → Bean name: "myBookingService"
```

**So sánh với @Component**:
```java
@Component  // Generic component
@Service    // Service layer (business logic)
@Repository // Data access layer
@Controller // Web controller (returns views)
@RestController // REST API controller (returns JSON)

// Tất cả đều là @Component, chỉ khác semantic meaning
```

---

#### **@Repository - Đánh Dấu Repository Layer**

```java
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByConfirmationCode(String code);
}
```

**Giải thích**:
- Đánh dấu class/interface thuộc **Repository Layer**
- Xử lý **truy cập database**
- Spring Data JPA tự động implement các method

**Đặc biệt của @Repository**:
```java
@Repository  // ← Có thêm tính năng exception translation
public class CustomBookingRepository {
    
    public void saveBooking() {
        try {
            // Database operation
        } catch (SQLException e) {
            // @Repository tự động chuyển SQLException
            // → DataAccessException (Spring exception)
            throw new DataAccessException(e);
        }
    }
}
```

**Exception Translation**:
```
Database Exception (vendor-specific)
    ↓
@Repository annotation
    ↓
Spring DataAccessException (generic)
    ↓
Application code không phụ thuộc vào database vendor
```

**Ví dụ với JPA**:
```java
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // Spring tự động generate implementation
    Optional<Booking> findById(Long id);
    Booking save(Booking booking);
    void delete(Booking booking);
    
    // Spring tự động generate SQL từ method name
    Optional<Booking> findByConfirmationCode(String code);
    // → SELECT * FROM booking WHERE confirmation_code = ?
    
    List<Booking> findByDriverIdAndStatus(Long driverId, Status status);
    // → SELECT * FROM booking WHERE driver_id = ? AND status = ?
}
```

---

#### **@RestController - REST API Controller**

```java
@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    
    @GetMapping("/{id}")
    public Booking getBooking(@PathVariable Long id) {
        return bookingService.findById(id);
    }
}
```

**Giải thích**:
- `@RestController` = `@Controller` + `@ResponseBody`
- Tất cả method tự động serialize response thành JSON
- Dùng cho **RESTful API**

**So sánh @Controller vs @RestController**:
```java
// @Controller: Trả về view (HTML)
@Controller
public class BookingController {
    
    @GetMapping("/bookings")
    public String listBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAll());
        return "bookings";  // ← Return view name (bookings.html)
    }
}

// @RestController: Trả về JSON
@RestController
public class BookingController {
    
    @GetMapping("/bookings")
    public List<Booking> listBookings() {
        return bookingService.getAll();  // ← Return object → JSON
    }
}
```

**Response flow**:
```
Method returns Booking object
    ↓
@RestController tự động serialize
    ↓
Jackson (JSON library) convert to JSON
    ↓
HTTP Response body:
{
  "id": 1,
  "confirmationCode": "ABC123",
  "status": "CONFIRMED"
}
```

---

#### **@Transactional - Quản Lý Transaction**

```java
@Transactional
public Booking createBooking(BookingRequest request) {
    // All database operations here are in one transaction
}
```

**Đã giải thích chi tiết ở phần 2.2 ↑**

**Các tùy chọn quan trọng**:

```java
// 1. READ ONLY - Tối ưu cho query
@Transactional(readOnly = true)
public List<Booking> getAllBookings() {
    return bookingRepository.findAll();
    // Spring biết chỉ đọc → không cần lock
}

// 2. ROLLBACK FOR - Rollback khi gặp exception cụ thể
@Transactional(rollbackFor = Exception.class)
public Booking createBooking() {
    // Rollback ngay cả với checked exception
}

// 3. NO ROLLBACK FOR - Không rollback với exception cụ thể
@Transactional(noRollbackFor = IllegalArgumentException.class)
public Booking createBooking() {
    // Ném IllegalArgumentException vẫn commit transaction
}

// 4. TIMEOUT - Giới hạn thời gian
@Transactional(timeout = 30)  // 30 seconds
public Booking createBooking() {
    // Nếu > 30s → rollback
}

// 5. ISOLATION LEVEL - Mức độ cô lập
@Transactional(isolation = Isolation.READ_COMMITTED)
public Booking createBooking() {
    // Đọc được data đã commit
}

// 6. PROPAGATION - Cách transaction lồng nhau
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void methodA() {
    // Luôn tạo transaction mới
}
```

**Isolation Levels**:
```java
// READ_UNCOMMITTED (thấp nhất - nhanh nhất - ít an toàn)
Isolation.READ_UNCOMMITTED
// Đọc được data chưa commit (dirty read)

// READ_COMMITTED (default trong nhiều DB)
Isolation.READ_COMMITTED
// Chỉ đọc data đã commit

// REPEATABLE_READ
Isolation.REPEATABLE_READ
// Đọc lại cùng data trong transaction cho cùng kết quả

// SERIALIZABLE (cao nhất - chậm nhất - an toàn nhất)
Isolation.SERIALIZABLE
// Transaction chạy tuần tự như serial
```

---

#### **@Autowired - Dependency Injection**

```java
@Autowired
private BookingRepository bookingRepository;
```

**Giải thích**:
- Spring tự động **inject bean** vào field/constructor/setter
- Không cần khởi tạo bằng `new`

**3 cách Autowired**:

```java
public class BookingService {
    
    // 1. FIELD INJECTION (không khuyến khích)
    @Autowired
    private BookingRepository bookingRepository;
    
    // 2. SETTER INJECTION
    private VehicleRepository vehicleRepository;
    
    @Autowired
    public void setVehicleRepository(VehicleRepository repo) {
        this.vehicleRepository = repo;
    }
    
    // 3. CONSTRUCTOR INJECTION (khuyến khích) ⭐
    private final StationRepository stationRepository;
    
    @Autowired  // Có thể bỏ từ Spring 4.3+
    public BookingService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }
}
```

**Tại sao Constructor Injection tốt nhất?**
```java
// ✅ CONSTRUCTOR INJECTION
@Service
@RequiredArgsConstructor  // Lombok tạo constructor
public class BookingService {
    
    private final BookingRepository bookingRepository;
    // final → bắt buộc phải inject, không null
    // immutable → không thể thay đổi sau khi khởi tạo
    
    public void createBooking() {
        bookingRepository.save(...);  // Chắc chắn không null
    }
}

// ❌ FIELD INJECTION
@Service
public class BookingService {
    
    @Autowired
    private BookingRepository bookingRepository;
    // Có thể null nếu injection fail
    // Khó test (cần Spring context)
    // Không thể dùng final
}
```

**@Autowired với @Qualifier**:
```java
// Khi có nhiều bean cùng type
@Service("bookingServiceV1")
public class BookingServiceV1 { }

@Service("bookingServiceV2")
public class BookingServiceV2 { }

// Chỉ định bean nào sẽ inject
@RestController
public class BookingController {
    
    @Autowired
    @Qualifier("bookingServiceV1")  // ← Chọn bean cụ thể
    private BookingService bookingService;
}
```

**@Autowired(required = false)**:
```java
@Autowired(required = false)  // Không bắt buộc phải có bean
private EmailService emailService;

public void sendEmail() {
    if (emailService != null) {
        emailService.send();
    }
}
```

---

#### **@PreAuthorize - Phân Quyền**

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteBooking(Long id) {
    // Chỉ ADMIN mới gọi được
}
```

**Giải thích**:
- Kiểm tra **quyền truy cập** trước khi thực thi method
- Dùng **SpEL (Spring Expression Language)**

**Các ví dụ phân quyền**:

```java
// 1. Kiểm tra ROLE
@PreAuthorize("hasRole('ADMIN')")
public void adminOnly() { }

@PreAuthorize("hasRole('DRIVER')")
public void driverOnly() { }

@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public void adminOrStaff() { }

// 2. Kiểm tra AUTHORITY
@PreAuthorize("hasAuthority('WRITE_BOOKING')")
public void createBooking() { }

// 3. Kiểm tra ownership (dữ liệu thuộc về user)
@PreAuthorize("#userId == authentication.principal.id")
public Booking getMyBooking(Long userId) {
    // Chỉ lấy được booking của chính mình
}

// 4. Kết hợp điều kiện
@PreAuthorize("hasRole('DRIVER') and #booking.driverId == authentication.principal.id")
public void updateMyBooking(Booking booking) {
    // Phải là DRIVER VÀ booking phải thuộc về mình
}

// 5. Dùng custom method
@PreAuthorize("@bookingSecurityService.canAccessBooking(#bookingId)")
public Booking getBooking(Long bookingId) {
    // Gọi custom service để check quyền
}
```

**SpEL Expressions**:
```java
// authentication: Object chứa thông tin user đang login
authentication.principal       // User object
authentication.principal.id    // User ID
authentication.principal.role  // User role

// Operators
and, or, not
==, !=, <, >, <=, >=

// Method parameters (dùng #)
#userId      // Tham số userId
#booking.id  // Property của tham số
```

**Custom Security Service**:
```java
@Service("bookingSecurityService")
public class BookingSecurityService {
    
    public boolean canAccessBooking(Long bookingId) {
        User currentUser = getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId);
        
        // Check logic phức tạp
        if (currentUser.isAdmin()) return true;
        if (currentUser.isStaff() && booking.getStation().hasStaff(currentUser)) return true;
        if (currentUser.isDriver() && booking.getDriverId().equals(currentUser.getId())) return true;
        
        return false;
    }
}

// Sử dụng:
@PreAuthorize("@bookingSecurityService.canAccessBooking(#bookingId)")
public Booking getBooking(Long bookingId) { }
```

---

### 6.2. Lombok Annotations

#### **@Data - Tự Động Generate Code**

```java
@Data
public class BookingRequest {
    private Long vehicleId;
    private Long stationId;
}
```

**Lombok tự động generate**:
```java
// Code Lombok tự động tạo:
public class BookingRequest {
    private Long vehicleId;
    private Long stationId;
    
    // Getter
    public Long getVehicleId() { return vehicleId; }
    public Long getStationId() { return stationId; }
    
    // Setter
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    
    // toString()
    public String toString() {
        return "BookingRequest(vehicleId=" + vehicleId + ", stationId=" + stationId + ")";
    }
    
    // equals()
    public boolean equals(Object o) { /* compare all fields */ }
    
    // hashCode()
    public int hashCode() { /* hash all fields */ }
}
```

**@Data bao gồm**:
- `@Getter`: Tạo getter cho tất cả fields
- `@Setter`: Tạo setter cho tất cả non-final fields
- `@ToString`: Tạo toString()
- `@EqualsAndHashCode`: Tạo equals() và hashCode()
- `@RequiredArgsConstructor`: Constructor cho final fields

---

#### **@RequiredArgsConstructor - Constructor Injection**

```java
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
}
```

**Lombok tự động generate**:
```java
// Code Lombok tự động tạo:
@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    
    @Autowired
    public BookingService(
        BookingRepository bookingRepository,
        VehicleRepository vehicleRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
    }
}
```

**Chỉ tạo constructor cho**:
- `final` fields (bắt buộc inject)
- Fields có `@NonNull` annotation

```java
@RequiredArgsConstructor
public class Example {
    private final String required1;  // ← Có trong constructor
    
    @NonNull
    private String required2;        // ← Có trong constructor
    
    private String optional;         // ← KHÔNG có trong constructor
}

// Generated constructor:
public Example(String required1, @NonNull String required2) {
    this.required1 = required1;
    this.required2 = Objects.requireNonNull(required2, "required2 is marked non-null but is null");
}
```

---

#### **@Slf4j - Logger**

```java
@Service
@Slf4j
public class BookingService {
    
    public void createBooking() {
        log.info("Bắt đầu tạo booking");
        log.debug("Debug info: {}", data);
        log.warn("Cảnh báo: {}", warning);
        log.error("Lỗi: {}", error);
    }
}
```

**Lombok tự động generate**:
```java
// Code Lombok tự động tạo:
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookingService {
    
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    
    public void createBooking() {
        log.info("Bắt đầu tạo booking");
    }
}
```

**Log Levels**:
```java
log.trace("Chi tiết nhất, ít dùng");
log.debug("Debug info, chỉ dùng khi develop");
log.info("Thông tin quan trọng");      // ← Dùng nhiều nhất
log.warn("Cảnh báo, có vấn đề nhưng vẫn chạy được");
log.error("Lỗi nghiêm trọng");
```

**Log với parameters**:
```java
// ✅ ĐÚNG (efficient)
log.info("Tạo booking cho driver {}, vehicle {}", driverId, vehicleId);

// ❌ SAI (string concatenation mỗi lần)
log.info("Tạo booking cho driver " + driverId + ", vehicle " + vehicleId);
```

---

#### **@Builder - Builder Pattern**

```java
@Builder
public class Booking {
    private Long id;
    private String confirmationCode;
    private Status status;
}
```

**Sử dụng Builder**:
```java
// Tạo object với Builder pattern
Booking booking = Booking.builder()
    .id(1L)
    .confirmationCode("ABC123")
    .status(Status.CONFIRMED)
    .build();

// Thay vì:
Booking booking = new Booking();
booking.setId(1L);
booking.setConfirmationCode("ABC123");
booking.setStatus(Status.CONFIRMED);
```

**Lombok tự động generate**:
```java
public class Booking {
    private Long id;
    private String confirmationCode;
    private Status status;
    
    // Builder class
    public static class BookingBuilder {
        private Long id;
        private String confirmationCode;
        private Status status;
        
        public BookingBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public BookingBuilder confirmationCode(String code) {
            this.confirmationCode = code;
            return this;
        }
        
        public BookingBuilder status(Status status) {
            this.status = status;
            return this;
        }
        
        public Booking build() {
            return new Booking(id, confirmationCode, status);
        }
    }
    
    public static BookingBuilder builder() {
        return new BookingBuilder();
    }
}
```

---

### 6.3. Validation Annotations

#### **@Valid - Kích Hoạt Validation**

```java
@PostMapping("/bookings")
public Booking create(@Valid @RequestBody BookingRequest request) {
    // Spring tự động validate request trước khi vào method
}
```

**Cách hoạt động**:
```
1. Client gửi request với JSON body
   ↓
2. Spring deserialize JSON → BookingRequest object
   ↓
3. Thấy @Valid → Validate tất cả fields
   ↓
4. Nếu PASS → Gọi method
   Nếu FAIL → Ném MethodArgumentNotValidException
```

---

#### **@NotNull, @NotEmpty, @NotBlank**

```java
public class BookingRequest {
    
    @NotNull(message = "Vehicle ID không được null")
    private Long vehicleId;
    
    @NotEmpty(message = "Confirmation code không được rỗng")
    private String confirmationCode;
    
    @NotBlank(message = "Driver name không được blank")
    private String driverName;
}
```

**Sự khác biệt**:
```java
String value;

// @NotNull: Không được null
value = null;        // ❌ FAIL
value = "";          // ✅ PASS
value = "  ";        // ✅ PASS
value = "abc";       // ✅ PASS

// @NotEmpty: Không được null và không được rỗng
value = null;        // ❌ FAIL
value = "";          // ❌ FAIL
value = "  ";        // ✅ PASS
value = "abc";       // ✅ PASS

// @NotBlank: Không được null, không rỗng, không chỉ có whitespace
value = null;        // ❌ FAIL
value = "";          // ❌ FAIL
value = "  ";        // ❌ FAIL
value = "abc";       // ✅ PASS
```

---

#### **@Email, @Min, @Max, @Size**

```java
public class DriverRegistration {
    
    @Email(message = "Email không đúng định dạng")
    private String email;
    
    @Min(value = 18, message = "Tuổi phải >= 18")
    @Max(value = 65, message = "Tuổi phải <= 65")
    private Integer age;
    
    @Size(min = 10, max = 10, message = "Số điện thoại phải 10 số")
    private String phoneNumber;
    
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    private String password;
}
```

**Validation examples**:
```java
// @Email
"test@gmail.com"     // ✅ PASS
"invalid-email"      // ❌ FAIL

// @Min(18) @Max(65)
17                   // ❌ FAIL (< 18)
25                   // ✅ PASS
70                   // ❌ FAIL (> 65)

// @Size(min=10, max=10)
"123456789"          // ❌ FAIL (9 chars)
"1234567890"         // ✅ PASS (10 chars)
"12345678901"        // ❌ FAIL (11 chars)
```

---

### 6.4. JPA Annotations

#### **@Entity - Đánh Dấu Entity Class**

```java
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "confirmation_code", unique = true, nullable = false)
    private String confirmationCode;
}
```

**Giải thích**:
- `@Entity`: Đây là entity class map với database table
- `@Table`: Chỉ định tên table (nếu khác tên class)
- `@Id`: Primary key
- `@GeneratedValue`: Auto-increment
- `@Column`: Map với column (custom name, constraints...)

---

#### **@ManyToOne, @OneToMany**

```java
@Entity
public class Booking {
    
    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;
    // Nhiều booking → 1 driver
    
    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;
    // Nhiều booking → 1 vehicle
}

@Entity
public class User {
    
    @OneToMany(mappedBy = "driver")
    private List<Booking> bookings;
    // 1 driver → nhiều booking
}
```

**Giải thích**:
- `@ManyToOne`: Phía "nhiều" trong relationship
- `@OneToMany`: Phía "một" trong relationship
- `@JoinColumn`: Chỉ định foreign key column
- `mappedBy`: Chỉ định field ở phía đối diện

---

Đây là giải thích chi tiết về các Annotation! Bạn muốn tôi giải thích thêm annotation nào không? 🎓

---

## 7. DESIGN PATTERN SỬ DỤNG

### 7.1. Dependency Injection (DI)
- Spring tự động inject bean vào constructor
- Tránh `new` trực tiếp → dễ test, dễ maintain

### 7.2. Repository Pattern
- Tách biệt logic database (Repository) và business logic (Service)

### 7.3. DTO (Data Transfer Object) Pattern
- Request/Response classes riêng biệt với Entity
- Ví dụ: `BookingRequest`, `QuickSwapPreviewResponse`

### 7.4. Exception Handling
- Custom exceptions: `NotFoundException`, `AuthenticationException`
- Global exception handler (không thấy trong code nhưng có trong project)

---

## 8. LUỒNG HOẠT ĐỘNG TỔNG THỂ

### 8.1. Luồng Booking
```
1. Driver tạo booking → BookingRequest
2. Service validate (subscription, xe, trạm)
3. Tìm pin tốt nhất → Reserve (PENDING)
4. Generate confirmation code
5. Lưu booking (CONFIRMED)
6. Trừ lượt swap
7. Gửi email xác nhận
8. Driver đến trạm, quét code
9. Staff tạo SwapTransaction
10. Đổi pin, hoàn tất
```

### 8.2. Luồng Quick Swap
```
1. Driver quét QR Code tại trạm
2. Frontend gọi /preview → Hiển thị pin sẽ đổi
3. Driver xác nhận → QuickSwapRequest
4. Service validate real-time
5. Tạo SwapTransaction (COMPLETED)
6. Lưu snapshot pin
7. Giảm pin mới xuống (mô phỏng sử dụng)
8. Đổi pin (SWAP_OUT, SWAP_IN)
9. Trừ lượt swap
10. Gửi email thông báo
```

### 8.3. Luồng Tạo QR Code
```
1. Admin/Staff yêu cầu tạo QR cho trạm
2. QRCodeService validate trạm tồn tại
3. Generate URL: baseUrl + /quick-swap?stationId=X
4. ZXing encode URL → BitMatrix
5. MatrixToImageWriter → PNG byte array
6. Trả về file PNG download được
7. In ra và dán tại trạm
```

---

## 9. CÂU HỎI VẤN ĐÁP VỚI THẦY CÔ

### 9.1. Về Booking
**Q: Tại sao phải reserve pin khi booking?**
A: Đảm bảo khi driver đến trạm vào đúng giờ booking sẽ có pin sẵn sàng. Pin status = PENDING để không ai khác dùng được.

**Q: Tại sao trừ lượt swap ngay khi booking?**
A: Tránh driver tạo nhiều booking để giữ chỗ rồi hủy. Nếu hủy sẽ được hoàn lại lượt.

**Q: Confirmation code có tác dụng gì?**
A: Driver dùng code này để xác nhận với staff tại trạm. Staff quét/nhập code để tạo swap transaction.

### 9.2. Về QR Code
**Q: Tại sao dùng thư viện ZXing?**
A: ZXing của Google là thư viện QR code phổ biến nhất cho Java, hỗ trợ đầy đủ tính năng, miễn phí, open-source.

**Q: QR code chứa thông tin gì?**
A: Chứa URL frontend kèm stationId. VD: `http://103.200.20.190:5173/quick-swap?stationId=1`

**Q: Tại sao không chứa thông tin nhạy cảm trong QR?**
A: QR code công khai, ai cũng quét được. Thông tin chi tiết sẽ được validate qua API khi driver quét.

### 9.3. Về Quick Swap
**Q: Quick Swap khác Booking thế nào?**
A: Quick Swap là đổi ngay lập tức, không cần đặt trước. Phù hợp khi driver đang ở trạm và muốn đổi nhanh.

**Q: Tại sao Quick Swap cần Preview?**
A: Cho driver xem trước pin sẽ nhận được (charge level, health) trước khi xác nhận đổi.

**Q: Tại sao lưu snapshot thông tin pin?**
A: Ghi lại thông tin pin VÀO THỜI ĐIỂM SWAP để làm bằng chứng, không bị ảnh hưởng nếu pin thay đổi sau đó.

### 9.4. Về Transaction Management
**Q: @Transactional hoạt động như thế nào?**
A: Spring tạo proxy bao quanh method. Nếu method thành công → commit, nếu có exception → rollback toàn bộ.

**Q: Tại sao cần @Transactional(readOnly = true)?**
A: Tối ưu performance, Spring biết method này chỉ đọc không ghi → có thể cache, không cần lock.

---

## 10. BEST PRACTICES HỌC ĐƯỢC

### 10.1. Validation Layers
```java
// Layer 1: Request validation (@Valid annotation)
// Layer 2: Business logic validation (trong service)
// Layer 3: Database constraint validation
```

### 10.2. Exception Handling
```java
// Ném custom exception với message rõ ràng
throw new AuthenticationException("Xe chưa được phê duyệt!");
throw new NotFoundException("Không tìm thấy trạm");
```

### 10.3. Logging
```java
@Slf4j  // Lombok tự động tạo logger
log.info("Quick swap hoàn tất - Transaction ID: {}", savedTransaction.getId());
log.error("Lỗi khi gửi email: {}", e.getMessage());
```

### 10.4. Separation of Concerns
- Controller: Nhận request, trả response
- Service: Business logic
- Repository: Database access
- Entity: Data model
- DTO: Data transfer

---

## 11. TÀI LIỆU THAM KHẢO

### 11.1. Documentation
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [ZXing Wiki](https://github.com/zxing/zxing/wiki)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Lombok](https://projectlombok.org/)

### 11.2. Code trong Project
- `QRCodeService.java` - Tạo QR code
- `BookingService.java` - Logic booking
- `QuickSwapService.java` - Logic quick swap
- `pom.xml` - Dependencies

---

## KẾT LUẬN

Project này sử dụng các công nghệ hiện đại:
- **Spring Boot**: Framework chính
- **ZXing**: Tạo QR code
- **JPA**: Quản lý database
- **Lombok**: Giảm boilerplate code
- **Transaction Management**: Đảm bảo data consistency

Các pattern áp dụng:
- Dependency Injection
- Repository Pattern
- DTO Pattern
- Transaction Management

Chúc bạn học tốt và vấn đáp thành công! 🎓
