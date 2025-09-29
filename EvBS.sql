-- =====================================
-- Database Script for EV Battery Swap System (SQL Server 2019)
-- =====================================

IF DB_ID('EVBatterySwap') IS NOT NULL
    DROP DATABASE EVBatterySwap;
GO

CREATE DATABASE EVBatterySwap;
GO
USE EVBatterySwap;
GO

-- ========================
-- 1. USERS
-- ========================
CREATE TABLE Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    FullName NVARCHAR(150) NOT NULL,
    Email NVARCHAR(150) NOT NULL UNIQUE,
    PhoneNumber NVARCHAR(30),
    PasswordHash NVARCHAR(255) NOT NULL,
    Role NVARCHAR(50) NOT NULL CHECK (Role IN ('Driver','Staff','Admin')),
    Status NVARCHAR(50) DEFAULT 'Active'
);

-- ========================
-- 2. DRIVER PROFILE
-- ========================
CREATE TABLE DriverProfile (
    DriverID INT PRIMARY KEY,
    DrivingLicense NVARCHAR(100) NOT NULL,
    CONSTRAINT FK_DriverProfile_User FOREIGN KEY (DriverID) REFERENCES Users(UserID)
);

-- ========================
-- 3. STATION
-- ========================
CREATE TABLE Station (
    StationID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(150) NOT NULL,
    Location NVARCHAR(255),
    Capacity INT,
    ContactInfo NVARCHAR(150),
    Status NVARCHAR(50) DEFAULT 'Active'
);

-- ========================
-- 4. STAFF PROFILE
-- ========================
CREATE TABLE StaffProfile (
    StaffID INT PRIMARY KEY,
    StationID INT,
    Position NVARCHAR(100),
    WorkShift NVARCHAR(100),
    CONSTRAINT FK_StaffProfile_User FOREIGN KEY (StaffID) REFERENCES Users(UserID),
    CONSTRAINT FK_StaffProfile_Station FOREIGN KEY (StationID) REFERENCES Station(StationID)
);

-- ========================
-- 5. VEHICLE
-- ========================
CREATE TABLE Vehicle (
    VehicleID INT IDENTITY(1,1) PRIMARY KEY,
    VIN NVARCHAR(100) NOT NULL UNIQUE,
    PlateNumber NVARCHAR(50) NOT NULL UNIQUE,
    Model NVARCHAR(100),
    DriverID INT,
    CONSTRAINT FK_Vehicle_Driver FOREIGN KEY (DriverID) REFERENCES DriverProfile(DriverID)
);

-- ========================
-- 6. BATTERY
-- ========================
CREATE TABLE Battery (
    BatteryID INT IDENTITY(1,1) PRIMARY KEY,
    Model NVARCHAR(100),
    Capacity FLOAT,
    StateOfHealth INT,
    Status NVARCHAR(50) DEFAULT 'Available'
        CHECK (Status IN ('Available','Charging','InUse','Maintenance','Faulty')),
    CurrentStationID INT,
    CONSTRAINT FK_Battery_Station FOREIGN KEY (CurrentStationID) REFERENCES Station(StationID)
);

-- ========================
-- 7. BATTERY HISTORY
-- ========================
CREATE TABLE BatteryHistory (
    HistoryID INT IDENTITY(1,1) PRIMARY KEY,
    BatteryID INT NOT NULL,
    EventType NVARCHAR(50) NOT NULL
        CHECK (EventType IN ('Charge','SwapOut','SwapIn','Maintenance','FaultDetected')),
    EventTime DATETIME NOT NULL,
    RelatedStationID INT,
    RelatedVehicleID INT,
    StaffID INT,
    CONSTRAINT FK_BatteryHistory_Battery FOREIGN KEY (BatteryID) REFERENCES Battery(BatteryID),
    CONSTRAINT FK_BatteryHistory_Vehicle FOREIGN KEY (RelatedVehicleID) REFERENCES Vehicle(VehicleID),
    CONSTRAINT FK_BatteryHistory_Staff FOREIGN KEY (StaffID) REFERENCES StaffProfile(StaffID)
);

-- ========================
-- 8. STATION INVENTORY
-- ========================
CREATE TABLE StationInventory (
    StationInventoryID INT IDENTITY(1,1) PRIMARY KEY,
    StationID INT NOT NULL,
    BatteryID INT NOT NULL,
    Status NVARCHAR(50) DEFAULT 'Available'
        CHECK (Status IN ('Available','Charging','Reserved','Maintenance')),
    LastUpdate DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_StationInventory_Station FOREIGN KEY (StationID) REFERENCES Station(StationID),
    CONSTRAINT FK_StationInventory_Battery FOREIGN KEY (BatteryID) REFERENCES Battery(BatteryID)
);

-- ========================
-- 9. BOOKING
-- ========================
CREATE TABLE Booking (
    BookingID INT IDENTITY(1,1) PRIMARY KEY,
    DriverID INT NOT NULL,
    VehicleID INT NOT NULL,
    StationID INT NOT NULL,
    BookingTime DATETIME NOT NULL,
    Status NVARCHAR(50) DEFAULT 'Pending'
        CHECK (Status IN ('Pending','Confirmed','Completed','Cancelled')),
    CONSTRAINT FK_Booking_Driver FOREIGN KEY (DriverID) REFERENCES DriverProfile(DriverID),
    CONSTRAINT FK_Booking_Vehicle FOREIGN KEY (VehicleID) REFERENCES Vehicle(VehicleID),
    CONSTRAINT FK_Booking_Station FOREIGN KEY (StationID) REFERENCES Station(StationID)
);

