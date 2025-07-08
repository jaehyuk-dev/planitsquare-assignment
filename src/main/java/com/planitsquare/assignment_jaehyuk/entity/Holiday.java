package com.planitsquare.assignment_jaehyuk.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "holiday")
@Getter
@Setter
@NoArgsConstructor
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "holiday_seq")  // 🔥 SEQUENCE로 변경!
    @SequenceGenerator(name = "holiday_seq", sequenceName = "holiday_seq", allocationSize = 100)
    private Long id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "country_name", nullable = false)
    private String countryName;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "local_name")
    private String localName;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_fixed")
    private Boolean fixed;

    @Column(name = "is_global")
    private Boolean global;

    @Column(name = "launch_year")
    private Integer launchYear;

    @Column(columnDefinition = "TEXT")
    private String types;

    @Column(columnDefinition = "TEXT")
    private String counties;

    // 🔥 @CreationTimestamp, @UpdateTimestamp 제거! 수동 관리로 변경
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Holiday(String countryCode, String countryName, LocalDate date, String localName,
                   String name, Boolean fixed, Boolean global, Integer launchYear,
                   String types, String counties) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.date = date;
        this.localName = localName;
        this.name = name;
        this.fixed = fixed;
        this.global = global;
        this.launchYear = launchYear;
        this.types = types;
        this.counties = counties;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateHoliday(LocalDate date, String localName, String name, Boolean fixed,
                              Boolean global, Integer launchYear, String types, String counties,
                              LocalDateTime createdAt) {
        this.date = date;
        this.localName = localName;
        this.name = name;
        this.fixed = fixed;
        this.global = global;
        this.launchYear = launchYear;
        this.types = types;
        this.counties = counties;
        this.createdAt = createdAt;
        this.updatedAt = LocalDateTime.now();  // 수정 시간만 업데이트
    }
}