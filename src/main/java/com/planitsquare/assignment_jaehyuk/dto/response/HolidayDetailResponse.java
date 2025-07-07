package com.planitsquare.assignment_jaehyuk.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class HolidayDetailResponse {

    private Long id;

    private String countryCode;

    private String countryName;

    private LocalDate date;

    private String localName;

    private String name;

    private Boolean fixed;

    private Boolean global;

    private Integer launchYear;

    private String types;

    private String counties;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @QueryProjection
    public HolidayDetailResponse(Long id, String countryCode, String countryName, LocalDate date, String localName, String name, Boolean fixed, Boolean global, Integer launchYear, String types, String counties, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
