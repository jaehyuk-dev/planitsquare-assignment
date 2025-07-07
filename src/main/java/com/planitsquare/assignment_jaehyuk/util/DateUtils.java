package com.planitsquare.assignment_jaehyuk.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;

@UtilityClass
public class DateUtils {

    /**
     * 날짜 범위를 나타내는 불변 객체
     */
    public record DateRange(LocalDate startDate, LocalDate endDate) {

        /**
         * 연도 기반 DateRange 생성
         */
        public static DateRange ofYear(int year) {
            return new DateRange(
                    LocalDate.of(year, 1, 1),
                    LocalDate.of(year, 12, 31)
            );
        }

        /**
         * 월 기반 DateRange 생성
         */
        public static DateRange ofMonth(int year, int month) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            return new DateRange(start, end);
        }
    }

    /**
     * 해당 연도의 시작일 반환 (1월 1일)
     */
    public LocalDate getYearStartDate(int year) {
        return LocalDate.of(year, 1, 1);
    }

    /**
     * 해당 연도의 마지막일 반환 (12월 31일)
     */
    public LocalDate getYearEndDate(int year) {
        return LocalDate.of(year, 12, 31);
    }

    /**
     * 해당 연도의 시작일과 끝일을 DateRange 객체로 반환
     */
    public DateRange getYearRange(int year) {
        return DateRange.ofYear(year);  // 🔥 정적 팩토리 메서드 활용
    }
}