package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.ProductCategory;
import com.ogabek.istudy.entity.ProductSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSaleRepository extends JpaRepository<ProductSale, Long> {

    @Query("SELECT ps FROM ProductSale ps " +
           "LEFT JOIN FETCH ps.branch " +
           "LEFT JOIN FETCH ps.student " +
           "WHERE ps.branch.id = :branchId " +
           "ORDER BY ps.createdAt DESC")
    List<ProductSale> findByBranchIdWithRelations(@Param("branchId") Long branchId);

    @Query("SELECT ps FROM ProductSale ps " +
           "LEFT JOIN FETCH ps.branch " +
           "LEFT JOIN FETCH ps.student " +
           "WHERE ps.id = :id")
    Optional<ProductSale> findByIdWithRelations(@Param("id") Long id);

    @Query("SELECT ps FROM ProductSale ps " +
           "LEFT JOIN FETCH ps.branch " +
           "LEFT JOIN FETCH ps.student " +
           "WHERE ps.branch.id = :branchId AND ps.category = :category " +
           "ORDER BY ps.createdAt DESC")
    List<ProductSale> findByBranchIdAndCategory(@Param("branchId") Long branchId, 
                                                @Param("category") ProductCategory category);

    @Query("SELECT ps FROM ProductSale ps " +
           "LEFT JOIN FETCH ps.branch " +
           "LEFT JOIN FETCH ps.student " +
           "WHERE ps.student.id = :studentId " +
           "ORDER BY ps.createdAt DESC")
    List<ProductSale> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT ps FROM ProductSale ps " +
           "LEFT JOIN FETCH ps.branch " +
           "LEFT JOIN FETCH ps.student " +
           "WHERE ps.branch.id = :branchId " +
           "AND ps.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ps.createdAt DESC")
    List<ProductSale> findByBranchIdAndDateRange(@Param("branchId") Long branchId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    // Sum queries
    @Query("SELECT COALESCE(SUM(ps.totalAmount), 0) FROM ProductSale ps " +
           "WHERE ps.branch.id = :branchId")
    BigDecimal sumTotalAmountByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(ps.totalAmount), 0) FROM ProductSale ps " +
           "WHERE ps.branch.id = :branchId " +
           "AND YEAR(ps.createdAt) = :year AND MONTH(ps.createdAt) = :month")
    BigDecimal sumTotalAmountByMonth(@Param("branchId") Long branchId, 
                                     @Param("year") int year, 
                                     @Param("month") int month);

    @Query("SELECT COALESCE(SUM(ps.totalAmount), 0) FROM ProductSale ps " +
           "WHERE ps.branch.id = :branchId " +
           "AND ps.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByDateRange(@Param("branchId") Long branchId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(ps.totalAmount), 0) FROM ProductSale ps " +
           "WHERE ps.branch.id = :branchId AND ps.category = :category")
    BigDecimal sumTotalAmountByCategory(@Param("branchId") Long branchId, 
                                       @Param("category") ProductCategory category);
}