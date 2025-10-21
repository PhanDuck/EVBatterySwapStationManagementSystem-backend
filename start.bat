@echo off
chcp 65001 >nul
echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║                HỆ THỐNG TRẠM ĐỔI PIN XE MÁY ĐIỆN             ║
echo ║                      Khởi động hệ thống                      ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

echo 🔄 Đang khởi động Spring Boot application...
echo ⚡ Port: 8080
echo  Database: EVBatterySwap_Test (tự động tạo dữ liệu nếu chưa có)
echo.

call mvnw.cmd spring-boot:run


pause