-- ========================
-- 10. SWAP TRANSACTION
-- ========================
CREATE TABLE SwapTransaction (
    TransactionID INT IDENTITY(1,1) PRIMARY KEY,
    DriverID INT NOT NULL,
    VehicleID INT NOT NULL,
    StationID INT NOT NULL,
    StaffID INT NOT NULL,
    SwapOutBatteryID INT,
    SwapInBatteryID INT,
    StartTime DATETIME,
    EndTime DATETIME,
    Cost DECIMAL(12,2),
    Status NVARCHAR(50) DEFAULT 'PendingPayment'
        CHECK (Status IN ('Success','Failed','PendingPayment')),
    CONSTRAINT FK_SwapTransaction_Driver FOREIGN KEY (DriverID) REFERENCES DriverProfile(DriverID),
    CONSTRAINT FK_SwapTransaction_Vehicle FOREIGN KEY (VehicleID) REFERENCES Vehicle(VehicleID),
    CONSTRAINT FK_SwapTransaction_Station FOREIGN KEY (StationID) REFERENCES Station(StationID),
    CONSTRAINT FK_SwapTransaction_Staff FOREIGN KEY (StaffID) REFERENCES StaffProfile(StaffID),
    CONSTRAINT FK_SwapTransaction_SwapOut FOREIGN KEY (SwapOutBatteryID) REFERENCES Battery(BatteryID),
    CONSTRAINT FK_SwapTransaction_SwapIn FOREIGN KEY (SwapInBatteryID) REFERENCES Battery(BatteryID)
);

-- ========================
-- 11. SERVICE PACKAGE
-- ========================
CREATE TABLE ServicePackage (
    PackageID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(150) NOT NULL,
    Description NVARCHAR(255),
    Price DECIMAL(12,2) NOT NULL,
    Duration INT NOT NULL, -- days
    MaxSwaps INT NOT NULL
);

-- ========================
-- 12. DRIVER SUBSCRIPTION
-- ========================
CREATE TABLE DriverSubscription (
    SubscriptionID INT IDENTITY(1,1) PRIMARY KEY,
    DriverID INT NOT NULL,
    PackageID INT NOT NULL,
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    Status NVARCHAR(50) DEFAULT 'Active',
    CONSTRAINT FK_DriverSubscription_Driver FOREIGN KEY (DriverID) REFERENCES DriverProfile(DriverID),
    CONSTRAINT FK_DriverSubscription_Package FOREIGN KEY (PackageID) REFERENCES ServicePackage(PackageID)
);

-- ========================
-- 13. PAYMENT
-- ========================
CREATE TABLE Payment (
    PaymentID INT IDENTITY(1,1) PRIMARY KEY,
    TransactionID INT,
    SubscriptionID INT,
    Amount DECIMAL(12,2) NOT NULL,
    PaymentMethod NVARCHAR(50),
    PaymentDate DATETIME NOT NULL,
    Status NVARCHAR(50) DEFAULT 'Completed',
    CONSTRAINT FK_Payment_Transaction FOREIGN KEY (TransactionID) REFERENCES SwapTransaction(TransactionID),
    CONSTRAINT FK_Payment_Subscription FOREIGN KEY (SubscriptionID) REFERENCES DriverSubscription(SubscriptionID)
);

-- ========================
-- 14. SUPPORT TICKET
-- ========================
CREATE TABLE SupportTicket (
    TicketID INT IDENTITY(1,1) PRIMARY KEY,
    DriverID INT NOT NULL,
    StationID INT,
    Subject NVARCHAR(200) NOT NULL,
    Description NVARCHAR(MAX),
    Status NVARCHAR(50) DEFAULT 'Open'
        CHECK (Status IN ('Open','InProgress','Resolved','Closed')),
    CreatedAt DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_SupportTicket_Driver FOREIGN KEY (DriverID) REFERENCES DriverProfile(DriverID),
    CONSTRAINT FK_SupportTicket_Station FOREIGN KEY (StationID) REFERENCES Station(StationID)
);

-- ========================
-- 15. TICKET RESPONSE
-- ========================
CREATE TABLE TicketResponse (
    ResponseID INT IDENTITY(1,1) PRIMARY KEY,
    TicketID INT NOT NULL,
    StaffID INT NOT NULL,
    Message NVARCHAR(MAX) NOT NULL,
    ResponseTime DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_TicketResponse_Ticket FOREIGN KEY (TicketID) REFERENCES SupportTicket(TicketID),
    CONSTRAINT FK_TicketResponse_Staff FOREIGN KEY (StaffID) REFERENCES StaffProfile(StaffID)
);

-- ========================
-- 16. ADMIN REPORT
-- ========================
CREATE TABLE AdminReport (
    ReportID INT IDENTITY(1,1) PRIMARY KEY,
    ReportType NVARCHAR(100) NOT NULL,
    GeneratedBy INT NOT NULL,
    CreatedAt DATETIME DEFAULT GETDATE(),
    ReportData NVARCHAR(MAX),
    CONSTRAINT FK_AdminReport_User FOREIGN KEY (GeneratedBy) REFERENCES Users(UserID)
);
