# Database Tables — Detailed Schema

Ngày: 2025-10-31

Tài liệu này mô tả chi tiết các bảng (fields, types, sizes, unique, not null, PK/FK, notes) theo đúng định dạng ví dụ bạn gửi (UserProfile attachment). Bạn có thể dùng file này làm tài liệu hoặc xuất sang Word/Excel.

---

## Table: Appointment

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | uniqueidentifier |  | Yes | Yes | PK | |
| 2 | PatientId | uniqueidentifier |  |  | Yes | FK | |
| 3 | DoctorId | uniqueidentifier |  |  | Yes | FK | |
| 4 | DoctorScheduleId | bigint |  |  | Yes | FK | |
| 5 | TreatmentServiceId | uniqueidentifier |  |  |  | FK | |
| 6 | Type | int |  |  | Yes |  | |
| 7 | OrderStepId | bigint |  |  |  | FK | |
| 8 | AppointmentDate | date |  |  | Yes |  | |
| 9 | StartTime | time |  |  | Yes |  | |
| 10 | EndTime | time |  |  | Yes |  | |
| 11 | Status | nvarchar |  |  | Yes |  | |
| 12 | CancellationReason | nvarchar | 1000 |  |  |  | |
| 13 | Note | ntext |  |  |  |  | |
| 14 | ExtraFee | decimal |  |  |  |  | |
| 15 | PaymentStatus | nvarchar |  |  | Yes |  | |
| 16 | CreatedAt | datetime |  |  | Yes |  | |
| 17 | UpdatedAt | datetime |  |  |  |  | |

---

## Table: Blog

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | uniqueidentifier |  | Yes | Yes | PK | |
| 2 | UserProfileId | uniqueidentifier |  |  | Yes | FK | |
| 3 | Content | ntext |  |  | Yes |  | |
| 4 | Status | int |  |  | Yes |  | |
| 5 | CreatedAt | datetime |  |  | Yes |  | |
| 6 | UpdatedAt | datetime |  |  |  |  | |
| 7 | ImageUrl | nvarchar |  |  |  |  | |

---

## Table: Doctor

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | uniqueidentifier |  | Yes | Yes | PK | |
| 2 | UserProfileId | uniqueidentifier |  |  | Yes | FK | |
| 3 | Degree | nvarchar | 255 |  |  |  | |
| 4 | Specialization | ntext |  |  |  |  | |
| 5 | YearsOfExperience | int |  |  |  |  | |
| 6 | Biography | nvarchar | 2000 |  |  |  | |
| 7 | Rating | decimal | 3,2 |  |  |  | |
| 8 | PatientsServed | int |  |  |  |  | Number of patients served |
| 9 | UpdatedAt | datetime |  |  |  |  | |

---

## Table: DoctorSchedule

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | bigint |  | Yes | Yes | PK | |
| 2 | DoctorId | uniqueidentifier |  |  | Yes | FK | |
| 3 | WorkDate | date |  |  | Yes |  | |
| 4 | SlotId | bigint |  |  | Yes | FK | |
| 5 | MaxAppointments | int |  |  |  |  | |
| 6 | IsAcceptingPatients | bit |  |  | Yes |  | |
| 7 | Note | nvarchar | 1000 |  |  |  | |
| 8 | CreatedAt | datetime |  |  | Yes |  | |
| 9 | UpdatedAt | datetime |  |  |  |  | |

---

## Table: EggGained

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | bigint |  | Yes | Yes | PK | |
| 2 | Grade | int |  |  | Yes |  | |
| 3 | IsUsable | bit |  |  | Yes |  | |
| 4 | DateGained | date |  |  | Yes |  | |
| 5 | OrderId | uniqueidentifier |  |  | Yes | FK | |

---

## Table: EmbryoGained

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | bigint |  | Yes | Yes | PK | |
| 2 | Grade | int |  |  | Yes |  | |
| 3 | EggGainedId | bigint |  |  | Yes | FK | |
| 4 | EmbryoStatus | int |  |  | Yes |  | |
| 5 | IsViable | bit |  |  | Yes |  | |
| 6 | IsFrozen | bit |  |  | Yes |  | |
| 7 | IsTransfered | bit |  |  | Yes |  | |
| 8 | OrderId | uniqueidentifier |  |  | Yes | FK | |

