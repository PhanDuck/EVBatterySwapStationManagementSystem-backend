package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload.path:/home/uploads/images/}")
    private String uploadPath;

    @Value("${file.upload.url:https://evbatteryswapsystem.com/images/}")
    private String uploadUrl;

    /**
     * Upload file và trả về URL đầy đủ
     */
    public String uploadFile(MultipartFile file) {
        try {
            // Log để debug
            System.out.println("=== FILE UPLOAD DEBUG ===");
            System.out.println("Upload Path: " + uploadPath);
            System.out.println("Upload URL: " + uploadUrl);

            // Validate file
            if (file.isEmpty()) {
                throw new AuthenticationException("File trống!");
            }

            // Kiểm tra định dạng file (chỉ cho phép ảnh)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new AuthenticationException("Chỉ chấp nhận ảnh!");
            }

            // Kiểm tra kích thước file (max 10MB)
            long maxFileSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxFileSize) {
                throw new AuthenticationException("File quá 10MB!");
            }

            // Tạo thư mục nếu chưa tồn tại
            Path uploadDirectory = Paths.get(uploadPath);
            System.out.println("Absolute Upload Path: " + uploadDirectory.toAbsolutePath());

            if (!Files.exists(uploadDirectory)) {
                System.out.println("Creating directory: " + uploadDirectory);
                Files.createDirectories(uploadDirectory);
            }

            // Kiểm tra quyền ghi
            if (!Files.isWritable(uploadDirectory)) {
                throw new AuthenticationException("Lỗi mạng!");
            }

            // Tạo tên file unique để tránh trùng lặp
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFilename.substring(dotIndex);
            }

            // Tên file: UUID + extension
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadDirectory.resolve(newFilename);

            System.out.println("Saving file to: " + filePath.toAbsolutePath());

            // Copy file vào thư mục
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Verify file đã được lưu
            if (Files.exists(filePath)) {
                System.out.println("File saved successfully!");
                System.out.println("File size: " + Files.size(filePath) + " bytes");
            } else {
                throw new AuthenticationException("Lỗi lưu file!");
            }

            // Trả về URL đầy đủ để truy cập file
            String fullUrl = uploadUrl + newFilename;
            System.out.println("Returning URL: " + fullUrl);
            System.out.println("========================");

            return fullUrl;

        } catch (IOException e) {
            System.err.println("Error uploading file: " + e.getMessage());
            e.printStackTrace();
            throw new AuthenticationException("Lỗi upload!");
        }
    }

    /**
     * Xóa file theo URL
     */
    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null && !fileUrl.isEmpty() && fileUrl.startsWith(uploadUrl)) {
                // Lấy tên file từ URL
                String filename = fileUrl.substring(uploadUrl.length());
                Path path = Paths.get(uploadPath, filename);
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            // Log error nhưng không throw exception
            System.err.println("Không thể xóa file: " + fileUrl + " - " + e.getMessage());
        }
    }
}
