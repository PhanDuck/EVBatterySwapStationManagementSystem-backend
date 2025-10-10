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
echo  Database: EVBatterySwap(tự động tạo dữ liệu nếu chưa có)
echo  Dữ liệu mẫu: 5 trạm, 75 pin, xe máy điện VN
echo.

call mvnw.cmd spring-boot:run

echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║                      THÔNG TIN QUAN TRỌNG                    ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.
echo 🌐 API Documentation: http://localhost:8080/swagger-ui.html
echo.
echo 🔐 TÀI KHOẢN TEST (mật khẩu: password123):
echo     Admin: admin@evbs.com (SĐT: 0901000001)
echo     Staff: staff1@evbs.com (SĐT: 0902000001)
echo     Driver: driver1@gmail.com (SĐT: 0903000001)
echo.
echo  Lưu ý: Database sẽ tự động được tạo với dữ liệu mẫu khi khởi động lần đầu
echo.

pause