package com.planitsquare.assignment_jaehyuk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HolidayUpdateForm {

    @NotBlank(message = "국가 코드는 필수입니다")
    private String countryCode;
    @NotBlank(message = "국가명은 필수입니다")
    private String countryName;
    @NotNull(message = "연도는 필수입니다")
    private Integer year;
}
