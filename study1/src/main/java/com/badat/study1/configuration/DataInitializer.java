package com.badat.study1.configuration;

import com.badat.study1.model.*;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;



@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final ShopRepository shopRepository;
    private final BankAccountRepository bankAccountRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create sample users if not exists
        if (userRepository.count() == 0) {
            createSampleUsers();
        }
        
        // Create wallets for all users if not exists
        createWalletsForUsers();
        
        // Skip creating sample bank accounts
        log.info("Skipping sample bank account creation");
        
        // Ensure admin has a shop
        createAdminShopIfMissing();
        
        // Skip creating sample stalls, and products
        log.info("Skipping sample stall, and product creation");
        
        log.info("Data initialization completed");
    }

    private void createSampleUsers() {
        User admin = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.Role.ADMIN)
                .status(User.Status.ACTIVE)
                .build();
        userRepository.save(admin);

        User seller = User.builder()
                .username("seller1")
                .email("seller1@example.com")
                .password(passwordEncoder.encode("seller123"))
                .role(User.Role.SELLER)
                .status(User.Status.ACTIVE)
                .build();
        userRepository.save(seller);

        User customer = User.builder()
                .username("customer1")
                .email("customer1@example.com")
                .password(passwordEncoder.encode("customer123"))
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();
        userRepository.save(customer);
    }

    private void createWalletsForUsers() {
        userRepository.findAll().forEach(user -> {
            if (!walletRepository.findByUserId(user.getId()).isPresent()) {
                Wallet wallet = Wallet.builder()
                        .userId(user.getId())
                        .balance(java.math.BigDecimal.ZERO)
                        .build();
                walletRepository.save(wallet);
                log.info("Created wallet for user: {}", user.getUsername());
            }
        });
    }

    private void createAdminShopIfMissing() {
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ADMIN)
                .findFirst()
                .ifPresent(admin -> {
                    if (shopRepository.findByUserId(admin.getId()).isEmpty()) {
                        // Create a simple bank account record for admin (required by Shop)
                        BankAccount bank = new BankAccount();
                        bank.setUserId(admin.getId());
                        bank.setAccountNumber("000000001");
                        bank.setBankName("SYSTEM");
                        bank.setBankAccount("000000001 SYSTEM");
                        bank.setCreatedAt(java.time.Instant.now());
                        bank.setDelete(false);
                        bank = bankAccountRepository.save(bank);

                        Shop shop = Shop.builder()
                                .userId(admin.getId())
                                .shopName("Admin Shop")
                                .cccd("ADMIN")
                                .bankAccountId(bank.getId())
                                .status(Shop.Status.ACTIVE)
                                .createdAt(java.time.Instant.now())
                                .isDelete(false)
                                .build();
                        shopRepository.save(shop);
                        log.info("Created default shop for admin: {}", admin.getUsername());
                    }
                });
    }


}
