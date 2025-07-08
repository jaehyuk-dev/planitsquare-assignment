package com.planitsquare.assignment_jaehyuk.client;

import com.planitsquare.assignment_jaehyuk.dto.external.CountryDto;
import com.planitsquare.assignment_jaehyuk.dto.external.HolidayDto;
import com.planitsquare.assignment_jaehyuk.error.ErrorCode;
import com.planitsquare.assignment_jaehyuk.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NagerDataApiClientAsync {

    private final WebClient webClient;
    
    @Value("${external.api.nager.timeout:3s}")
    private String timeoutStr;
    
    @Value("${external.api.nager.retry.max-attempts:2}")
    private int retryMaxAttempts;
    
    @Value("${external.api.nager.retry.delay:1s}")
    private String retryDelayStr;
    
    private Duration getTimeout() {
        return Duration.parse("PT" + timeoutStr.toUpperCase());
    }
    
    private Duration getRetryDelay() {
        return Duration.parse("PT" + retryDelayStr.toUpperCase());
    }

    public Mono<List<CountryDto>> getAvailableCountries() {
        log.debug("국가 목록 조회 요청");

        return webClient
                .get()
                .uri("/AvailableCountries")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new BusinessException(ErrorCode.COUNTRY_API_CALL_FAILED))
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new BusinessException(ErrorCode.COUNTRY_API_CALL_FAILED))
                )
                .bodyToMono(CountryDto[].class)
                .map(Arrays::asList)
                .timeout(getTimeout())
                .retryWhen(Retry.fixedDelay(retryMaxAttempts, getRetryDelay()))
                .doOnSuccess(countries ->
                        log.info("국가 목록 조회 완료: {} 개국", countries != null ? countries.size() : 0)
                )
                .doOnError(WebClientResponseException.class, e ->
                        log.error("국가 목록 조회 실패 - HTTP Status: {}, Body: {}",
                                e.getStatusCode(), e.getResponseBodyAsString())
                )
                .doOnError(Exception.class, e ->
                        log.error("국가 목록 조회 중 예외 발생", e)
                )
                .onErrorMap(WebClientResponseException.class, e ->
                        new BusinessException(ErrorCode.COUNTRY_API_CALL_FAILED)
                )
                .onErrorMap(Exception.class, e ->
                        new BusinessException(ErrorCode.EXTERNAL_API_TIMEOUT)
                );
    }

    public Mono<List<HolidayDto>> getPublicHolidays(String countryCode, int year) {
        log.debug("공휴일 조회 요청 - 연도: {}, 국가코드: {}", year, countryCode);

        return webClient
                .get()
                .uri("/PublicHolidays/{year}/{countryCode}", year, countryCode)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new BusinessException(ErrorCode.COUNTRY_API_CALL_FAILED))
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new BusinessException(ErrorCode.COUNTRY_API_CALL_FAILED))
                )
                .bodyToMono(HolidayDto[].class)
                .map(Arrays::asList)
                .timeout(getTimeout())
                .retryWhen(Retry.fixedDelay(retryMaxAttempts, getRetryDelay()))
                .doOnSuccess(holidays ->
                        log.info("공휴일 조회 완료 - 연도: {}, 국가코드: {}, 공휴일 수: {}",
                                year, countryCode, holidays.size())
                )
                .doOnError(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 404) {
                        log.warn("공휴일 데이터 없음 - 연도: {}, 국가코드: {}", year, countryCode);
                    } else {
                        log.error("공휴일 조회 실패 - 연도: {}, 국가코드: {}, HTTP Status: {}, Body: {}",
                                year, countryCode, e.getStatusCode(), e.getResponseBodyAsString());
                    }
                })
                .doOnError(Exception.class, e ->
                        log.error("공휴일 조회 중 예외 발생 - 연도: {}, 국가코드: {}", year, countryCode, e)
                )
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 404) {
                        return Mono.just(List.of());
                    }
                    throw new BusinessException(ErrorCode.HOLIDAY_API_CALL_FAILED);
                })
                .onErrorMap(Exception.class, e ->
                        new BusinessException(ErrorCode.EXTERNAL_API_TIMEOUT)
                );

    }
}