---

## Table: EmbryoTransfer

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | bigint |  | Yes | Yes | PK | |
| 2 | EmbryoGainedId | bigint |  |  | Yes | FK | |
| 3 | TransferDate | datetime |  |  | Yes |  | |
| 4 | TransferType | int |  |  | Yes |  | |
| 5 | AppointmentId | uniqueidentifier |  |  |  | FK | |
| 6 | OrderId | uniqueidentifier |  |  | Yes | FK | |
| 7 | UpdatedAt | datetime |  |  |  |  | |

---

## Table: Feedback

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | uniqueidentifier |  | Yes | Yes | PK | |
| 2 | PatientId | uniqueidentifier |  |  | Yes | FK | |
| 3 | DoctorId | uniqueidentifier |  |  |  | FK | |
| 4 | TreatmentServiceId | uniqueidentifier |  |  |  | FK | |
| 5 | Status | bit |  |  | Yes |  | |
| 6 | Rating | decimal | 3,2 | Yes |  |  | |
| 7 | Comment | nvarchar |  |  |  |  | |
| 8 | CreatedAt | datetime |  |  | Yes |  | |
| 9 | UpdatedAt | datetime |  |  |  |  | |

---

## Table: MedicalExamination

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | bigint |  | Yes | Yes | PK | |
| 2 | AppointmentId | uniqueidentifier |  |  | Yes | FK | |
| 3 | Symptoms | nvarchar |  |  |  |  | |
| 4 | Diagnosis | nvarchar |  |  |  |  | |
| 5 | Indications | nvarchar |  |  |  |  | |
| 6 | Note | nvarchar |  |  |  |  | |

---

## Table: Order

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | uniqueidentifier |  | Yes | Yes | PK | |
| 2 | PatientId | uniqueidentifier |  |  | Yes | FK | |
| 3 | DoctorId | uniqueidentifier |  |  | Yes | FK | |
| 4 | TreatmentServiceId | uniqueidentifier |  |  | Yes | FK | |
| 5 | StartDate | date |  |  | Yes |  | |
| 6 | EndDate | date |  |  |  |  | |
| 7 | Status | int |  |  | Yes |  | |
| 8 | TotalAmount | decimal | 18,2 |  |  |  | |
| 9 | TotalEgg | bigint |  |  |  |  | |
| 10 | IsFrozen | bit |  |  | Yes |  | |
| 11 | UpdatedAt | datetime |  |  |  |  | |

---

## Table: OrderStep

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | bigint |  | Yes | Yes | PK | |
| 2 | OrderId | uniqueidentifier |  |  | Yes | FK | |
| 3 | TreatmentStepId | bigint |  |  | Yes | FK | |
| 4 | Status | int |  |  | Yes |  | |
| 5 | StartDate | date |  |  | Yes |  | |
| 6 | EndDate | date |  |  |  |  | |
| 7 | PaymentStatus | int |  |  | Yes |  | |
| 8 | TotalAmount | decimal | 18,2 |  |  |  | |

---

## Table: orderStepPayments

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | uniqueidentifier |  | Yes | Yes | PK | |
| 2 | PatientId | uniqueidentifier |  |  | Yes | FK | |
| 3 | OrderStepId | bigint |  |  | Yes | FK | |
| 4 | PaymentCode | nvarchar | 255 |  |  |  | |
| 5 | TotalAmount | decimal | 18,2 | Yes |  |  | |
| 6 | PaymentMethod | int |  |  | Yes |  | |
| 7 | TransactionCode | nvarchar | 255 |  |  |  | |
| 8 | PaymentDate | datetime |  |  | Yes |  | |
| 9 | Status | int |  |  | Yes |  | |
| 10 | GatewayResponseCode | nvarchar | 50 |  |  |  | |
| 11 | GatewayMessage | nvarchar | 255 |  |  |  | |

---

## Table: Patient

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | uniqueidentifier |  | Yes | Yes | PK | |
| 2 | UserProfileId | uniqueidentifier |  |  | Yes | FK | |
| 3 | MedicalHistory | ntext |  |  |  |  | |
| 4 | PartnerFullName | nvarchar | 500 |  |  |  | |
| 5 | PartnerEmail | nvarchar | 255 |  |  |  | |
| 6 | PartnerPhone | nvarchar | 20 |  |  |  | |

