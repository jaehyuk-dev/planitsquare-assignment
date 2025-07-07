package com.planitsquare.assignment_jaehyuk.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HolidaySearchCondition {
    @Size(max = 100, message = "국가명은 100자를 초과할 수 없습니다")
    private String countryName;      // 국가명 (포함 검색)

    @PastOrPresent(message = "시작 날짜는 현재 날짜 이전이어야 합니다")
    private LocalDate startDate;     // 시작 날짜
    @PastOrPresent(message = "종료 날짜는 현재 날짜 이전이어야 합니다")
    private LocalDate endDate;       // 종료 날짜
    @Size(max = 200, message = "현지명은 200자를 초과할 수 없습니다")
    private String localName;        // 현지명 (포함 검색)
    @Size(max = 200, message = "공휴일명은 200자를 초과할 수 없습니다")
    private String name;             // 공휴일명 (포함 검색)
    private Boolean global;          // 전국 공휴일 여부

    @Min(value = 2020, message = "시행 연도는 2020년 이후여야 합니다")
    @Max(value = 2025, message = "시행 연도는 2025년 이전이어야 합니다")
    private Integer launchYear;      // 정확한 시행 연도

    private String sortBy;           // date, countryName, name
    private String sortDirection;    // asc, desc
}
