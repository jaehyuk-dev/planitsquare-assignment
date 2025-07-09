package com.planitsquare.assignment_jaehyuk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "공휴일 고급 검색 조건")
public class HolidaySearchCondition {

    @Schema(description = "국가명 (포함 검색)",
            example = "Korea",
            maxLength = 100)
    @Size(max = 100, message = "국가명은 100자를 초과할 수 없습니다")
    private String countryName;

    @Schema(description = "검색 시작 날짜",
            example = "2024-01-01",
            type = "string",
            format = "date")
    @PastOrPresent(message = "시작 날짜는 현재 날짜 이전이어야 합니다")
    private LocalDate startDate;

    @Schema(description = "검색 종료 날짜",
            example = "2024-12-31",
            type = "string",
            format = "date")
    @PastOrPresent(message = "종료 날짜는 현재 날짜 이전이어야 합니다")
    private LocalDate endDate;

    @Schema(description = "현지명 (포함 검색)",
            example = "새해",
            maxLength = 200)
    @Size(max = 200, message = "현지명은 200자를 초과할 수 없습니다")
    private String localName;

    @Schema(description = "공휴일명 (포함 검색)",
            example = "New Year",
            maxLength = 200)
    @Size(max = 200, message = "공휴일명은 200자를 초과할 수 없습니다")
    private String name;

    @Schema(description = "전국 공휴일 여부",
            example = "true",
            allowableValues = {"true", "false"})
    private Boolean global;

    @Schema(description = "공휴일 시행 연도",
            example = "2024",
            minimum = "2020",
            maximum = "2025")
    @Min(value = 2020, message = "시행 연도는 2020년 이후여야 합니다")
    @Max(value = 2025, message = "시행 연도는 2025년 이전이어야 합니다")
    private Integer launchYear;

    @Schema(description = "정렬 기준",
            example = "date",
            allowableValues = {"date", "countryName", "name"})
    private String sortBy;

    @Schema(description = "정렬 방향",
            example = "asc",
            allowableValues = {"asc", "desc"})
    private String sortDirection;
}