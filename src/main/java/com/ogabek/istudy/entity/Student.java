// src/main/java/com/ogabek/istudy/entity/Student.java
package com.ogabek.istudy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phoneNumber;
    private String parentPhoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "deleted")
    private boolean deleted = false;

    // NEW: Day of month when student pays (1-31)
    // Example: 15 means student pays on 15th of each month
    @Column(name = "payment_day_of_month")
    private Integer paymentDayOfMonth;

    @CreationTimestamp
    private LocalDateTime createdAt;
}