package com.planitsquare.assignment_jaehyuk.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Schema(description = "공휴일 기본 정보 응답")
public class HolidayResponse {

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