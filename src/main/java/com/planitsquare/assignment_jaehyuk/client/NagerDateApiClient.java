package com.planitsquare.assignment_jaehyuk.client;

import com.planitsquare.assignment_jaehyuk.dto.external.CountryDto;
import com.planitsquare.assignment_jaehyuk.dto.external.HolidayDto;
import com.planitsquare.assignment_jaehyuk.error.ErrorCode;
import com.planitsquare.assignment_jaehyuk.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NagerDateApiClient {

    private final WebClient webClient;

    public List<CountryDto> getAvailableCountries() {
        try {
            log.debug("국가 목록 조회 요청");

            List<CountryDto> countries = webClient
                    .get()
                    .uri("/AvailableCountries")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            Mono.error(new BusinessException(ErrorCode.COUNTRY_API_CALL_FAILED))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new BusinessException(ErrorCode.COUNTRY_API_CALL_FAILED))
                    )
                    .bodyToMono(new ParameterizedTypeReference<List<CountryDto>>() {})
                    .timeout(Duration.ofSeconds(3))
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                    .block();

            log.info("국가 목록 조회 완료: {} 개국", countries != null ? countries.size() : 0);
            return countries;

        } catch (WebClientResponseException e) {
            log.error("국가 목록 조회 실패 - HTTP Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.COUNTRY_API_CALL_FAILED);
        } catch (Exception e) {
            log.error("국가 목록 조회 중 예외 발생", e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_TIMEOUT);
        }
    }

    public List<HolidayDto> getPublicHolidays(String countryCode, int year) {
        try {
            log.debug("공휴일 조회 요청 - 연도: {}, 국가코드: {}", year, countryCode);

            List<HolidayDto> holidays = webClient
                    .get()
                    .uri("/PublicHolidays/{year}/{countryCode}", year, countryCode)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            Mono.error(new BusinessException(ErrorCode.COUNTRY_API_CALL_FAILED))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new BusinessException(ErrorCode.COUNTRY_API_CALL_FAILED))
                    )
                    .bodyToMono(new ParameterizedTypeReference<List<HolidayDto>>() {})
                    .timeout(Duration.ofSeconds(3))
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                    .block();

            log.info("공휴일 조회 완료 - 연도: {}, 국가코드: {}, 공휴일 수: {}",
                    year, countryCode, holidays != null ? holidays.size() : 0);
            return holidays;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                log.warn("공휴일 데이터 없음 - 연도: {}, 국가코드: {}", year, countryCode);
                return List.of();
            }
            log.error("공휴일 조회 실패 - 연도: {}, 국가코드: {}, HTTP Status: {}, Body: {}",
                    year, countryCode, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.HOLIDAY_API_CALL_FAILED);
        } catch (Exception e) {
            log.error("공휴일 조회 중 예외 발생 - 연도: {}, 국가코드: {}", year, countryCode, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_TIMEOUT);
        }
    }
}
