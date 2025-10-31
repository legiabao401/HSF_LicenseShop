package com.badat.study1.repository;

import com.badat.study1.model.Cart;
import com.badat.study1.model.CartItem;
import com.badat.study1.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}



