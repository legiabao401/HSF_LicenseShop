package com.badat.study1.service;

import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CustomOAuth2UserService(UserRepository userRepository, WalletRepository walletRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("CustomOAuth2UserService.loadUser called - Registration ID: {}", 
                userRequest.getClientRegistration().getRegistrationId());
        
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        log.info("OAuth2User loaded from Google - Email: {}", (String) oAuth2User.getAttribute("email"));
        
        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        if (!"google".equals(registrationId)) {
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
        }

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String googleId = (String) attributes.get("id");
        String picture = (String) attributes.get("picture"); // Google avatar URL
        
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Tìm user theo email (chỉ tài khoản chưa bị xóa)
        Optional<User> existingUser = userRepository.findByEmailAndIsDeleteFalse(email);
        
        // Kiểm tra nếu email tồn tại nhưng tài khoản đã bị xóa
        if (existingUser.isEmpty()) {
            Optional<User> deletedUser = userRepository.findByEmail(email);
            if (deletedUser.isPresent() && deletedUser.get().getIsDelete()) {
                throw new OAuth2AuthenticationException(
                    "Tài khoản Google với email " + email + " đã bị xóa. " +
                    "Vui lòng liên hệ quản trị viên để khôi phục tài khoản."
                );
            }
        }
        
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            
            // Kiểm tra nếu tài khoản bị khóa
            if (user.getStatus() == User.Status.LOCKED) {
                throw new OAuth2AuthenticationException(
                    "Tài khoản Google với email " + email + " đã bị khóa. " +
                    "Vui lòng liên hệ quản trị viên để mở khóa tài khoản."
                );
            }
            
            // Kiểm tra nếu user đã đăng ký bằng cách thủ công (provider = LOCAL)
            if ("LOCAL".equals(user.getProvider())) {
                // THÔNG BÁO LỖI: Email đã được sử dụng cho tài khoản LOCAL
                throw new OAuth2AuthenticationException(
                    "Email " + email + " đã được sử dụng cho tài khoản đăng ký thủ công. " +
                    "Vui lòng sử dụng tên đăng nhập và mật khẩu để đăng nhập, " +
                    "hoặc sử dụng email khác để đăng nhập Google."
                );
            } else if ("GOOGLE".equals(user.getProvider()) && googleId.equals(user.getProviderId())) {
                // User đã đăng nhập bằng Google trước đó - cập nhật avatar nếu chưa có
                // Temporarily comment out to avoid database issues
                // if ((user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) && picture != null && !picture.isEmpty()) {
                //     user.setAvatarUrl(picture);
                //     userRepository.save(user);
                //     log.info("Updated avatar URL for existing Google user {}", email);
                // }
                log.info("Existing Google user {} logged in", email);
            } else {
                // Email đã được sử dụng bởi provider khác
                throw new OAuth2AuthenticationException("Email " + email + " is already associated with another account");
            }
        } else {
            // Tạo user mới
            user = createNewGoogleUser(email, name, googleId, picture);
            log.info("Created new Google user: {}", email);
        }

        return new CustomOAuth2User(
            user.getUsername(),
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
            attributes,
            user
        );
    }

    private User createNewGoogleUser(String email, String name, String googleId, String picture) {
        // Username = Email (theo yêu cầu)
        String username = email;
        
        // Đảm bảo username là unique (nếu email đã tồn tại) - chỉ kiểm tra tài khoản chưa bị xóa
        String originalUsername = username;
        int counter = 1;
        while (userRepository.findByUsernameAndIsDeleteFalse(username).isPresent()) {
            username = originalUsername + "_" + counter;
            counter++;
        }

        // Tạo password random và hash
        String randomPassword = UUID.randomUUID().toString();
        
        User newUser = User.builder()
                .username(username)
                .email(email)
                .fullName(name)
                .password(passwordEncoder.encode(randomPassword))
                .provider("GOOGLE")
                .providerId(googleId)
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();
        
        // Set avatar URL after building to avoid issues with new fields
        // Temporarily comment out to avoid database issues
        // if (picture != null && !picture.isEmpty()) {
        //     newUser.setAvatarUrl(picture);
        // }

        User savedUser = userRepository.save(newUser);
        
        // Create wallet for new Google user
        Wallet wallet = Wallet.builder()
                .userId(savedUser.getId())
                .balance(BigDecimal.ZERO)
                .build();
        walletRepository.save(wallet);
        log.info("Created wallet for new Google user: {}", email);
        
        return savedUser;
    }

    public static class CustomOAuth2User implements OAuth2User {
        private final String username;
        private final String password;
        private final java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities;
        private final Map<String, Object> attributes;
        private final User user;

        public CustomOAuth2User(String username, String password, 
                               java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities,
                               Map<String, Object> attributes, User user) {
            this.username = username;
            this.password = password;
            this.authorities = authorities;
            this.attributes = attributes;
            this.user = user;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getName() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public User getUser() {
            return user;
        }
    }
}
