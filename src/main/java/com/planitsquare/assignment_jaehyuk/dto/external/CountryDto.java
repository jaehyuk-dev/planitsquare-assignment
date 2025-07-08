package com.planitsquare.assignment_jaehyuk.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CountryDto {

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("name")
    private String name;

    @Builder
    public CountryDto(String countryCode, String name) {
        this.countryCode = countryCode;
        this.name = name;
    }
}

