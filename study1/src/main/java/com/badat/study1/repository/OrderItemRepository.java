package com.badat.study1.repository;

import com.badat.study1.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Tìm tất cả OrderItem theo Order ID
     */
    List<OrderItem> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    /**
     * Tìm tất cả OrderItem theo Order ID với fetch join
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "LEFT JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH oi.warehouse w " +
           "WHERE oi.orderId = :orderId " +
           "ORDER BY oi.createdAt ASC")
    List<OrderItem> findByOrderIdWithDetails(@Param("orderId") Long orderId);

    /**
     * Tìm OrderItem theo Order ID và Product ID
     */
    Optional<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);

    /**
     * Tìm tất cả OrderItem theo Product ID
     */
    List<OrderItem> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Tìm tất cả OrderItem theo Warehouse ID
     */
    List<OrderItem> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    /**
     * Đếm số lượng OrderItem theo Order ID
     */
    long countByOrderId(Long orderId);

    /**
     * Tìm tất cả OrderItem theo trạng thái
     */
    List<OrderItem> findByStatusOrderByCreatedAtDesc(OrderItem.Status status);

    /**
     * Tìm tất cả OrderItem theo Order ID và trạng thái
     */
    List<OrderItem> findByOrderIdAndStatusOrderByCreatedAtAsc(Long orderId, OrderItem.Status status);

    /**
     * Đếm số lượng OrderItem theo Order ID và trạng thái
     */
    long countByOrderIdAndStatus(Long orderId, OrderItem.Status status);

    /**
     * Tìm tất cả OrderItem theo Warehouse User ID
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "LEFT JOIN FETCH oi.warehouse w " +
           "LEFT JOIN FETCH w.user u " +
           "WHERE u.id = :sellerId " +
           "ORDER BY oi.createdAt DESC")
    List<OrderItem> findByWarehouseUserOrderByCreatedAtDesc(@Param("sellerId") Long sellerId);

    /**
     * Đếm số lượng OrderItem theo Warehouse User ID và trạng thái
     */
    @Query("SELECT COUNT(oi) FROM OrderItem oi " +
           "LEFT JOIN oi.warehouse w " +
           "LEFT JOIN w.user u " +
           "WHERE u.id = :sellerId AND oi.status = :status")
    long countByWarehouseUserAndStatus(@Param("sellerId") Long sellerId, @Param("status") OrderItem.Status status);

    /**
     * Tìm OrderItem theo Warehouse ID và trạng thái
     */
    List<OrderItem> findByWarehouseIdAndStatusOrderByCreatedAtDesc(Long warehouseId, OrderItem.Status status);

    /**
     * Kiểm tra xem user đã mua sản phẩm có warehouse item_type là KEY_LICENSE_BASIC hoặc KEY_LICENSE_PREMIUM chưa
     * Logic: Tìm order_item -> kiểm tra order status COMPLETED -> kiểm tra warehouse item_type
     */
    @Query(value = "SELECT CASE WHEN COUNT(oi.id) > 0 THEN 1 ELSE 0 END FROM order_item oi " +
           "INNER JOIN `order` o ON oi.order_id = o.id " +
           "INNER JOIN warehouse w ON oi.warehouse_id = w.id " +
           "WHERE o.buyer_id = :buyerId " +
           "AND o.status = 'COMPLETED' " +
           "AND (w.item_type = 'KEY_LICENSE_PREMIUM' OR w.item_type = 'KEY_LICENSE_BASIC')", 
           nativeQuery = true)
    Long countPurchasedKeyProduct(@Param("buyerId") Long buyerId);
    
    /**
     * Debug: Lấy tất cả warehouse item_types từ các đơn hàng COMPLETED của user
     */
    @Query(value = "SELECT DISTINCT w.item_type FROM order_item oi " +
           "INNER JOIN `order` o ON oi.order_id = o.id " +
           "INNER JOIN warehouse w ON oi.warehouse_id = w.id " +
           "WHERE o.buyer_id = :buyerId " +
           "AND o.status = 'COMPLETED'", nativeQuery = true)
    List<String> getAllWarehouseItemTypesFromCompletedOrders(@Param("buyerId") Long buyerId);
    
    default boolean hasPurchasedKeyProduct(Long buyerId) {
        Long count = countPurchasedKeyProduct(buyerId);
        return count != null && count > 0;
    }
}
