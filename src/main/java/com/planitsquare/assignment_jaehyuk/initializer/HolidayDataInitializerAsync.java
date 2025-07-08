package com.planitsquare.assignment_jaehyuk.initializer;

import com.planitsquare.assignment_jaehyuk.client.NagerDataApiClientAsync;
import com.planitsquare.assignment_jaehyuk.dto.external.CountryDto;
import com.planitsquare.assignment_jaehyuk.dto.external.HolidayDto;
import com.planitsquare.assignment_jaehyuk.serivce.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "holiday.initializer.async", havingValue = "true", matchIfMissing = true)
class HolidayDataInitializerAsync implements ApplicationRunner {

    private final NagerDataApiClientAsync nagerDateApiClient;
    private final HolidayService holidayService;

    @Value("${holiday.data-initialization.start-year}")
    private int startYear;

    @Value("${holiday.data-initialization.end-year}")
    private int endYear;

    private static final int MAX_CONCURRENT_COUNTRIES = 30;
    private int maxConcurrentCountries = MAX_CONCURRENT_COUNTRIES;
    private static final int MAX_CONCURRENT_YEARS = 6;

    @Override
    public void run(ApplicationArguments args) {
        StopWatch stopWatch = new StopWatch("HolidayDataInitializer");
        String startTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("공휴일 데이터 초기화 시작 - 시작 시간: {}", startTimeStr);
        stopWatch.start("공휴일 데이터 전체 초기화");

        try {
            initializeHolidayDataAsyncBulk()
                    .doOnSuccess(totalCount -> {
                        stopWatch.stop();
                        String endTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        log.info("공휴일 데이터 초기화 완료 - 완료 시간: {}", endTimeStr);
                        log.info("총 저장된 공휴일: {} 개", totalCount);
                        log.info("실행 시간 상세:\n{}", stopWatch.prettyPrint());
                        log.info("총 소요 시간: {}초", String.format("%.3f", stopWatch.getTotalTimeSeconds()));
                    })
                    .doOnError(e -> {
                        stopWatch.stop();
                        String endTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        log.error("공휴일 데이터 초기화 실패 - 완료 시간: {}", endTimeStr);
                        log.info("실행 시간 상세:\n{}", stopWatch.prettyPrint());
                    })
                    .block();

        } catch (Exception e) {
            stopWatch.stop();
            log.error("공휴일 데이터 초기화 중 예외 발생", e);
        }
    }

    private Mono<Integer> initializeHolidayDataAsyncBulk() {
        return getAvailableCountriesAsync()
                .flatMap(countries -> {
                    if (countries.isEmpty()) {
                        log.warn("사용 가능한 국가 목록이 없습니다");
                        return Mono.just(0);
                    }
                    maxConcurrentCountries = countries.size();
                    log.info("총 {} 개국의 공휴일 데이터 수집", countries.size());

                    Map<String, String> countryNameMap = createCountryNameMap(countries);

                    return collectAllHolidaysAsync(countries, countryNameMap)
                            .flatMap(allHolidays -> saveAllHolidaysBulk(allHolidays, countryNameMap));
                });
    }

    private Map<String, String> createCountryNameMap(List<CountryDto> countries) {
        Map<String, String> countryNameMap = countries.stream()
                .collect(Collectors.toMap(
                        CountryDto::getCountryCode,
                        CountryDto::getName,
                        (existing, replacement) -> existing
                ));

        log.info("국가 매핑 맵 생성 완료: {} 개국", countryNameMap.size());
        return countryNameMap;
    }

    private Mono<List<CountryDto>> getAvailableCountriesAsync() {
        return nagerDateApiClient.getAvailableCountries()
                .doOnSuccess(countries -> log.info("국가 목록 조회 완료: {} 개국", countries.size()))
                .onErrorResume(e -> {
                    log.error("국가 목록 조회 실패", e);
                    return Mono.just(List.of());
                });
    }

    // 🔄 수정: 국가 매핑 맵을 함께 전달
    private Mono<List<HolidayDto>> collectAllHolidaysAsync(List<CountryDto> countries, Map<String, String> countryNameMap) {
        log.info("모든 국가의 공휴일 데이터 수집 시작");

        return Flux.fromIterable(countries)
                .flatMap(this::collectCountryHolidaysAsync, maxConcurrentCountries)
                .collectList()
                .map(holidayLists -> {
                    List<HolidayDto> allHolidays = holidayLists.stream()
                            .flatMap(List::stream)
                            .toList();

                    log.info("전체 공휴일 데이터 수집 완료: {} 개", allHolidays.size());
                    return allHolidays;
                });
    }

    private Mono<List<HolidayDto>> collectCountryHolidaysAsync(CountryDto country) {
        log.debug("{}({}) 공휴일 데이터 수집 시작", country.getName(), country.getCountryCode());

        return Flux.range(startYear, endYear - startYear + 1)
                .flatMap(year ->
                        fetchHolidaysForYear(country, year), MAX_CONCURRENT_YEARS
                )
                .collectList()
                .map(holidayLists -> {
                    // 해당 국가의 모든 연도 공휴일을 합치기
                    List<HolidayDto> countryHolidays = holidayLists.stream()
                            .flatMap(List::stream)
                            .toList();

                    if (!countryHolidays.isEmpty()) {
                        log.info("{}({}) 공휴일 데이터 수집 완료: {} 개",
                                country.getName(), country.getCountryCode(), countryHolidays.size());
                    } else {
                        log.debug("{}({}) 공휴일 데이터 없음", country.getName(), country.getCountryCode());
                    }

                    return countryHolidays;
                })
                .onErrorResume(e -> {
                    log.warn("국가 {}({}) 공휴일 데이터 수집 실패: {}",
                            country.getName(), country.getCountryCode(), e.getMessage());
                    return Mono.just(List.of()); // 실패해도 빈 리스트 반환
                });
    }

    private Mono<List<HolidayDto>> fetchHolidaysForYear(CountryDto country, int year) {
        return nagerDateApiClient.getPublicHolidays(country.getCountryCode(), year)
                .doOnSuccess(holidays -> {
                    if (!holidays.isEmpty()) {
                        log.debug("{}({}) {}년 공휴일 {} 개 수집",
                                country.getName(), country.getCountryCode(), year, holidays.size());
                    }
                })
                .onErrorResume(e -> {
                    log.warn("{}({}) {}년 데이터 조회 실패: {}",
                            country.getName(), country.getCountryCode(), year, e.getMessage());
                    return Mono.just(List.of());
                });
    }

    private Mono<Integer> saveAllHolidaysBulk(List<HolidayDto> allHolidays, Map<String, String> countryNameMap) {
        if (allHolidays.isEmpty()) {
            log.warn("저장할 공휴일 데이터가 없습니다");
            return Mono.just(0);
        }

        log.info("벌크 저장 시작: {} 개의 공휴일 데이터", allHolidays.size());

        return Mono.fromCallable(() -> {
                    try {
                        List<HolidayDto> savedHolidays = holidayService.saveAllHolidaysBulk(allHolidays, countryNameMap);
                        log.info("벌크 저장 완료: {} 개의 공휴일 데이터 저장", savedHolidays.size());
                        return savedHolidays.size();

                    } catch (Exception e) {
                        log.error("벌크 저장 실패", e);
                        throw new RuntimeException("벌크 저장 실패", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}