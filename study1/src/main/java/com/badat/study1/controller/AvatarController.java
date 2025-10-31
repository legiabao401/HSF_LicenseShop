package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.service.UserActivityLogService;
import com.badat.study1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class AvatarController {
    
    private final UserService userService;
    private final UserActivityLogService userActivityLogService;
    
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                        HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            
            User currentUser = (User) authentication.getPrincipal();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            log.info("Avatar upload request for user: {}, IP: {}", currentUser.getUsername(), ipAddress);
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống"));
            }
            
            // Check file size (2MB max)
            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không được vượt quá 2MB"));
            }
            
            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && 
                !contentType.equals("image/png") && !contentType.equals("image/gif"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file JPG, PNG, GIF"));
            }
            
            // Upload avatar
            userService.uploadAvatar(currentUser.getId(), file);
            
            // Log the avatar upload
            userActivityLogService.logProfileUpdate(currentUser, "Cập nhật avatar", ipAddress, userAgent, request.getRequestURI(), request.getMethod(), true, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Avatar đã được cập nhật thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Error uploading avatar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Có lỗi xảy ra khi upload avatar: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error uploading avatar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Có lỗi xảy ra khi upload avatar: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        try {
            // Add timeout handling - return default avatar if takes too long
            byte[] avatarData = userService.getAvatar(userId);
            
            if (avatarData != null && avatarData.length > 0) {
                HttpHeaders headers = new HttpHeaders();
                // Detect content type from byte data
                String contentType = detectContentType(avatarData);
                headers.setContentType(MediaType.valueOf(contentType));
                headers.setContentLength(avatarData.length);
                headers.setCacheControl("public, max-age=300"); // Cache for 5 minutes
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(avatarData);
            } else {
                // Return default avatar instead of 404
                return getDefaultAvatarResponse();
            }
            
        } catch (Exception e) {
            log.error("Error getting avatar for user {}: {}", userId, e.getMessage());
            // Return default avatar instead of 404
            return getDefaultAvatarResponse();
        }
    }

    @GetMapping("/avatar/me")
    public ResponseEntity<byte[]> getMyAvatar() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
                log.warn("Unauthenticated request for avatar/me");
                return getDefaultAvatarResponse();
            }
            User currentUser = (User) authentication.getPrincipal();
            log.info("Getting avatar for current user: {} (ID: {})", currentUser.getUsername(), currentUser.getId());
            
            byte[] avatarData = userService.getAvatar(currentUser.getId());
            log.info("Avatar data received: {} bytes", avatarData != null ? avatarData.length : 0);
            
            if (avatarData != null && avatarData.length > 0) {
                HttpHeaders headers = new HttpHeaders();
                // Detect content type from byte data
                String contentType = detectContentType(avatarData);
                headers.setContentType(MediaType.valueOf(contentType));
                headers.setContentLength(avatarData.length);
                headers.setCacheControl("public, max-age=300"); // Cache for 5 minutes
                log.info("Returning avatar with content type: {}", contentType);
                return ResponseEntity.ok().headers(headers).body(avatarData);
            }
            log.info("No avatar data, returning default avatar");
            return getDefaultAvatarResponse();
        } catch (Exception e) {
            log.error("Error getting current user's avatar: {}", e.getMessage());
            return getDefaultAvatarResponse();
        }
    }
    
    private ResponseEntity<byte[]> getDefaultAvatarResponse() {
        try {
            byte[] defaultAvatar = userService.getDefaultAvatar();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/svg+xml"));
            headers.setContentLength(defaultAvatar.length);
            headers.setCacheControl("public, max-age=300"); // Cache for 5 minutes
            return ResponseEntity.ok().headers(headers).body(defaultAvatar);
        } catch (Exception e) {
            log.error("Error getting default avatar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }
            
            User currentUser = (User) authentication.getPrincipal();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            log.info("Avatar delete request for user: {}, IP: {}", currentUser.getUsername(), ipAddress);
            
            // Delete avatar
            userService.deleteAvatar(currentUser.getId());
            
            // Log the avatar deletion
            userActivityLogService.logProfileUpdate(currentUser, "Xóa avatar", ipAddress, userAgent, request.getRequestURI(), request.getMethod(), true, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Avatar đã được xóa thành công");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting avatar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Có lỗi xảy ra khi xóa avatar: " + e.getMessage()
            ));
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private String detectContentType(byte[] data) {
        if (data.length < 4) {
            return "image/svg+xml"; // Default fallback
        }
        
        // Check for common image formats
        // JPEG: FF D8 FF
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        
        // PNG: 89 50 4E 47
        if (data[0] == (byte) 0x89 && data[1] == (byte) 0x50 && data[2] == (byte) 0x4E && data[3] == (byte) 0x47) {
            return "image/png";
        }
        
        // GIF: 47 49 46 38
        if (data[0] == (byte) 0x47 && data[1] == (byte) 0x49 && data[2] == (byte) 0x46 && data[3] == (byte) 0x38) {
            return "image/gif";
        }
        
        // SVG: Check for SVG content
        String content = new String(data, 0, Math.min(100, data.length));
        if (content.contains("<svg") || content.contains("<?xml")) {
            return "image/svg+xml";
        }
        
        // Default fallback
        return "image/png";
    }
}