---

## Table: Prescription

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | uniqueidentifier |  | Yes | Yes | PK | |
| 2 | OrderId | uniqueidentifier |  |  | Yes | FK | |
| 3 | PrescriptionDate | datetime |  |  | Yes |  | |

---

## Table: PrescriptionItem

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | bigint |  | Yes | Yes | PK | |
| 2 | PrescriptionId | uniqueidentifier |  |  | Yes | FK | |
| 3 | MedicationName | nvarchar | 255 |  |  |  | |
| 4 | Quantity | int |  |  |  |  | |
| 5 | StartDate | date |  |  |  |  | |
| 6 | EndDate | date |  |  |  |  | |
| 7 | SpecialInstructions | ntext |  |  |  |  | |

---

## Table: Slot

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | bigint |  | Yes | Yes | PK | |
| 2 | SlotNumber | int |  |  | Yes |  | |
| 3 | StartTime | time |  |  | Yes |  | |
| 4 | EndTime | time |  |  | Yes |  | |

---

## Table: TreatmentService

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | uniqueidentifier |  | Yes | Yes | PK | |
| 2 | Name | nvarchar |  |  | Yes |  | |
| 3 | Description | ntext |  |  |  |  | |
| 4 | EstimatePrice | decimal | 18,2 |  |  |  | |
| 5 | Duration | int |  |  |  |  | |
| 6 | SuccessRate | decimal | 5,2 |  |  |  | Rating of treatment |
| 7 | RecommendedFor | nvarchar |  |  |  |  | |
| 8 | Contraindications | nvarchar |  |  |  |  | |
| 9 | UpdatedAt | datetime |  |  |  |  | |

---

## Table: TreatmentStep

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | bigint |  | Yes | Yes | PK | |
| 2 | TreatmentServiceId | uniqueidentifier |  |  | Yes | FK | |
| 3 | StepName | nvarchar |  |  | Yes |  | |
| 4 | Description | nvarchar | 1000 |  |  |  | |
| 5 | StepOrder | int |  |  | Yes |  | |
| 6 | EstimatedDurationDays | int |  |  |  |  | |
| 7 | Amount | decimal | 18,2 |  |  |  | |
| 8 | UpdatedAt | datetime |  |  |  |  | |

---

## Table: UserProfile

| # | Field name | Type | Size | Unique | Not Null | PK/FK | Notes |
|---:|---|---|---:|---:|---:|---:|---|
| 1 | Id | uniqueidentifier |  | Yes | Yes | PK | |
| 2 | FirstName | nvarchar | 255 |  |  |  | |
| 3 | MiddleName | nvarchar | 255 |  |  |  | |
| 4 | LastName | nvarchar | 255 |  |  |  | |
| 5 | Gender | int |  |  |  |  | |
| 6 | DateOfBirth | date |  |  |  |  | |
| 7 | Address | nvarchar |  |  |  |  | |
| 8 | AvatarUrl | nvarchar |  |  |  |  | |
| 9 | CreatedAt | datetime |  |  | Yes |  | |
| 10 | UpdatedAt | datetime |  |  |  |  | |

---


### Ghi chú chung
- Trường "uniqueidentifier" tương ứng với UUID / GUID ở DB (sử dụng java.util.UUID khi map JPA).
- Một số kiểu dữ liệu (ntext, nvarchar) đã giữ nguyên như trong schema của bạn.
- Nếu bạn muốn file Word (`.docx`) hoặc Excel (`.xlsx`), tôi có thể xuất Markdown này sang định dạng đó và thêm header/footers theo yêu cầu.

---

File này được tạo tại: `docs/TABLES_SCHEMA.md`

Nếu OK, tôi sẽ:
- (A) xuất thành `docs/TABLES_SCHEMA.docx` hoặc `docs/TABLES_SCHEMA.xlsx` theo yêu cầu, hoặc
- (B) tiến hành sinh các class JPA + repository tương ứng dựa trên các bảng này.

Bạn muốn tiếp theo (A) xuất sang Word/Excel hay (B) tạo entity class + repositories? Hoặc chỉnh sửa nội dung bảng nào trước khi xuất?