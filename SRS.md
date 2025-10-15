# Software Requirements Specification (SRS)
## Hệ thống Quản lý Trạm Thay Pin Xe Điện
### (EV Battery Swap Station Management System)

---

**Document Information**
- **Project Name**: EV Battery Swap Station Management System
- **Document Type**: Software Requirements Specification (SRS)
- **Version**: 2.0
- **Date**: October 14, 2025
- **Status**: Final
- **Classification**: Internal Use

**Document History**

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | 2025-10-14 | Development Team | Initial draft |
| 2.0 | 2025-10-14 | Development Team | Complete specification |

**Approval**

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Project Manager | | | |
| Technical Lead | | | |
| Quality Assurance | | | |

---

## Table of Contents

1. [GIỚI THIỆU](#1-giới-thiệu)
2. [TỔNG QUAN HỆ THỐNG](#2-tổng-quan-hệ-thống)
3. [YÊU CẦU CHỨC NĂNG](#3-yêu-cầu-chức-năng)
4. [YÊU CẦU PHI CHỨC NĂNG](#4-yêu-cầu-phi-chức-năng)
5. [KIẾN TRÚC HỆ THỐNG](#5-kiến-trúc-hệ-thống)
6. [THIẾT KẾ GIAO DIỆN](#6-thiết-kế-giao-diện)
7. [CƠ SỞ DỮ LIỆU](#7-cơ-sở-dữ-liệu)
8. [BẢO MẬT](#8-bảo-mật)
9. [KIỂM THỬ](#9-kiểm-thử)
10. [TRIỂN KHAI](#10-triển-khai)
11. [PHỤ LỤC](#11-phụ-lục)

---

## 1. GIỚI THIỆU

### 1.1 Mục đích tài liệu
Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này mô tả đầy đủ các yêu cầu chức năng và phi chức năng cho Hệ thống Quản lý Trạm Thay Pin Xe Điện (EV Battery Swap Station Management System). Tài liệu này phục vụ như một hợp đồng giữa các bên liên quan và là cơ sở cho việc thiết kế, phát triển, kiểm thử và bảo trì hệ thống.

**Đối tượng đọc:**
- Đội ngũ phát triển phần mềm
- Kiến trúc sư hệ thống
- Đội ngũ kiểm thử
- Product Manager và Stakeholders
- Đội ngũ vận hành và bảo trì

### 1.2 Phạm vi sản phẩm
**Tên sản phẩm:** EV Battery Swap Station Management System (EVBSSMS)

**Mô tả:** Hệ thống quản lý toàn diện cho mạng lưới trạm thay pin xe điện, bao gồm:

#### 1.2.1 Các module chính:
- **Quản lý người dùng và phân quyền**
  - Đăng ký, đăng nhập, xác thực
  - Phân quyền theo vai trò (RBAC)
  - Quản lý hồ sơ người dùng

- **Quản lý trạm thay pin**
  - CRUD operations cho trạm
  - Quản lý kho pin tại từng trạm
  - Theo dõi trạng thái hoạt động

- **Quản lý pin và lịch sử**
  - Quản lý thông tin pin
  - Theo dõi trạng thái pin
  - Lưu trữ lịch sử sử dụng

- **Hệ thống đặt lịch**
  - Đặt lịch thay pin trước
  - Quản lý lịch trình
  - Thông báo và nhắc nhở

- **Xử lý giao dịch**
  - Giao dịch thay pin
  - Tích hợp thanh toán
  - Quản lý gói dịch vụ

- **Hỗ trợ khách hàng**
  - Hệ thống ticket
  - Phản hồi và giải quyết

#### 1.2.2 Lợi ích mang lại:
- Tối ưu hóa quy trình thay pin xe điện
- Giảm thời gian chờ đợi của khách hàng
- Tăng hiệu quả quản lý tài nguyên
- Cải thiện trải nghiệm người dùng
- Hỗ trợ mở rộng quy mô kinh doanh

### 1.3 Định nghĩa, từ viết tắt và ký hiệu

#### 1.3.1 Định nghĩa
- **Battery Swap**: Quá trình thay thế pin cũ bằng pin mới đã được sạc đầy
- **Driver**: Chủ sở hữu xe điện sử dụng dịch vụ thay pin
- **Staff**: Nhân viên vận hành tại trạm thay pin
- **Admin**: Quản trị viên hệ thống
- **Station**: Trạm thay pin xe điện
- **Inventory**: Kho lưu trữ pin tại trạm

#### 1.3.2 Từ viết tắt
- **API**: Application Programming Interface
- **CRUD**: Create, Read, Update, Delete
- **EV**: Electric Vehicle (Xe điện)
- **EVBSSMS**: EV Battery Swap Station Management System
- **HTTP/HTTPS**: HyperText Transfer Protocol (Secure)
- **JPA**: Java Persistence API
- **JSON**: JavaScript Object Notation
- **JWT**: JSON Web Token
- **RBAC**: Role-Based Access Control
- **REST**: Representational State Transfer
- **SOC**: State of Charge (Mức sạc pin)
- **SQL**: Structured Query Language
- **SRS**: Software Requirements Specification
- **UI/UX**: User Interface/User Experience

#### 1.3.3 Ký hiệu
- **[R]**: Required (Bắt buộc)
- **[O]**: Optional (Tùy chọn)
- **[C]**: Conditional (Có điều kiện)
- **FR**: Functional Requirement
- **NFR**: Non-Functional Requirement

### 1.4 Tham khảo
- IEEE 830-1998: Recommended Practice for Software Requirements Specifications
- ISO/IEC 25010: Systems and software Quality Requirements and Evaluation
- Spring Boot Documentation 3.5.6
- Microsoft SQL Server Documentation
- RESTful API Design Guidelines

---

## 2. TỔNG QUAN HỆ THỐNG

### 2.1 Mô tả sản phẩm

#### 2.1.1 Bối cảnh
Với sự phát triển mạnh mẽ của ngành xe điện tại Việt Nam, nhu cầu về các giải pháp sạc và thay pin hiệu quả ngày càng tăng cao. Hệ thống EVBSSMS được phát triển để đáp ứng nhu cầu này thông qua việc cung cấp một nền tảng quản lý toàn diện cho mạng lưới trạm thay pin xe điện.

#### 2.1.2 Tổng quan giải pháp
EVBSSMS là một hệ thống backend RESTful API được xây dựng trên nền tảng Spring Boot, cung cấp các dịch vụ sau:
- Quản lý người dùng đa cấp với hệ thống phân quyền RBAC
- Quản lý mạng lưới trạm thay pin trên toàn quốc
- Theo dõi và quản lý kho pin thời gian thực
- Hệ thống đặt lịch và xử lý giao dịch tự động
- Tích hợp thanh toán đa phương thức
- Hỗ trợ khách hàng 24/7

#### 2.1.3 Kiến trúc tổng thể
```
[Mobile App] ←→ [Web Frontend] ←→ [REST API Gateway] ←→ [Backend Services]
                                                            ↓
                                                    [Database Layer]
```

### 2.2 Chức năng chính

#### 2.2.1 Authentication & User Management
- **Đăng ký/Đăng nhập đa kênh**: Email, số điện thoại
- **Xác thực bảo mật**: JWT token với refresh mechanism
- **Phân quyền RBAC**: 3 roles chính (Driver, Staff, Admin)
- **Quản lý hồ sơ**: Cập nhật thông tin cá nhân, lịch sử giao dịch

#### 2.2.2 Station & Inventory Management
- **CRUD Trạm**: Tạo, cập nhật, xóa thông tin trạm
- **Quản lý địa điểm**: Tích hợp bản đồ và định vị GPS
- **Quản lý kho pin**: Theo dõi số lượng, trạng thái pin realtime
- **Capacity Planning**: Dự đoán nhu cầu và tối ưu hóa kho

#### 2.2.3 Battery Lifecycle Management
- **Quản lý thông tin pin**: Model, capacity, specifications
- **Theo dõi trạng thái**: SOC, health status, temperature
- **Lịch sử sử dụng**: Tracking đầy đủ lifecycle của từng pin
- **Maintenance Scheduling**: Lập lịch bảo trì định kỳ

#### 2.2.4 Booking & Transaction Processing
- **Smart Booking**: Đặt lịch với AI optimization
- **Real-time Availability**: Kiểm tra trạng thái trạm realtime
- **Transaction Processing**: Xử lý giao dịch tự động
- **Queue Management**: Quản lý hàng đợi thông minh

#### 2.2.5 Payment & Subscription
- **Multi-payment Gateway**: Hỗ trợ đa phương thức thanh toán
- **Subscription Plans**: Các gói dịch vụ linh hoạt
- **Billing Management**: Quản lý hóa đơn và chi phí
- **Loyalty Program**: Chương trình khách hàng thân thiết

#### 2.2.6 Support & Analytics
- **Ticketing System**: Hệ thống hỗ trợ chuyên nghiệp
- **Real-time Chat**: Hỗ trợ trực tuyến 24/7
- **Analytics Dashboard**: Báo cáo và phân tích dữ liệu
- **Performance Monitoring**: Giám sát hiệu suất hệ thống

### 2.3 Người dùng và Stakeholders

#### 2.3.1 Primary Users

**Driver (Khách hàng)**
- **Profile**: Chủ sở hữu xe điện cần dịch vụ thay pin
- **Goals**: Thay pin nhanh, tiện lợi, giá cả hợp lý
- **Pain Points**: Thời gian chờ đợi, tìm trạm, thanh toán phức tạp
- **Key Features**: Booking, payment, history tracking

**Staff (Nhân viên trạm)**
- **Profile**: Nhân viên vận hành tại trạm thay pin
- **Goals**: Xử lý giao dịch hiệu quả, quản lý kho pin
- **Pain Points**: Quá tải công việc, thiếu thông tin realtime
- **Key Features**: Transaction processing, inventory management

**Admin (Quản trị viên)**
- **Profile**: Quản lý cấp cao của hệ thống
- **Goals**: Giám sát toàn bộ operations, tối ưu hiệu quả
- **Pain Points**: Thiếu visibility, khó quản lý quy mô lớn
- **Key Features**: Analytics, system management, user management

#### 2.3.2 Secondary Stakeholders

**Business Owners**
- ROI tracking và business intelligence
- Revenue optimization và cost management

**Maintenance Team**
- Battery health monitoring
- Preventive maintenance scheduling

**Customer Service**
- Support ticket management
- Customer satisfaction tracking

### 2.4 Constraints và Assumptions

#### 2.4.1 Business Constraints
- Budget: Giới hạn ngân sách phát triển và vận hành
- Timeline: Ra mắt sản phẩm trong Q4 2025
- Compliance: Tuân thủ các quy định về an toàn điện và bảo vệ dữ liệu

#### 2.4.2 Technical Constraints
- Platform: Java 17, Spring Boot 3.5.6
- Database: Microsoft SQL Server
- Integration: RESTful API với external payment gateways
- Performance: Response time < 2s, 99.9% uptime

#### 2.4.3 Assumptions
- Người dùng có smartphone với internet connection
- Các trạm thay pin có kết nối internet ổn định
- Payment gateways hoạt động bình thường
- Đội ngũ kỹ thuật có đủ expertise để vận hành hệ thống

---

## 3. YÊU CẦU CHỨC NĂNG

### 3.1 User Authentication & Authorization Module

#### 3.1.1 User Registration
**FR-001: Đăng ký tài khoản mới** [R]
- **Description**: Người dùng có thể tạo tài khoản mới trong hệ thống
- **Actor**: Guest User
- **Preconditions**: Người dùng chưa có tài khoản
- **Input**: 
  - FullName (String, required, max 150 chars)
  - Email (String, required, unique, valid format)
  - PhoneNumber (String, required, unique, VN format: 03/05/07/08/09xxxxxxxx)
  - Password (String, required, min 8 chars, strong password)
- **Business Rules**:
  - Email phải là duy nhất trong hệ thống
  - Số điện thoại phải theo format Việt Nam
  - Password phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số
- **Output**: User entity với role mặc định là DRIVER
- **Postconditions**: 
  - Tài khoản được tạo với status ACTIVE
  - Email xác thực được gửi (future enhancement)
- **Exception Flows**:
  - Email đã tồn tại → Error message
  - Số điện thoại đã tồn tại → Error message
  - Password không đủ mạnh → Validation error

#### 3.1.2 User Authentication
**FR-002: Đăng nhập hệ thống** [R]
- **Description**: Người dùng đăng nhập bằng email hoặc số điện thoại
- **Actor**: Registered User
- **Preconditions**: Có tài khoản hợp lệ với status ACTIVE
- **Input**: 
  - Username (email hoặc phoneNumber)
  - Password
- **Process**: 
  1. Validate input credentials
  2. Authenticate user
  3. Generate JWT access token (15 minutes)
  4. Generate refresh token (7 days)
- **Output**: 
  - Access token (JWT)
  - Refresh token
  - User profile information
- **Business Rules**:
  - Tài khoản bị SUSPENDED không thể đăng nhập
  - Sau 5 lần đăng nhập sai → lock account 15 minutes
- **Exception Flows**:
  - Credentials sai → Authentication error
  - Account suspended → Access denied error

**FR-003: Refresh Token** [R]
- **Description**: Gia hạn access token khi hết hạn
- **Input**: Valid refresh token
- **Output**: New access token và refresh token
- **Business Rules**: Refresh token chỉ sử dụng được 1 lần

#### 3.1.3 User Profile Management
**FR-004: Cập nhật thông tin cá nhân** [R]
- **Description**: Người dùng có thể cập nhật thông tin profile
- **Actor**: Authenticated User
- **Input**: Updated profile fields (FullName, PhoneNumber)
- **Business Rules**: Email không thể thay đổi sau khi tạo account

**FR-005: Quản lý trạng thái tài khoản** [R]
- **Description**: Admin có thể quản lý trạng thái user accounts
- **Actor**: Admin
- **Input**: UserID, new status (ACTIVE/INACTIVE/SUSPENDED)
- **Business Rules**: Admin không thể suspend chính mình

### 3.2 Station Management Module

#### 3.2.1 Station CRUD Operations
**FR-006: Tạo trạm mới** [R]
- **Description**: Admin/Staff có thể tạo trạm thay pin mới
- **Actor**: Admin, Staff
- **Input**:
  - Name (String, required, max 200 chars)
  - Location (String, required, address)
  - Latitude, Longitude (Double, required)
  - Capacity (Integer, required, > 0)
  - OperatingHours (String, format: "HH:MM-HH:MM")
  - Status (Enum: ACTIVE/INACTIVE/MAINTENANCE)
- **Business Rules**:
  - Tọa độ GPS phải hợp lệ
  - Capacity phải > 0
  - Operating hours phải valid format
- **Output**: Station entity được tạo

**FR-007: Xem danh sách trạm** [R]
- **Description**: Tất cả người dùng có thể xem danh sách trạm
- **Actor**: All users
- **Input**: 
  - Page (optional, default 1)
  - Size (optional, default 10)
  - Location filter (optional)
- **Output**: Paginated list of stations với thông tin cơ bản
- **Business Rules**: Chỉ hiển thị trạm có status ACTIVE cho public

**FR-008: Xem chi tiết trạm** [R]
- **Description**: Xem thông tin chi tiết của một trạm
- **Actor**: All users
- **Input**: Station ID
- **Output**: 
  - Thông tin trạm đầy đủ
  - Số lượng pin available
  - Operating hours
  - Current queue status

**FR-009: Cập nhật thông tin trạm** [R]
- **Description**: Admin/Staff cập nhật thông tin trạm
- **Actor**: Admin, Staff
- **Input**: Station ID + updated fields
- **Business Rules**: Staff chỉ có thể update trạm mình được assigned

**FR-010: Xóa trạm** [R]
- **Description**: Admin có thể xóa trạm (soft delete)
- **Actor**: Admin only
- **Preconditions**: Trạm không có battery nào đang IN_USE
- **Business Rules**: Không thể xóa trạm có giao dịch đang pending

### 3.3 Battery Management Module

#### 3.3.1 Battery CRUD Operations
**FR-011: Tạo pin mới** [R]
- **Description**: Admin/Staff có thể thêm pin mới vào hệ thống
- **Actor**: Admin, Staff
- **Input**:
  - Model (String, required)
  - Capacity (BigDecimal, required, in kWh)
  - Manufacturer (String, required)
  - SerialNumber (String, required, unique)
  - PurchaseDate (Date, required)
  - WarrantyExpiry (Date, optional)
- **Business Rules**:
  - SerialNumber phải unique
  - Capacity > 0
  - PurchaseDate không thể trong tương lai
- **Output**: Battery entity với status AVAILABLE

**FR-012: Cập nhật thông tin pin** [R]
- **Description**: Admin/Staff cập nhật thông tin pin
- **Business Rules**: 
  - Không thể sửa SerialNumber
  - Pin đang IN_USE có hạn chế update

**FR-013: Xóa pin** [R]
- **Description**: Admin có thể xóa pin khỏi hệ thống
- **Preconditions**: Pin phải có status AVAILABLE
- **Business Rules**: Không thể xóa pin đang được sử dụng

#### 3.3.2 Battery Status Management
**FR-014: Cập nhật trạng thái pin** [R]
- **Description**: Staff có thể cập nhật trạng thái pin
- **Input**: Battery ID + new status
- **Valid Status Transitions**:
  - AVAILABLE → IN_USE (khi swap)
  - IN_USE → AVAILABLE (sau khi return)
  - AVAILABLE → MAINTENANCE (khi cần bảo trì)
  - MAINTENANCE → AVAILABLE (sau bảo trì)
  - Any → FAULTY (khi phát hiện lỗi)

#### 3.3.3 Battery History Tracking
**FR-015: Ghi lại lịch sử pin** [R]
- **Description**: Hệ thống tự động ghi lại mọi hoạt động của pin
- **Events được tracking**:
  - CREATED: Pin được tạo
  - CHARGED: Pin được sạc
  - DISCHARGED: Pin bị xả
  - SWAPPED_OUT: Pin được lấy ra khỏi xe
  - SWAPPED_IN: Pin được lắp vào xe
  - MAINTENANCE_START: Bắt đầu bảo trì
  - MAINTENANCE_END: Kết thúc bảo trì
  - RELOCATED: Pin được chuyển trạm
- **Data Recorded**:
  - EventType, Timestamp
  - Battery ID, Station ID
  - Vehicle ID (if applicable)
  - Staff ID, Notes
  - Previous/Current SOC (State of Charge)

### 3.4 Station Inventory Module

**FR-016: Thêm pin vào trạm** [R]
- **Description**: Staff có thể thêm pin vào kho của trạm
- **Actor**: Admin, Staff
- **Input**:
  - Station ID
  - Battery ID
  - Status (default: AVAILABLE)
- **Business Rules**:
  - Pin chỉ có thể ở một trạm tại một thời điểm
  - Trạm không được vượt quá capacity
  - Pin phải có status phù hợp

**FR-017: Xem kho pin theo trạm** [R]
- **Description**: Xem danh sách pin tại một trạm cụ thể
- **Output**: 
  - Tổng số pin
  - Số pin AVAILABLE
  - Số pin IN_USE
  - Số pin MAINTENANCE
  - Chi tiết từng pin

**FR-018: Chuyển pin giữa các trạm** [R]
- **Description**: Admin có thể di chuyển pin giữa các trạm
- **Business Rules**: 
  - Pin phải AVAILABLE
  - Trạm đích phải có capacity
  - Tự động log vào BatteryHistory

### 3.5 Booking Management Module

#### 3.5.1 Booking Creation
**FR-019: Đặt lịch thay pin** [R]
- **Description**: Driver có thể đặt lịch thay pin trước
- **Actor**: Driver (authenticated)
- **Input**:
  - Station ID
  - Vehicle ID
  - Preferred DateTime
  - Special Requirements (optional)
- **Business Rules**:
  - Chỉ được đặt lịch trong giờ hoạt động
  - Không được đặt quá 7 ngày trước
  - Mỗi driver chỉ có tối đa 3 booking PENDING
  - Phải có pin available tại thời điểm đặt
- **Output**: Booking entity với status PENDING
- **Notifications**: SMS/Email confirmation

#### 3.5.2 Booking Management
**FR-020: Xem lịch đặt của tôi** [R]
- **Description**: Driver xem danh sách booking của mình
- **Output**: List of bookings với status và chi tiết

**FR-021: Hủy đặt lịch** [R]
- **Description**: Driver có thể hủy booking
- **Business Rules**:
  - Chỉ hủy được booking có status PENDING
  - Phải hủy trước ít nhất 30 phút
  - Sau khi hủy không thể khôi phục

**FR-022: Xác nhận đặt lịch** [R]
- **Description**: Staff xác nhận booking khi driver đến
- **Actor**: Staff
- **Process**: PENDING → CONFIRMED → Ready for swap

### 3.6 Swap Transaction Module

#### 3.6.1 Transaction Processing
**FR-023: Tạo giao dịch thay pin** [R]
- **Description**: Staff xử lý giao dịch thay pin
- **Actor**: Staff
- **Preconditions**: 
  - Có booking CONFIRMED hoặc walk-in customer
  - Trạm có pin AVAILABLE
- **Input**:
  - Driver ID (từ booking hoặc manual input)
  - Vehicle ID
  - Old Battery ID (from vehicle)
  - New Battery ID (from station inventory)
- **Process Flow**:
  1. Validate inputs
  2. Update old battery: IN_USE → AVAILABLE
  3. Update new battery: AVAILABLE → IN_USE
  4. Update vehicle current battery
  5. Calculate cost based on subscription
  6. Create payment record
  7. Update station inventory
  8. Log battery history
- **Output**: SwapTransaction record
- **Business Rules**:
  - Old battery về station hiện tại
  - New battery phải compatible với vehicle
  - Transaction không thể rollback sau khi complete

#### 3.6.2 Cost Calculation
**FR-024: Tính toán chi phí giao dịch** [R]
- **Description**: Hệ thống tự động tính cost dựa trên subscription
- **Factors**:
  - Base price từ service package
  - Driver subscription status và discount
  - Peak/off-peak pricing (future)
  - Location-based pricing (future)
- **Business Rules**:
  - Subscriber có discount theo gói
  - Non-subscriber trả full price
  - Giá không thể âm

### 3.7 Payment Module

#### 3.7.1 Payment Processing
**FR-025: Xử lý thanh toán** [R]
- **Description**: Driver thực hiện thanh toán cho giao dịch
- **Actor**: Driver
- **Input**:
  - Transaction ID
  - Payment Method (CASH, CARD, E_WALLET)
  - Amount
- **Payment Methods**:
  - Cash: Staff xác nhận đã nhận tiền
  - Card: Integration với payment gateway
  - E-Wallet: QR code hoặc API integration
- **Status Flow**: PENDING → PROCESSING → COMPLETED/FAILED
- **Business Rules**:
  - Amount phải match với transaction cost
  - Timeout sau 15 phút nếu không complete

**FR-026: Xem lịch sử thanh toán** [R]
- **Description**: Driver xem lịch sử các giao dịch thanh toán
- **Output**: Payment history với details và receipts

#### 3.7.2 Refund Processing
**FR-027: Xử lý hoàn tiền** [R]
- **Description**: Admin/Staff có thể hoàn tiền trong trường hợp đặc biệt
- **Actor**: Admin, Staff (with approval)
- **Business Rules**: 
  - Chỉ hoàn tiền trong 24h sau transaction
  - Cần lý do và approval

### 3.8 Service Package & Subscription Module

#### 3.8.1 Service Package Management
**FR-028: Quản lý gói dịch vụ** [R]
- **Description**: Admin tạo và quản lý các gói dịch vụ
- **Actor**: Admin
- **Package Types**:
  - BASIC: Pay-per-use, no discount
  - PREMIUM: Monthly fee, 15% discount
  - VIP: Monthly fee, 25% discount, priority booking
- **Input**:
  - Name, Description
  - Price (monthly fee)
  - Discount percentage
  - Max swaps per month
  - Special benefits
- **Business Rules**:
  - Giá phải >= 0
  - Discount 0-50%

#### 3.8.2 Subscription Management
**FR-029: Đăng ký gói dịch vụ** [R]
- **Description**: Driver có thể đăng ký các gói dịch vụ
- **Actor**: Driver
- **Input**: Service Package ID
- **Process**:
  1. Check current subscription
  2. Calculate prorated amount (if upgrading)
  3. Process payment
  4. Activate subscription
- **Business Rules**:
  - Chỉ có 1 active subscription
  - Auto-renew trừ khi cancel
  - Downgrade chỉ có hiệu lực kỳ tiếp theo

### 3.9 Vehicle Management Module

**FR-030: Đăng ký xe điện** [R]
- **Description**: Driver có thể đăng ký thông tin xe điện
- **Actor**: Driver
- **Input**:
  - Make, Model, Year
  - License Plate (unique)
  - Battery Specifications
  - VIN Number (optional)
- **Business Rules**:
  - License plate phải unique
  - Một driver có thể có nhiều xe
  - Vehicle phải compatible với battery types trong hệ thống

**FR-031: Cập nhật thông tin xe** [R]
- **Description**: Driver cập nhật thông tin xe
- **Business Rules**: Không thể sửa license plate sau khi tạo

### 3.10 Support Ticket Module

#### 3.10.1 Ticket Management
**FR-032: Tạo ticket hỗ trợ** [R]
- **Description**: Driver có thể tạo ticket để được hỗ trợ
- **Actor**: Driver
- **Input**:
  - Category (TECHNICAL, BILLING, GENERAL)
  - Priority (LOW, MEDIUM, HIGH)
  - Subject, Description
  - Related Transaction ID (optional)
- **Output**: Support ticket với unique ID
- **Business Rules**:
  - Auto-assign category dựa trên keywords
  - HIGH priority cho billing issues

**FR-033: Phản hồi ticket** [R]
- **Description**: Staff có thể phản hồi và giải quyết ticket
- **Actor**: Staff, Admin
- **Input**: Ticket ID + Response message
- **Status Transitions**:
  - OPEN → IN_PROGRESS (khi staff bắt đầu xử lý)
  - IN_PROGRESS → RESOLVED (khi giải quyết xong)
  - RESOLVED → CLOSED (sau 48h hoặc customer confirm)
  - Any → ESCALATED (khi cần escalate)

**FR-034: Theo dõi ticket** [R]
- **Description**: Driver theo dõi trạng thái ticket của mình
- **Output**: Ticket details, response history, current status

#### 3.10.2 Ticket Analytics
**FR-035: Báo cáo ticket** [R]
- **Description**: Admin xem analytics về support tickets
- **Metrics**:
  - Resolution time average
  - Ticket volume by category
  - Staff performance
  - Customer satisfaction (future)

### 3.11 Reporting & Analytics Module

**FR-036: Dashboard tổng quan** [R]
- **Description**: Admin xem dashboard tổng quan hệ thống
- **Metrics**:
  - Daily/Monthly transaction volume
  - Revenue analytics
  - Station utilization rates
  - Battery health statistics
  - User growth metrics

**FR-037: Báo cáo chi tiết** [R]
- **Description**: Generate các báo cáo chi tiết theo yêu cầu
- **Report Types**:
  - Financial reports
  - Operations reports  
  - User activity reports
  - Battery lifecycle reports

---

## 4. YÊU CẦU PHI CHỨC NĂNG

### 4.1 Performance Requirements

#### 4.1.1 Response Time
**NFR-001: API Response Time** [R]
- **Requirement**: Thời gian phản hồi API <= 2 giây cho 95% requests
- **Measurement**: Measured from request initiation to complete response
- **Critical APIs**: Authentication, booking, payment processing
- **Target Metrics**:
  - Simple GET requests: <= 500ms
  - Complex queries (with joins): <= 1.5s
  - Payment processing: <= 3s
  - File uploads: <= 5s

**NFR-002: Database Performance** [R]
- **Requirement**: Database query time <= 1 giây
- **Optimization**: 
  - Proper indexing on frequently queried fields
  - Query optimization và execution plan analysis
  - Connection pooling (min: 5, max: 20 connections)

#### 4.1.2 Throughput
**NFR-003: Concurrent Users** [R]
- **Requirement**: Hỗ trợ tối thiểu 1000 concurrent users
- **Peak Load**: 2000 users during rush hours (7-9 AM, 5-7 PM)
- **Load Distribution**:
  - 70% read operations (browsing stations, viewing history)
  - 25% booking/transaction operations
  - 5% admin operations

**NFR-004: Transaction Processing** [R]
- **Requirement**: Xử lý tối thiểu 500 giao dịch/phút
- **Peak Capacity**: 1000 giao dịch/phút trong giờ cao điểm
- **Data Volume**: Support 100GB+ database với performance ổn định

#### 4.1.3 Availability & Reliability
**NFR-005: System Uptime** [R]
- **Requirement**: 99.9% uptime (tương đương 8.76 hours downtime/year)
- **Maintenance Window**: Scheduled maintenance 2-6 AM Sunday
- **Recovery Time**: Mean Time To Recovery (MTTR) <= 15 minutes
- **Monitoring**: 24/7 system monitoring với alerting

**NFR-006: Data Backup & Recovery** [R]
- **Backup Frequency**: 
  - Full backup: Daily at 2 AM
  - Incremental backup: Every 6 hours
  - Transaction log backup: Every 15 minutes
- **Recovery Point Objective (RPO)**: <= 15 minutes
- **Recovery Time Objective (RTO)**: <= 1 hour
- **Backup Retention**: 30 days online, 1 year archived

### 4.2 Security Requirements

#### 4.2.1 Authentication & Authorization
**NFR-007: Authentication Security** [R]
- **Password Policy**:
  - Minimum 8 characters
  - Must contain uppercase, lowercase, number
  - Special characters recommended
  - Password hashing sử dụng BCrypt với cost factor 12
- **Account Lockout**: 5 failed attempts → lock 15 minutes
- **Session Management**:
  - JWT access token: 15 minutes expiry
  - Refresh token: 7 days expiry
  - Automatic logout after 1 hour inactivity

**NFR-008: Authorization Control** [R]
- **Role-Based Access Control (RBAC)**:
  - Principle of least privilege
  - Clear role separation (Driver/Staff/Admin)
  - Resource-level permissions
- **API Security**:
  - All endpoints require authentication (except public endpoints)
  - Role-based endpoint access control
  - Rate limiting: 100 requests/minute per user

#### 4.2.2 Data Protection
**NFR-009: Data Encryption** [R]
- **In Transit**: All communication via HTTPS/TLS 1.3
- **At Rest**: Database encryption for sensitive fields
- **Sensitive Data**:
  - Passwords: BCrypt hashed
  - Payment info: PCI DSS compliance
  - Personal data: AES-256 encryption

**NFR-010: Input Security** [R]
- **Input Validation**: All user inputs validated và sanitized
- **SQL Injection Prevention**: JPA/Hibernate parameterized queries
- **XSS Prevention**: Output encoding for all dynamic content
- **CSRF Protection**: CSRF tokens for state-changing operations

#### 4.2.3 Compliance & Privacy
**NFR-011: Data Privacy** [R]
- **Personal Data Protection**: Comply với Vietnamese data protection laws
- **Data Minimization**: Chỉ collect data cần thiết
- **Right to Delete**: Support data deletion requests
- **Audit Trail**: Log all access to sensitive data

### 4.3 Scalability Requirements

#### 4.3.1 Horizontal Scaling
**NFR-012: Application Scaling** [R]
- **Stateless Design**: Application không lưu state locally
- **Load Balancing**: Support multiple application instances
- **Auto-scaling**: Scale based on CPU/memory usage
- **Container Support**: Docker containerization ready

#### 4.3.2 Database Scaling
**NFR-013: Database Optimization** [R]
- **Indexing Strategy**: 
  - Primary keys, foreign keys automatically indexed
  - Composite indexes for multi-column queries
  - Regular index maintenance và optimization
- **Query Optimization**:
  - Avoid N+1 query problems
  - Use pagination for large datasets
  - Implement query caching where appropriate

**NFR-014: Caching Strategy** [R]
- **Application-level Caching**: 
  - Station information (TTL: 1 hour)
  - Battery availability (TTL: 5 minutes)
  - User session data (TTL: 15 minutes)
- **Database Caching**: Query result caching
- **CDN**: Static content delivery optimization

#### 4.3.3 Storage Scaling
**NFR-015: Data Growth** [R]
- **Database Growth**: Support 10% monthly growth
- **File Storage**: Scalable file storage for images, documents
- **Archival Strategy**: Archive old transaction data (>2 years)

### 4.4 Usability Requirements

#### 4.4.1 User Experience
**NFR-016: API Usability** [R]
- **RESTful Design**: Follow REST principles
- **Consistent Response Format**: Standardized JSON response structure
- **Error Messages**: Clear, actionable error messages
- **API Documentation**: Comprehensive Swagger/OpenAPI documentation

**NFR-017: Response Format** [R]
- **Standard Response Structure**:
```json
{
  "success": boolean,
  "data": object/array,
  "message": string,
  "error": {
    "code": string,
    "details": string
  },
  "pagination": {
    "page": number,
    "size": number,
    "total": number
  }
}
```

#### 4.4.2 Internationalization
**NFR-018: Multi-language Support** [O]
- **Primary Language**: Vietnamese
- **Secondary Language**: English (future enhancement)
- **Message Localization**: Error messages, notifications
- **Data Format**: Vietnamese date/time, currency formats

### 4.5 Compatibility Requirements

#### 4.5.1 Platform Compatibility
**NFR-019: Java Compatibility** [R]
- **Java Version**: Java 17 LTS
- **Spring Boot Version**: 3.5.6
- **Backward Compatibility**: Support previous minor versions

#### 4.5.2 Database Compatibility
**NFR-020: Database Requirements** [R]
- **Primary Database**: Microsoft SQL Server 2019+
- **Connection**: JDBC driver compatibility
- **Features**: Support for transactions, stored procedures

#### 4.5.3 Integration Compatibility
**NFR-021: API Compatibility** [R]
- **REST API**: HTTP/1.1 và HTTP/2 support
- **Data Format**: JSON primary, XML support optional
- **Client Support**: Mobile apps, web applications, third-party integrations

### 4.6 Maintainability Requirements

#### 4.6.1 Code Quality
**NFR-022: Code Standards** [R]
- **Coding Standards**: Follow Java coding conventions
- **Documentation**: Javadoc for all public methods
- **Test Coverage**: Minimum 80% code coverage
- **Static Analysis**: SonarQube quality gates

#### 4.6.2 Monitoring & Logging
**NFR-023: Logging Requirements** [R]
- **Log Levels**: ERROR, WARN, INFO, DEBUG
- **Structured Logging**: JSON format for easy parsing
- **Log Retention**: 30 days application logs, 90 days audit logs
- **Sensitive Data**: Never log passwords, payment info

**NFR-024: Monitoring** [R]
- **Application Metrics**: Response times, error rates, throughput
- **System Metrics**: CPU, memory, disk usage
- **Business Metrics**: Transaction volume, user activity
- **Alerting**: Email/SMS alerts for critical issues

#### 4.6.3 Deployment & DevOps
**NFR-025: Deployment** [R]
- **Environment Support**: Development, Testing, Production
- **Configuration Management**: Externalized configuration
- **Zero-downtime Deployment**: Blue-green deployment capability
- **Rollback Strategy**: Quick rollback trong trường hợp có issues

### 4.7 Legal & Regulatory Requirements

#### 4.7.1 Compliance Requirements
**NFR-026: Business Compliance** [R]
- **Vietnamese Law**: Comply with Vietnamese business regulations
- **Tax Compliance**: VAT calculation và reporting
- **Financial Regulations**: Money handling regulations

#### 4.7.2 Data Protection Compliance
**NFR-027: Privacy Regulations** [R]
- **Data Protection**: Vietnamese Personal Data Protection Decree
- **Data Retention**: Clear data retention policies
- **Consent Management**: User consent for data processing
- **Data Portability**: Export user data upon request

### 4.8 Environmental Requirements

#### 4.8.1 Operating Environment
**NFR-028: Production Environment** [R]
- **Operating System**: Linux (Ubuntu 20.04+ or CentOS 8+)
- **Hardware Requirements**:
  - Minimum: 4 CPU cores, 8GB RAM, 100GB SSD
  - Recommended: 8 CPU cores, 16GB RAM, 500GB SSD
- **Network**: Stable internet connection với minimum 100 Mbps

#### 4.8.2 Development Environment
**NFR-029: Development Setup** [R]
- **IDE Support**: IntelliJ IDEA, Eclipse, VS Code
- **Build Tools**: Maven 3.8+
- **Testing**: JUnit 5, Mockito, TestContainers
- **Local Database**: SQL Server hoặc H2 for testing

---

## 5. KIẾN TRÚC HỆ THỐNG

### 5.1 Tổng quan kiến trúc

#### 5.1.1 Kiến trúc tổng thể (High-Level Architecture)
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Mobile App    │    │   Web Client    │    │  Admin Portal   │
│   (Flutter/RN)  │    │    (React)      │    │    (Angular)    │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │     Load Balancer       │
                    │      (Nginx/HAProxy)    │
                    └────────────┬────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
    ┌─────▼─────┐          ┌─────▼─────┐          ┌─────▼─────┐
    │  API GW   │          │  API GW   │          │  API GW   │
    │Instance-1 │          │Instance-2 │          │Instance-3 │
    └─────┬─────┘          └─────┬─────┘          └─────┬─────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
    ┌────────────────────────────▼────────────────────────────┐
    │              Spring Boot Backend Cluster                │
    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
    │  │   App-1     │  │    App-2    │  │    App-3    │    │
    │  │  (Primary)  │  │  (Secondary)│  │ (Secondary) │    │
    │  └─────────────┘  └─────────────┘  └─────────────┘    │
    └────────────────────────────┬───────────────────────────┘
                                 │
    ┌────────────────────────────▼───────────────────────────┐
    │                  Data Layer                            │
    │  ┌─────────────────────────────────────────────────┐  │
    │  │         Microsoft SQL Server Cluster           │  │
    │  │    ┌─────────────┐    ┌─────────────┐         │  │
    │  │    │   Primary   │───▶│  Secondary  │         │  │
    │  │    │  Database   │    │ (Read Only) │         │  │
    │  │    └─────────────┘    └─────────────┘         │  │
    │  └─────────────────────────────────────────────────┘  │
    │                                                        │
    │  ┌─────────────────────────────────────────────────┐  │
    │  │            Caching Layer (Redis)                │  │
    │  └─────────────────────────────────────────────────┘  │
    │                                                        │
    │  ┌─────────────────────────────────────────────────┐  │
    │  │         File Storage (MinIO/AWS S3)             │  │
    │  └─────────────────────────────────────────────────┘  │
    └────────────────────────────────────────────────────────┘
```

#### 5.1.2 Layered Architecture (Chi tiết Backend)
```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │ Controllers │ │   Filters   │ │   Exception Handler │   │
│  │   (REST)    │ │  (CORS,     │ │    (Global Error    │   │
│  │             │ │  Security)  │ │     Handling)       │   │
│  └─────────────┘ └─────────────┘ └─────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                     Business Layer                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │   Service   │ │ Validation  │ │   Business Logic    │   │
│  │   Classes   │ │   Rules     │ │    (Workflows)      │   │
│  │             │ │             │ │                     │   │
│  └─────────────┘ └─────────────┘ └─────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                   Data Access Layer                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │ Repositories│ │    JPA      │ │     Entities        │   │
│  │ (Interface) │ │ Implementation │ │   (Domain Model)    │   │
│  │             │ │             │ │                     │   │
│  └─────────────┘ └─────────────┘ └─────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Infrastructure Layer                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │   Database  │ │   Caching   │ │   External Services │   │
│  │(SQL Server) │ │   (Redis)   │ │  (Payment Gateway)  │   │
│  │             │ │             │ │                     │   │
│  └─────────────┘ └─────────────┘ └─────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Technology Stack

#### 5.2.1 Core Technologies
**Backend Framework:**
- **Spring Boot 3.5.6**: Main application framework
- **Java 17**: Programming language (LTS version)
- **Maven 3.8+**: Build và dependency management

**Database:**
- **Microsoft SQL Server 2019+**: Primary database
- **HikariCP**: Connection pooling
- **Spring Data JPA**: ORM framework
- **Hibernate 6.2+**: JPA implementation

#### 5.2.2 Security & Authentication
**Security Framework:**
- **Spring Security 6**: Authentication và authorization
- **JWT (JSON Web Token)**: Stateless authentication
- **BCrypt**: Password hashing
- **HTTPS/TLS 1.3**: Encrypted communication

**Additional Security:**
- **OWASP**: Security best practices
- **Rate Limiting**: Request throttling
- **Input Validation**: Bean Validation (JSR-303)

#### 5.2.3 API & Documentation
**API Design:**
- **RESTful Architecture**: HTTP-based REST APIs
- **OpenAPI 3**: API specification
- **Swagger UI**: Interactive API documentation
- **JSON**: Primary data exchange format

**API Standards:**
- **Richardson Maturity Model Level 2**
- **HTTP Status Codes**: Proper usage
- **Content Negotiation**: Multiple formats support
- **Versioning**: URL-based versioning (/api/v1/)

#### 5.2.4 Monitoring & Observability
**Application Monitoring:**
- **Spring Boot Actuator**: Application metrics
- **Micrometer**: Metrics collection
- **Logback**: Logging framework
- **Structured Logging**: JSON format logs

**Health Checking:**
- **Health Endpoints**: /actuator/health
- **Custom Health Indicators**: Database, external services
- **Readiness/Liveness Probes**: Kubernetes-ready

#### 5.2.5 Testing Framework
**Unit Testing:**
- **JUnit 5**: Test framework
- **Mockito**: Mocking framework
- **AssertJ**: Fluent assertions
- **TestContainers**: Integration testing với real database

**API Testing:**
- **MockMvc**: Spring MVC testing
- **REST Assured**: API testing
- **WireMock**: External service mocking

### 5.3 Project Structure

#### 5.3.1 Source Code Organization
```
src/
├── main/
│   ├── java/com/evbs/BackEndEvBs/
│   │   ├── BackEndEvBsApplication.java          # Main application class
│   │   │
│   │   ├── config/                              # Configuration classes
│   │   │   ├── CORSConfig.java                  # Cross-origin configuration
│   │   │   ├── DatabaseInitializer.java         # Database initialization
│   │   │   ├── SecurityConfig.java              # Security configuration
│   │   │   ├── ModelMapperConfig.java           # Object mapping configuration
│   │   │   └── Filter.java                      # Custom filters
│   │   │
│   │   ├── controller/                          # REST API Controllers
│   │   │   ├── AuthenticationController.java    # Authentication endpoints
│   │   │   ├── UserController.java              # User management
│   │   │   ├── StationController.java           # Station operations
│   │   │   ├── BatteryController.java           # Battery management
│   │   │   ├── BookingController.java           # Booking operations
│   │   │   ├── SwapTransactionController.java   # Transaction processing
│   │   │   ├── PaymentController.java           # Payment handling
│   │   │   ├── ServicePackageController.java    # Service packages
│   │   │   ├── VehicleController.java           # Vehicle management
│   │   │   ├── SupportTicketController.java     # Customer support
│   │   │   ├── StationInventoryController.java  # Inventory management
│   │   │   ├── BatteryHistoryController.java    # Battery tracking
│   │   │   ├── DriverSubscriptionController.java # Subscriptions
│   │   │   ├── TicketResponseController.java    # Support responses
│   │   │   └── AdminUserController.java         # Admin operations
│   │   │
│   │   ├── entity/                              # JPA Entities (Domain Model)
│   │   │   ├── User.java                        # User entity với roles
│   │   │   ├── Station.java                     # Battery swap station
│   │   │   ├── Battery.java                     # Battery information
│   │   │   ├── StationInventory.java            # Station battery inventory
│   │   │   ├── Vehicle.java                     # Customer vehicles
│   │   │   ├── Booking.java                     # Service bookings
│   │   │   ├── SwapTransaction.java             # Battery swap transactions
│   │   │   ├── Payment.java                     # Payment records
│   │   │   ├── ServicePackage.java              # Service plan packages
│   │   │   ├── DriverSubscription.java          # User subscriptions
│   │   │   ├── SupportTicket.java               # Support tickets
│   │   │   ├── TicketResponse.java              # Ticket responses
│   │   │   └── BatteryHistory.java              # Battery usage history
│   │   │
│   │   ├── repository/                          # Data Access Layer
│   │   │   ├── UserRepository.java              # User data operations
│   │   │   ├── StationRepository.java           # Station data access
│   │   │   ├── BatteryRepository.java           # Battery data management
│   │   │   ├── StationInventoryRepository.java  # Inventory queries
│   │   │   ├── VehicleRepository.java           # Vehicle data access
│   │   │   ├── BookingRepository.java           # Booking queries
│   │   │   ├── SwapTransactionRepository.java   # Transaction data
│   │   │   ├── PaymentRepository.java           # Payment queries
│   │   │   ├── ServicePackageRepository.java    # Package data
│   │   │   ├── DriverSubscriptionRepository.java # Subscription data
│   │   │   ├── SupportTicketRepository.java     # Support data
│   │   │   ├── TicketResponseRepository.java    # Response data
│   │   │   └── BatteryHistoryRepository.java    # History tracking
│   │   │
│   │   ├── service/                             # Business Logic Layer
│   │   │   ├── AuthenticationService.java       # Auth business logic
│   │   │   ├── UserService.java                 # User operations
│   │   │   ├── StationService.java              # Station management
│   │   │   ├── BatteryService.java              # Battery operations
│   │   │   ├── StationInventoryService.java     # Inventory management
│   │   │   ├── VehicleService.java              # Vehicle operations
│   │   │   ├── BookingService.java              # Booking logic
│   │   │   ├── SwapTransactionService.java      # Transaction processing
│   │   │   ├── PaymentService.java              # Payment processing
│   │   │   ├── ServicePackageService.java       # Package management
│   │   │   ├── DriverSubscriptionService.java   # Subscription logic
│   │   │   ├── SupportTicketService.java        # Support operations
│   │   │   ├── TicketResponseService.java       # Response handling
│   │   │   └── BatteryHistoryService.java       # History tracking
│   │   │
│   │   ├── model/                               # Data Transfer Objects
│   │   │   ├── request/                         # Request DTOs
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── StationRequest.java
│   │   │   │   ├── BatteryRequest.java
│   │   │   │   ├── BookingRequest.java
│   │   │   │   └── ...
│   │   │   └── response/                        # Response DTOs
│   │   │       ├── LoginResponse.java
│   │   │       ├── UserResponse.java
│   │   │       ├── ApiResponse.java
│   │   │       └── ...
│   │   │
│   │   └── exception/                           # Exception Handling
│   │       ├── GlobalExceptionHandler.java      # Central error handling
│   │       └── exceptions/                      # Custom exceptions
│   │           ├── AuthenticationException.java
│   │           ├── NotFoundException.java
│   │           ├── ValidationException.java
│   │           └── BusinessException.java
│   │
│   └── resources/
│       ├── application.properties               # Main configuration
│       ├── application-dev.properties           # Development config
│       ├── application-prod.properties          # Production config
│       ├── static/                              # Static resources
│       └── templates/                           # Email templates
│
└── test/                                        # Test classes
    ├── java/com/evbs/BackEndEvBs/
    │   ├── controller/                          # Controller tests
    │   ├── service/                             # Service tests
    │   ├── repository/                          # Repository tests
    │   └── integration/                         # Integration tests
    └── resources/
        ├── application-test.properties          # Test configuration
        └── test-data/                           # Test data files
```

#### 5.3.2 Configuration Structure
```
src/main/resources/
├── application.properties                       # Base configuration
├── application-dev.properties                   # Development environment
├── application-test.properties                  # Testing environment  
├── application-prod.properties                  # Production environment
├── logback-spring.xml                          # Logging configuration
└── db/
    ├── migration/                              # Database migrations
    └── seeds/                                  # Seed data
```

### 5.4 Domain Model Design

#### 5.4.1 Core Entities Overview

**User Management:**
- `User`: Central user entity với role-based permissions
- `Vehicle`: User's registered vehicles

**Station & Inventory:**
- `Station`: Physical swap stations
- `Battery`: Individual battery units
- `StationInventory`: Battery inventory per station
- `BatteryHistory`: Complete battery lifecycle tracking

**Business Operations:**
- `Booking`: Service reservations
- `SwapTransaction`: Actual battery swap operations
- `Payment`: Financial transactions
- `ServicePackage`: Subscription plans
- `DriverSubscription`: User subscriptions

**Support:**
- `SupportTicket`: Customer support requests
- `TicketResponse`: Support team responses

#### 5.4.2 Entity Relationships Design

```
User (1) ←→ (M) Vehicle
User (1) ←→ (M) Booking  
User (1) ←→ (M) SwapTransaction (as Driver)
User (1) ←→ (M) SwapTransaction (as Staff)
User (1) ←→ (M) DriverSubscription
User (1) ←→ (M) SupportTicket

Station (1) ←→ (M) StationInventory
Station (1) ←→ (M) Booking
Station (1) ←→ (M) SwapTransaction

Battery (1) ←→ (M) StationInventory
Battery (1) ←→ (M) SwapTransaction  
Battery (1) ←→ (M) BatteryHistory

ServicePackage (1) ←→ (M) DriverSubscription

Booking (1) ←→ (1) SwapTransaction
SwapTransaction (1) ←→ (1) Payment

SupportTicket (1) ←→ (M) TicketResponse
```

#### 5.4.3 Entity Status Management

**Enumerated Status Values:**

```java
// User statuses
enum UserStatus { ACTIVE, INACTIVE, SUSPENDED }

// Battery statuses  
enum BatteryStatus { AVAILABLE, IN_USE, MAINTENANCE, FAULTY }

// Booking statuses
enum BookingStatus { PENDING, CONFIRMED, COMPLETED, CANCELLED }

// Payment statuses
enum PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }

// Subscription statuses
enum SubscriptionStatus { ACTIVE, EXPIRED, CANCELLED }

// Station statuses
enum StationStatus { ACTIVE, INACTIVE, MAINTENANCE }

// Ticket statuses  
enum TicketStatus { OPEN, IN_PROGRESS, RESOLVED, CLOSED, ESCALATED }
```

### 5.5 API Design Principles

#### 5.5.1 RESTful API Standards

**URL Design:**
- Resource-based URLs: `/api/v1/stations`, `/api/v1/batteries`
- Hierarchical relationships: `/api/v1/stations/{id}/batteries`
- Query parameters for filtering: `?status=ACTIVE&page=1&size=10`

**HTTP Methods:**
- `GET`: Retrieve resources (idempotent)
- `POST`: Create new resources
- `PUT`: Update entire resource (idempotent)
- `PATCH`: Partial resource update
- `DELETE`: Remove resources

**Response Codes:**
- `200`: Success with response body
- `201`: Created successfully
- `204`: Success without response body
- `400`: Bad request (validation errors)
- `401`: Unauthorized (authentication required)
- `403`: Forbidden (insufficient permissions)
- `404`: Resource not found
- `409`: Conflict (business rule violation)
- `500`: Internal server error

#### 5.5.2 Request/Response Format

**Standard Request Format:**
```json
{
  "data": {
    // Actual request payload
  },
  "metadata": {
    "requestId": "uuid",
    "timestamp": "2025-10-14T10:30:00Z"
  }
}
```

**Standard Response Format:**
```json
{
  "success": true,
  "data": {
    // Response payload
  },
  "message": "Operation completed successfully",
  "metadata": {
    "requestId": "uuid", 
    "timestamp": "2025-10-14T10:30:00Z",
    "version": "2.0"
  },
  "pagination": {
    "page": 1,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

**Error Response Format:**
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Input validation failed",
    "details": [
      {
        "field": "email",
        "message": "Email format is invalid",
        "rejectedValue": "invalid-email"
      }
    ]
  },
  "metadata": {
    "requestId": "uuid",
    "timestamp": "2025-10-14T10:30:00Z"
  }
}
```

---

## 6. API ENDPOINTS

### 6.1 Authentication
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/refresh` - Refresh token

### 6.2 User Management
- `GET /api/admin/users` - Lấy danh sách user (Admin)
- `PUT /api/admin/users/{id}` - Cập nhật user (Admin)
- `DELETE /api/admin/users/{id}` - Xóa user (Admin)

### 6.3 Station Management
- `GET /api/station` - Lấy danh sách trạm (Public)
- `GET /api/station/{id}` - Chi tiết trạm (Public)
- `POST /api/station` - Tạo trạm (Admin/Staff)
- `PUT /api/station/{id}` - Cập nhật trạm (Admin/Staff)
- `DELETE /api/station/{id}` - Xóa trạm (Admin)

### 6.4 Battery Management
- `GET /api/battery` - Danh sách pin (Admin/Staff)
- `GET /api/battery/available` - Pin có sẵn (Public)
- `POST /api/battery` - Tạo pin (Admin/Staff)
- `PUT /api/battery/{id}` - Cập nhật pin (Admin/Staff)
- `DELETE /api/battery/{id}` - Xóa pin (Admin)

### 6.5 Station Inventory
- `GET /api/station-inventory` - Tất cả inventory (Admin/Staff)
- `GET /api/station-inventory/station/{id}` - Inventory theo trạm (Public)
- `POST /api/station-inventory` - Thêm pin vào trạm (Admin/Staff)

### 6.6 Booking
- `POST /api/booking` - Đặt lịch (Driver)
- `GET /api/booking/my` - Lịch đặt của tôi (Driver)
- `PUT /api/booking/{id}/cancel` - Hủy đặt lịch (Driver)
- `GET /api/booking/station/{id}` - Đặt lịch theo trạm (Staff)

### 6.7 Swap Transaction
- `POST /api/swap-transaction` - Tạo giao dịch (Staff)
- `GET /api/swap-transaction/my` - Giao dịch của tôi (Driver)
- `GET /api/swap-transaction/station/{id}` - Giao dịch theo trạm (Staff)

### 6.8 Payment
- `POST /api/payment` - Tạo thanh toán (Driver)
- `GET /api/payment/my` - Thanh toán của tôi (Driver)
- `PUT /api/payment/{id}/status` - Cập nhật trạng thái (Admin/Staff)

---

## 7. CƠ SỞ DỮ LIỆU

### 7.1 ERD chính
```
User ||--o{ Vehicle : owns
User ||--o{ Booking : makes
User ||--o{ SwapTransaction : performs
User ||--o{ DriverSubscription : subscribes
User ||--o{ SupportTicket : creates

Station ||--o{ StationInventory : contains
Station ||--o{ Booking : receives
Station ||--o{ SwapTransaction : processes

Battery ||--o{ StationInventory : stored_in
Battery ||--o{ SwapTransaction : used_in
Battery ||--o{ BatteryHistory : has_history

ServicePackage ||--o{ DriverSubscription : subscribed_to
Booking ||--|| SwapTransaction : results_in
SwapTransaction ||--|| Payment : requires
```

### 7.2 Trạng thái entities
- **User Status**: ACTIVE, INACTIVE, SUSPENDED
- **Battery Status**: AVAILABLE, IN_USE, MAINTENANCE, FAULTY
- **Booking Status**: PENDING, CONFIRMED, COMPLETED, CANCELLED
- **Payment Status**: PENDING, COMPLETED, FAILED, REFUNDED
- **Subscription Status**: ACTIVE, EXPIRED, CANCELLED

---

## 8. BẢO MẬT

### 8.1 Authentication & Authorization
- JWT-based authentication với access token và refresh token
- Role-based access control (RBAC)
- Password hashing sử dụng BCrypt

### 8.2 Data Protection
- HTTPS cho tất cả communications
- Input validation và sanitization
- SQL injection prevention thông qua JPA
- CORS configuration

---

## 9. KIỂM THỬ

### 9.1 Unit Testing
- Test coverage cho service layer
- Mock external dependencies
- Validation testing

### 9.2 Integration Testing
- API endpoint testing
- Database integration testing
- Security testing

### 9.3 Performance Testing
- Load testing cho concurrent users
- Stress testing cho peak usage
- Database performance testing

---

## 10. TRIỂN KHAI

### 10.1 Environment Requirements
- **Production**: Cloud deployment (AWS/Azure)
- **Database**: Microsoft SQL Server
- **Java Version**: Java 17+
- **Memory**: Minimum 2GB RAM

### 10.2 Configuration
- Environment-specific application.properties
- Database connection pooling
- Logging configuration
- Security configurations

---

## 11. BẢO TRÌ VÀ HỖ TRỢ

### 11.1 Monitoring
- Application performance monitoring
- Database performance tracking
- Error logging và alerting

### 11.2 Backup Strategy
- Daily database backups
- File system backups
- Disaster recovery plan

---

## 12. FUTURE ENHANCEMENTS

### 12.1 Planned Features
- Real-time battery status monitoring
- Mobile app integration
- IoT integration với battery sensors
- Advanced analytics và reporting
- Multi-language support

### 12.2 Scalability Improvements
- Microservices architecture
- Caching layer (Redis)
- Load balancing
- CDN integration

---

**Version**: 1.0  
**Date**: October 14, 2025  
**Author**: EVBatterySwapStationManagementSystem Team  
**Status**: Draft