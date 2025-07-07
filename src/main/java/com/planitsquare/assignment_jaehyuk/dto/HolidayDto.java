package com.planitsquare.assignment_jaehyuk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class HolidayDto {

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("localName")
    private String localName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("fixed")
    private Boolean fixed;

    @JsonProperty("global")
    private Boolean global;

    @JsonProperty("counties")
    private List<String> counties;

    @JsonProperty("launchYear")
    private Integer launchYear;

    @JsonProperty("types")
    private List<String> types;

    @Builder
    public HolidayDto(LocalDate date, String localName, String name, String countryCode, Boolean fixed, Boolean global, List<String> counties, Integer launchYear, List<String> types) {
        this.date = date;
        this.localName = localName;
        this.name = name;
        this.countryCode = countryCode;
        this.fixed = fixed;
        this.global = global;
        this.counties = counties;
        this.launchYear = launchYear;
        this.types = types;
    }
}
