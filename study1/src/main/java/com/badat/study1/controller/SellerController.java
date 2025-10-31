package com.badat.study1.controller;

import com.badat.study1.model.BankAccount;
import com.badat.study1.model.Shop;
import com.badat.study1.model.User;
import com.badat.study1.repository.BankAccountRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

@Controller
public class SellerController {

    private final ShopRepository shopRepository;
    private final BankAccountRepository bankAccountRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public SellerController(ShopRepository shopRepository,
                            BankAccountRepository bankAccountRepository,
                            WalletRepository walletRepository,
                            UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/seller/register")
    public String submitSellerRegistration(@RequestParam("ownerName") String ownerName,
                                           @RequestParam("identity") String identity,
                                           @RequestParam("bankAccountNumber") String bankAccountNumber,
                                           @RequestParam("bankName") String bankName,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {
        // Registration disabled: redirect to seller management
        return "redirect:/seller/stall-management";
    }
}


