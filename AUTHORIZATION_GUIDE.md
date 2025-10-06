# 🔐 Hướng dẫn phân quyền trong Spring Boot

## 1. Setup cơ bản

### a) Kích hoạt Method Security
```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    // ... config khác
}
```

### b) User Entity với Role
```java
public enum Role {
    DRIVER,   // Người lái xe
    STAFF,    // Nhân viên  
    ADMIN     // Quản trị viên
}
```

## 2. Các cách phân quyền chính

### a) **Chỉ ADMIN** - Quản lý hệ thống
```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<...> adminOnlyMethod() {
    // VD: Tạo/sửa/xóa user, cấu hình hệ thống
}
```

### b) **ADMIN + STAFF** - Vận hành
```java
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
public ResponseEntity<...> adminAndStaffMethod() {
    // VD: Xem báo cáo, quản lý đơn hàng
}
```

### c) **Tất cả user đã đăng nhập**
```java
@PreAuthorize("isAuthenticated()")
public ResponseEntity<...> authenticatedMethod() {
    // VD: Xem profile cá nhân, lịch sử giao dịch
}
```

### d) **Chỉ chính user đó hoặc ADMIN**
```java
@PreAuthorize("hasRole('ADMIN') or @userService.getCurrentUser().id == #userId")
public ResponseEntity<...> updateProfile(@PathVariable Long userId) {
    // VD: Sửa thông tin cá nhân
}
```

## 3. Phân quyền theo từng module

### 🧑‍💼 **User Management**
```java
@RequestMapping("/api/admin/user")
public class AdminUserController {
    
    @PreAuthorize("hasRole('ADMIN')")  // Chỉ admin
    @PostMapping
    public ResponseEntity<...> createUser() {}
    
    @PreAuthorize("hasRole('ADMIN')")  // Chỉ admin
    @DeleteMapping("/{id}")
    public ResponseEntity<...> deleteUser() {}
}
```

### 🔋 **Battery Station Management**
```java
@RequestMapping("/api/station")
public class StationController {
    
    @PreAuthorize("hasRole('ADMIN')")  // Admin tạo trạm
    @PostMapping
    public ResponseEntity<...> createStation() {}
    
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")  // Admin/Staff quản lý
    @PutMapping("/{id}")
    public ResponseEntity<...> updateStation() {}
    
    @PreAuthorize("isAuthenticated()")  // Tất cả user xem được
    @GetMapping
    public ResponseEntity<...> getAllStations() {}
}
```

### 🔄 **Battery Swap**
```java
@RequestMapping("/api/swap")
public class SwapController {
    
    @PreAuthorize("hasRole('DRIVER')")  // Chỉ driver đổi pin
    @PostMapping
    public ResponseEntity<...> requestSwap() {}
    
    @PreAuthorize("hasRole('STAFF')")  // Staff xử lý yêu cầu
    @PutMapping("/{id}/approve")
    public ResponseEntity<...> approveSwap() {}
    
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")  // Admin/Staff xem báo cáo
    @GetMapping("/report")
    public ResponseEntity<...> getSwapReport() {}
}
```

### 💰 **Payment**
```java
@RequestMapping("/api/payment")
public class PaymentController {
    
    @PreAuthorize("hasRole('DRIVER')")  // Driver thanh toán
    @PostMapping
    public ResponseEntity<...> makePayment() {}
    
    @PreAuthorize("hasRole('ADMIN')")  // Admin xem tất cả giao dịch
    @GetMapping("/all")
    public ResponseEntity<...> getAllPayments() {}
    
    @PreAuthorize("@paymentService.isOwner(authentication.name, #paymentId)")  // Chỉ chủ giao dịch
    @GetMapping("/{paymentId}")
    public ResponseEntity<...> getPaymentDetail(@PathVariable Long paymentId) {}
}
```

## 4. Custom Authorization Logic

### a) Tạo Service check quyền
```java
@Service
public class AuthorizationService {
    
    public boolean isOwner(String username, Long resourceId) {
        // Logic kiểm tra user có phải chủ sở hữu resource không
        User currentUser = getCurrentUser(username);
        return resourceRepository.findById(resourceId)
                .map(resource -> resource.getOwnerId().equals(currentUser.getId()))
                .orElse(false);
    }
    
    public boolean canAccessStation(String username, Long stationId) {
        User user = getCurrentUser(username);
        if (user.getRole() == Role.ADMIN) return true;
        if (user.getRole() == Role.STAFF) {
            // Staff chỉ truy cập trạm được phân công
            return userStationService.isAssigned(user.getId(), stationId);
        }
        return false;
    }
}
```

### b) Sử dụng custom authorization
```java
@PreAuthorize("@authorizationService.canAccessStation(authentication.name, #stationId)")
@GetMapping("/station/{stationId}/details")
public ResponseEntity<...> getStationDetails(@PathVariable Long stationId) {}
```

## 5. Error Handling cho Authorization

```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body("Bạn không có quyền truy cập tài nguyên này");
}
```

## 6. Testing Authorization

```java
@Test
@WithMockUser(roles = "ADMIN")  // Giả lập user có role ADMIN
public void testAdminCanCreateUser() {
    // Test code
}

@Test
@WithMockUser(roles = "DRIVER")  // Giả lập user có role DRIVER
public void testDriverCannotCreateUser() {
    // Expect AccessDeniedException
}
```

## 7. Best Practices

### ✅ **Nên làm:**
- Luôn kiểm tra quyền ở Controller level
- Sử dụng role rõ ràng và có ý nghĩa
- Tạo custom authorization cho logic phức tạp
- Test kỹ các trường hợp phân quyền

### ❌ **Không nên:**
- Chỉ dựa vào front-end để hide/show UI
- Hardcode role trong code
- Bỏ qua kiểm tra ownership
- Cho phép tất cả endpoints permitAll() trong production

## 8. Pattern thường dùng cho EVBS

```java
// Tạo/Xóa tài nguyên quan trọng
@PreAuthorize("hasRole('ADMIN')")

// Quản lý vận hành hàng ngày
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")

// Truy cập dữ liệu cá nhân
@PreAuthorize("hasRole('ADMIN') or @service.isOwner(authentication.name, #id)")

// Chức năng dành cho driver
@PreAuthorize("hasRole('DRIVER')")

// Xem thông tin công khai
@PreAuthorize("isAuthenticated()")
```