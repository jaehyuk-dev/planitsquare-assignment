package com.planitsquare.assignment_jaehyuk.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class HolidayResponse {
    private Long id;

    private String countryCode;

    private String countryName;

    private LocalDate date;

    private String localName;

    private String name;

    @Builder
    @QueryProjection
    public HolidayResponse(Long id, String countryCode, String countryName, LocalDate date, String localName, String name) {
        this.id = id;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.date = date;
        this.localName = localName;
        this.name = name;
    }
}
