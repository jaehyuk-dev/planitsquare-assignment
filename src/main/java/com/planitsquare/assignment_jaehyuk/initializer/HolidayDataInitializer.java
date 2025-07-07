package com.planitsquare.assignment_jaehyuk.initializer;


import com.planitsquare.assignment_jaehyuk.client.NagerDateApiClient;
import com.planitsquare.assignment_jaehyuk.dto.external.CountryDto;
import com.planitsquare.assignment_jaehyuk.dto.external.HolidayDto;
import com.planitsquare.assignment_jaehyuk.serivce.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class HolidayDataInitializer implements ApplicationRunner {

    private final NagerDateApiClient nagerDateApiClient;
    private final HolidayService holidayService;

    @Value("${holiday.data-initialization.start-year}")
    private int startYear;

    @Value("${holiday.data-initialization.end-year}")
    private int endYear;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        StopWatch stopWatch = new StopWatch("HolidayDataInitializer");
        String startTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("공휴일 데이터 초기화 시작 - 시작 시간: {}", startTimeStr);

        stopWatch.start("공휴일 데이터 전체 초기화");

        try {
            initializeHolidayData();

            stopWatch.stop();
            String endTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            log.info("공휴일 데이터 초기화 완료 - 완료 시간: {}", endTimeStr);
            log.info("실행 시간 상세:\n{}", stopWatch.prettyPrint());
            log.info("총 소요 시간: {}초", String.format("%.3f", stopWatch.getTotalTimeSeconds()));

        } catch (Exception e) {
            stopWatch.stop();
            String endTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            log.info("공휴일 데이터 초기화 완료 - 완료 시간: {}", endTimeStr);
            log.info("실행 시간 상세:\n{}", stopWatch.prettyPrint());
            log.info("총 소요 시간: {}초", String.format("%.3f", stopWatch.getTotalTimeSeconds()));
        }
    }

    private void initializeHolidayData() {
        List<CountryDto> countries = getAvailableCountries();

        if (countries.isEmpty()) {
            log.warn("사용 가능한 국가 목록이 없습니다");
            return;
        }

        log.info("총 {} 개국의 공휴일 데이터를 초기화합니다", countries.size());

        for (CountryDto country : countries) {
            initializeCountryHolidays(country);
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

    private void initializeCountryHolidays(CountryDto country) {
        try {
            log.debug("{}({}) 공휴일 데이터 초기화 시작", country.getName(), country.getCountryCode());

            List<HolidayDto> allHolidayList = new ArrayList<>();

            IntStream.rangeClosed(startYear, endYear)
                    .forEach(year -> {
                        try {
                            List<HolidayDto> holidayList = nagerDateApiClient.getPublicHolidays(country.getCountryCode(), year);

                            if (!holidayList.isEmpty()) {
                                allHolidayList.addAll(holidayList);
                                log.debug("{}({}) {}년 공휴일 {} 개 수집", country.getName(), country.getCountryCode(), year, holidayList.size());
                            }

                            Thread.sleep(100);

                        } catch (Exception e) {
                            log.warn("{}({}) {}년 데이터 조회 실패: {}", country.getName(), country.getCountryCode(), year, e.getMessage());
                        }

                    });


            holidayService.saveHolidayList(country.getName(), allHolidayList);

            log.debug("{}({}) 공휴일 데이터 초기화 완료", country.getName(), country.getCountryCode());

        } catch (Exception e) {
            log.warn("국가 {}({}) 공휴일 데이터 초기화 실패: {}",
                    country.getName(), country.getCountryCode(), e.getMessage());
        }
    }
}
