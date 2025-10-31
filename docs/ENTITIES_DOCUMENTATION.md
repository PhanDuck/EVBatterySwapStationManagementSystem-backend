# Entity Documentation — EVBatterySwapStationManagementSystem-backend

Ngày: 2025-10-31

Mục tiêu: tập hợp thành 1 file tài liệu (doc) liệt kê các lớp entity hiện có trong project, đường dẫn file, và so sánh nhanh với bảng schema bạn gửi (Appointment, Blog, Doctor, ...). Tệp này ở dạng Markdown để bạn dễ đọc hoặc chuyển sang .docx nếu cần.

---

## 1) Tổng quan nhanh
- Project hiện là backend Spring Boot cho hệ thống đổi pin EV (không phải hệ thống y tế). Vì vậy nhiều bảng bạn đưa (ví dụ: Appointment, EggGained, Embryo...) KHÔNG có trong project.
- File tài liệu này liệt kê entity hiện có và đánh dấu các bảng trong yêu cầu là "Found" hoặc "Missing".

## 2) Entities hiện có (tên lớp và đường dẫn)
- `Battery` — src/main/java/com/evbs/BackEndEvBs/entity/Battery.java
- `BatteryType` — src/main/java/com/evbs/BackEndEvBs/entity/BatteryType.java
- `Booking` — src/main/java/com/evbs/BackEndEvBs/entity/Booking.java
- `DriverSubscription` — src/main/java/com/evbs/BackEndEvBs/entity/DriverSubscription.java
- `Payment` — src/main/java/com/evbs/BackEndEvBs/entity/Payment.java
- `ServicePackage` — src/main/java/com/evbs/BackEndEvBs/entity/ServicePackage.java
- `StaffStationAssignment` — src/main/java/com/evbs/BackEndEvBs/entity/StaffStationAssignment.java
- `Station` — src/main/java/com/evbs/BackEndEvBs/entity/Station.java
- `StationInventory` — src/main/java/com/evbs/BackEndEvBs/entity/StationInventory.java
- `SupportTicket` — src/main/java/com/evbs/BackEndEvBs/entity/SupportTicket.java
- `SwapTransaction` — src/main/java/com/evbs/BackEndEvBs/entity/SwapTransaction.java
- `TicketResponse` — src/main/java/com/evbs/BackEndEvBs/entity/TicketResponse.java
- `User` — src/main/java/com/evbs/BackEndEvBs/entity/User.java
- `Vehicle` — src/main/java/com/evbs/BackEndEvBs/entity/Vehicle.java

> Ghi chú: Một file `UserProfile` được tạo tạm trước đó nhưng đã bị revert theo lịch sử bạn thông báo — hiện file đó không tồn tại.

## 3) Repositories chính (đường dẫn)
- `BatteryRepository` — src/main/java/com/evbs/BackEndEvBs/repository/BatteryRepository.java
- `BatteryTypeRepository` — src/main/java/com/evbs/BackEndEvBs/repository/BatteryTypeRepository.java
- `BookingRepository` — src/main/java/com/evbs/BackEndEvBs/repository/BookingRepository.java
- `DriverSubscriptionRepository` — src/main/java/com/evbs/BackEndEvBs/repository/DriverSubscriptionRepository.java
- `PaymentRepository` — src/main/java/com/evbs/BackEndEvBs/repository/PaymentRepository.java
- `ServicePackageRepository` — src/main/java/com/evbs/BackEndEvBs/repository/ServicePackageRepository.java
- `StationRepository` — src/main/java/com/evbs/BackEndEvBs/repository/StationRepository.java
- `StationInventoryRepository` — src/main/java/com/evbs/BackEndEvBs/repository/StationInventoryRepository.java
- `SupportTicketRepository` — src/main/java/com/evbs/BackEndEvBs/repository/SupportTicketRepository.java
- `SwapTransactionRepository` — src/main/java/com/evbs/BackEndEvBs/repository/SwapTransactionRepository.java
- `TicketResponseRepository` — src/main/java/com/evbs/BackEndEvBs/repository/TicketResponseRepository.java
- `UserRepository` — src/main/java/com/evbs/BackEndEvBs/repository/UserRepository.java
- `VehicleRepository` — src/main/java/com/evbs/BackEndEvBs/repository/VehicleRepository.java

## 4) So sánh với danh sách bảng bạn cung cấp
Bên dưới là các bảng trong yêu cầu của bạn và trạng thái (Found = đã có entity tương ứng, Missing = chưa có):

- Appointment — Missing
- Blog — Missing
- Doctor — Missing
- DoctorSchedule — Missing
- EggGained — Missing
- EmbryoGained — Missing
- EmbryoTransfer — Missing
- Feedback — Missing
- MedicalExamination — Missing
- Order — Missing (project có `Booking`/`Payment` nhưng không phải "Order" theo schema bạn gửi)
- OrderStep — Missing
- orderStepPayments — Missing
- Patient — Missing
- Prescription — Missing
- PrescriptionItem — Missing
- Slot — Missing
- TreatmentService — Missing
- TreatmentStep — Missing
- UserProfile — Missing (was previously added then reverted)

Tóm lại: hầu hết các bảng y tế bạn liệt kê không có trong project (project hiện tập trung vào pin/booking/station).

## 5) Gợi ý tiếp theo (tùy bạn chọn)
- Nếu bạn muốn tôi tạo các entity tương ứng với schema (ví dụ `Appointment`, `Doctor`, `UserProfile`, ...), tôi có thể:
  - tạo các lớp JPA trong `src/main/java/com/evbs/BackEndEvBs/entity/` theo schema bạn gửi,
  - tạo các `Repository` interface tương ứng,
  - nếu cần, tạo migration SQL (file .sql) để áp lên DB.

- Nếu bạn chỉ cần file doc (Word/Excel): tôi có thể xuất Markdown này thành `.docx` hoặc `.xlsx` và thêm bảng chi tiết từng cột (Field name, Type, Size, Unique, Not Null, PK/FK, Notes) theo đúng schema bạn gửi.

## 6) Muốn xuất định dạng nào?
Chọn một trong các tuỳ chọn sau để tôi tiếp tục:

1) Tạo đầy đủ các class entity + repositories cho toàn bộ schema (Appointment, Blog, Doctor, ...). Tôi sẽ tạo và chạy build/tests. (Action: code changes)
2) Chỉ xuất tài liệu chi tiết sang `.docx` (Word) hoặc `.xlsx` (Excel) chứa các bảng trường như attachments bạn cung cấp. (Action: tạo file docs/ENTITIES_DOCUMENTATION.docx hoặc .xlsx)
3) Xuất toàn bộ danh sách file hiện có ra CSV để bạn tải.

Trả lời với số lựa chọn (1/2/3) hoặc mô tả khác bạn muốn.