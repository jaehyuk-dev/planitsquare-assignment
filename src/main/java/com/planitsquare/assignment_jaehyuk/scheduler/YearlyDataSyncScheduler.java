package com.planitsquare.assignment_jaehyuk.scheduler;

import com.planitsquare.assignment_jaehyuk.client.NagerDateApiClient;
import com.planitsquare.assignment_jaehyuk.dto.external.CountryDto;
import com.planitsquare.assignment_jaehyuk.dto.external.HolidayDto;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidayUpdateForm;
import com.planitsquare.assignment_jaehyuk.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class YearlyDataSyncScheduler {

    private final NagerDateApiClient nagerDateApiClient;
    private final HolidayService holidayService;

    private final int endYear = LocalDate.now().getYear();
    private final int startYear = endYear - 1;

    @Scheduled(cron = "0 0 1 2 1 ?", zone = "Asia/Seoul")
    public void syncYearlyData() {
        StopWatch stopWatch = new StopWatch("HolidayDataInitializer");
        String startTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("공휴일 데이터 동기화 시작 - 시작 시간: {}", startTimeStr);

        stopWatch.start("공휴일 데이터 전체 동기화");

        try {
            syncHolidayDataForYear();

            stopWatch.stop();
            String endTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            log.info("공휴일 데이터 동기화 완료 - 완료 시간: {}", endTimeStr);
            log.info("실행 시간 상세:\n{}", stopWatch.prettyPrint());
            log.info("총 소요 시간: {}초", String.format("%.3f", stopWatch.getTotalTimeSeconds()));

        } catch (Exception e) {
            stopWatch.stop();
            String endTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            log.info("공휴일 데이터 동기화 완료 - 완료 시간: {}", endTimeStr);
            log.info("실행 시간 상세:\n{}", stopWatch.prettyPrint());
            log.info("총 소요 시간: {}초", String.format("%.3f", stopWatch.getTotalTimeSeconds()));
        }
    }

    private void syncHolidayDataForYear() {
        List<CountryDto> countries = getAvailableCountries();

        if (countries.isEmpty()) {
            log.warn("사용 가능한 국가 목록이 없습니다");
            return;
        }

        log.info("총 {} 개국의 공휴일 데이터를 초기화합니다", countries.size());

        for (CountryDto country : countries) {
            syncYearHolidays(country);
        }
    }

    private List<CountryDto> getAvailableCountries() {
        try {
            return nagerDateApiClient.getAvailableCountries();
        } catch (Exception e) {
            log.error("국가 목록 조회 실패", e);
            return List.of();
        }
    }

    private void syncYearHolidays(CountryDto country) {
        try {
            log.debug("{}({}) 공휴일 최근 2개년 데이터 동기화 시작", country.getName(), country.getCountryCode());

            List<HolidayDto> allHolidayList = new ArrayList<>();

            IntStream.rangeClosed(startYear, endYear)
                    .forEach(year -> {
                        try {
                            HolidayUpdateForm updateForm = new HolidayUpdateForm();
                            updateForm.setCountryCode(country.getCountryCode());
                            updateForm.setCountryName(country.getName());
                            updateForm.setYear(year);
                            holidayService.updateHolidayList(updateForm);

                            Thread.sleep(100);

                        } catch (Exception e) {
                            log.warn("{}({}) {}년 데이터 조회 실패: {}", country.getName(), country.getCountryCode(), year, e.getMessage());
                        }

                    });

            log.debug("{}({}) 공휴일 데이터 동기화 완료", country.getName(), country.getCountryCode());

        } catch (Exception e) {
            log.warn("국가 {}({}) 공휴일 데이터 동기화 실패: {}",
                    country.getName(), country.getCountryCode(), e.getMessage());
        }
    }
}