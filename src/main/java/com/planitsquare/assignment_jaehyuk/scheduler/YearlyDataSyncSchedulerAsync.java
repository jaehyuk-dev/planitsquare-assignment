package com.planitsquare.assignment_jaehyuk.scheduler;

import com.planitsquare.assignment_jaehyuk.client.NagerDataApiClientAsync;
import com.planitsquare.assignment_jaehyuk.dto.external.CountryDto;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidayUpdateForm;
import com.planitsquare.assignment_jaehyuk.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "holiday.scheduler.async", havingValue = "true", matchIfMissing = true)
public class YearlyDataSyncSchedulerAsync {

    private final NagerDataApiClientAsync nagerDateApiClient;
    private final HolidayService holidayService;

    @Value("${holiday.scheduler.concurrency.max-countries:30}")
    private int maxConcurrentCountries;
    
    @Value("${holiday.scheduler.concurrency.max-years-per-country:6}")
    private int maxConcurrentYears;

    @Scheduled(cron = "0 0 1 2 1 ?", zone = "Asia/Seoul")
    public void syncYearlyDataAsync() {
        StopWatch stopWatch = new StopWatch("YearlyDataSyncAsync");
        String startTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("=== 연간 공휴일 데이터 동기화 시작 (고속 비동기) - 시작 시간: {} ===", startTimeStr);
        stopWatch.start("연간 공휴일 데이터 동기화");

        try {
            int currentYear = LocalDate.now().getYear();
            int previousYear = currentYear - 1;

            log.info("동기화 대상 연도: {}년, {}년", previousYear, currentYear);

            syncAllCountriesDataAsync(previousYear, currentYear)
                    .doOnSuccess(totalSyncCount -> {
                        stopWatch.stop();
                        String endTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        log.info("=== 연간 공휴일 데이터 동기화 완료 - 완료 시간: {} ===", endTimeStr);
                        log.info("총 동기화된 국가 수: {} 개국", totalSyncCount);
                        log.info("실행 시간 상세:\n{}", stopWatch.prettyPrint());
                        log.info("총 소요 시간: {}초", String.format("%.3f", stopWatch.getTotalTimeSeconds()));
                    })
                    .doOnError(e -> {
                        stopWatch.stop();
                        String endTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        log.error("=== 연간 공휴일 데이터 동기화 실패 - 완료 시간: {} ===", endTimeStr);
                        log.info("실행 시간 상세:\n{}", stopWatch.prettyPrint());
                    })
                    .block();

        } catch (Exception e) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            log.error("연간 공휴일 데이터 동기화 중 예외 발생", e);
        }
    }

    private Mono<Integer> syncAllCountriesDataAsync(int previousYear, int currentYear) {
        return getAvailableCountriesAsync()
                .flatMap(countries -> {
                    if (countries.isEmpty()) {
                        log.warn("사용 가능한 국가 목록이 없습니다");
                        return Mono.just(0);
                    }

                    log.info("총 {} 개국의 공휴일 데이터를 비동기로 동기화합니다", countries.size());

                    Map<String, String> countryNameMap = createCountryNameMap(countries);

                    return Flux.fromIterable(countries)
                            .flatMap(country ->
                                    syncCountryDataAsync(country, countryNameMap, previousYear, currentYear), maxConcurrentCountries)
                            .collectList()
                            .map(results -> {
                                int successCount = (int) results.stream().filter(success -> success).count();
                                log.info("동기화 완료: 성공 {} 개국 / 전체 {} 개국", successCount, countries.size());
                                return successCount;
                            });
                });
    }

    private Mono<Boolean> syncCountryDataAsync(CountryDto country, Map<String, String> countryNameMap, int previousYear, int currentYear) {
        log.debug("{}({}) 공휴일 데이터 동기화 시작", country.getName(), country.getCountryCode());

        return Flux.just(previousYear, currentYear)
                .flatMap(year ->
                        syncCountryYearDataAsync(country, countryNameMap, year), maxConcurrentCountries)
                .collectList()
                .map(results -> {
                    boolean allSuccess = results.stream().allMatch(success -> success);
                    if (allSuccess) {
                        log.info("{}({}) 동기화 완료: {}년, {}년",
                                country.getName(), country.getCountryCode(), previousYear, currentYear);
                    } else {
                        log.warn("{}({}) 동기화 일부 실패", country.getName(), country.getCountryCode());
                    }
                    return allSuccess;
                })
                .onErrorResume(e -> {
                    log.error("국가 {}({}) 동기화 실패: {}",
                            country.getName(), country.getCountryCode(), e.getMessage());
                    return Mono.just(false);
                });
    }

    private Mono<Boolean> syncCountryYearDataAsync(CountryDto country, Map<String, String> countryNameMap, int year) {
        return Mono.fromCallable(() -> {
                    try {
                        HolidayUpdateForm updateForm = new HolidayUpdateForm();
                        updateForm.setCountryCode(country.getCountryCode());
                        updateForm.setCountryName(countryNameMap.get(country.getCountryCode()));
                        updateForm.setYear(year);

                        holidayService.updateHolidayList(updateForm);

                        log.debug("{}({}) {}년 동기화 완료",
                                country.getName(), country.getCountryCode(), year);
                        return true;

                    } catch (Exception e) {
                        log.warn("{}({}) {}년 동기화 실패: {}",
                                country.getName(), country.getCountryCode(), year, e.getMessage());
                        return false;
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<List<CountryDto>> getAvailableCountriesAsync() {
        return nagerDateApiClient.getAvailableCountries()
                .doOnSuccess(countries -> log.info("국가 목록 조회 완료: {} 개국", countries.size()))
                .onErrorResume(e -> {
                    log.error("국가 목록 조회 실패", e);
                    return Mono.just(List.of());
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

    public void manualSyncTrigger() {
        log.info("=== 수동 동기화 트리거 실행 ===");
        syncYearlyDataAsync();
    }
}