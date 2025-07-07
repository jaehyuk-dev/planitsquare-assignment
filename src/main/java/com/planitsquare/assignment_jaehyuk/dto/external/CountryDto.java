package com.planitsquare.assignment_jaehyuk.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CountryDto {

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("name")
    private String name;
}

