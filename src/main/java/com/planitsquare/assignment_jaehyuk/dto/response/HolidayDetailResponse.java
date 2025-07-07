package com.planitsquare.assignment_jaehyuk.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Schema(description = "공휴일 상세 정보 응답")
public class HolidayDetailResponse {

    @Schema(description = "공휴일 고유 ID",
            example = "1")
    private Long id;

    @Schema(description = "국가 코드 (ISO 2자리)",
            example = "KR",
            maxLength = 2)
    private String countryCode;

    @Schema(description = "국가명",
            example = "Korea",
            maxLength = 100)
    private String countryName;

    @Schema(description = "공휴일 날짜",
            example = "2024-01-01",
            type = "string",
            format = "date")
    private LocalDate date;

    @Schema(description = "현지명",
            example = "신정",
            maxLength = 200)
    private String localName;

    @Schema(description = "공휴일명",
            example = "New Year's Day",
            maxLength = 200)
    private String name;

    @Schema(description = "고정 공휴일 여부",
            example = "true",
            allowableValues = {"true", "false"})
    private Boolean fixed;

    @Schema(description = "전국 공휴일 여부",
            example = "true",
            allowableValues = {"true", "false"})
    private Boolean global;

    @Schema(description = "공휴일 시행 연도",
            example = "1949",
            minimum = "1900",
            maximum = "2030")
    private Integer launchYear;

    @Schema(description = "공휴일 유형 목록",
            example = "[\"Public\", \"National\"]")
    private List<String> types;

    @Schema(description = "적용 지역 목록",
            example = "[\"Seoul\", \"Busan\"]")
    private List<String> counties;

    @Schema(description = "생성일시",
            example = "2024-01-01T10:00:00",
            type = "string",
            format = "date-time")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시",
            example = "2024-01-01T10:00:00",
            type = "string",
            format = "date-time")
    private LocalDateTime updatedAt;

    @Builder
    @QueryProjection
    public HolidayDetailResponse(Long id, String countryCode, String countryName, LocalDate date, String localName, String name, Boolean fixed, Boolean global, Integer launchYear, List<String> types, List<String> counties, LocalDateTime createdAt, LocalDateTime updatedAt) {
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