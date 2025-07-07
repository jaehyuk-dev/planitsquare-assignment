package com.planitsquare.assignment_jaehyuk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "공휴일 데이터 삭제 요청")
public class HolidayDeleteForm {

    @Schema(description = "국가 코드 (ISO 2자리)",
            example = "KR",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 2)
    @NotBlank(message = "국가 코드는 필수입니다")
    private String countryCode;

    @Schema(description = "국가명",
            example = "Korea",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 100)
    @NotBlank(message = "국가명은 필수입니다")
    private String countryName;

    @Schema(description = "삭제할 공휴일 연도",
            example = "2024",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "2020",
            maximum = "2030")
    @NotNull(message = "연도는 필수입니다")
    private Integer year;
}