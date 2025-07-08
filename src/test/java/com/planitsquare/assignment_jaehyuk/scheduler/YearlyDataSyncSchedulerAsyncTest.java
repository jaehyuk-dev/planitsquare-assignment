package com.planitsquare.assignment_jaehyuk.scheduler;

import com.planitsquare.assignment_jaehyuk.client.NagerDataApiClientAsync;
import com.planitsquare.assignment_jaehyuk.dto.external.CountryDto;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidayUpdateForm;
import com.planitsquare.assignment_jaehyuk.serivce.HolidayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("YearlyDataSyncSchedulerAsync 테스트")
class YearlyDataSyncSchedulerAsyncTest {

    @Mock
    private NagerDataApiClientAsync nagerDateApiClient;

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private YearlyDataSyncSchedulerAsync scheduler;

    private List<CountryDto> mockCountries;
    private int currentYear;
    private int previousYear;

    @BeforeEach
    void setUp() {
        // 🎯 테스트용 모킹 데이터 준비
        currentYear = LocalDate.now().getYear();
        previousYear = currentYear - 1;

        mockCountries = List.of(
                createCountryDto("KR", "대한민국"),
                createCountryDto("US", "미국"),
                createCountryDto("JP", "일본")
        );
    }

    @Test
    @DisplayName("✅ 정상적인 연간 데이터 동기화 테스트")
    void syncYearlyDataAsync_Success() {
        // Given: API 호출이 성공하고 서비스 호출도 성공
        when(nagerDateApiClient.getAvailableCountries())
                .thenReturn(Mono.just(mockCountries));

        doNothing().when(holidayService).updateHolidayList(any(HolidayUpdateForm.class));

        // When & Then: 동기화가 성공적으로 완료되어야 함
        assertDoesNotThrow(() -> {
            scheduler.syncYearlyDataAsync();
        });

        // Verify: API 호출 1번, 서비스 호출은 국가 수 × 2년 = 6번
        verify(nagerDateApiClient, times(1)).getAvailableCountries();
        verify(holidayService, times(6)).updateHolidayList(any(HolidayUpdateForm.class));
    }

    @Test
    @DisplayName("🚨 국가 목록 조회 실패 테스트")
    void syncYearlyDataAsync_CountryListFetchFailed() {
        // Given: API 호출이 실패
        when(nagerDateApiClient.getAvailableCountries())
                .thenReturn(Mono.error(new RuntimeException("API 호출 실패")));

        // When & Then: 예외가 발생해도 애플리케이션은 중단되지 않아야 함
        assertDoesNotThrow(() -> {
            scheduler.syncYearlyDataAsync();
        });

        // Verify: API 호출 1번, 서비스 호출은 0번
        verify(nagerDateApiClient, times(1)).getAvailableCountries();
        verify(holidayService, never()).updateHolidayList(any(HolidayUpdateForm.class));
    }

    @Test
    @DisplayName("📭 빈 국가 목록 테스트")
    void syncYearlyDataAsync_EmptyCountryList() {
        // Given: 빈 국가 목록
        when(nagerDateApiClient.getAvailableCountries())
                .thenReturn(Mono.just(List.of()));

        // When & Then: 정상적으로 처리되어야 함
        assertDoesNotThrow(() -> {
            scheduler.syncYearlyDataAsync();
        });

        // Verify: API 호출 1번, 서비스 호출은 0번
        verify(nagerDateApiClient, times(1)).getAvailableCountries();
        verify(holidayService, never()).updateHolidayList(any(HolidayUpdateForm.class));
    }

    @Test
    @DisplayName("⚠️ 일부 국가 동기화 실패 테스트")
    void syncYearlyDataAsync_PartialFailure() {
        // Given: API 호출은 성공하지만 일부 서비스 호출이 실패
        when(nagerDateApiClient.getAvailableCountries())
                .thenReturn(Mono.just(mockCountries));

        // 첫 번째 국가는 성공, 두 번째 국가는 실패, 세 번째 국가는 성공
        doNothing()
                .doThrow(new RuntimeException("US 동기화 실패"))
                .doNothing()
                .doThrow(new RuntimeException("US 동기화 실패"))
                .doNothing()
                .doNothing()
                .when(holidayService).updateHolidayList(any(HolidayUpdateForm.class));

        // When & Then: 일부 실패해도 전체 프로세스는 완료되어야 함
        assertDoesNotThrow(() -> {
            scheduler.syncYearlyDataAsync();
        });

        // Verify: 모든 호출이 시도되어야 함
        verify(nagerDateApiClient, times(1)).getAvailableCountries();
        verify(holidayService, times(6)).updateHolidayList(any(HolidayUpdateForm.class));
    }

    @Test
    @DisplayName("🛠️ 수동 동기화 트리거 테스트")
    void manualSyncTrigger_Success() {
        // Given: 정상적인 모킹 설정
        when(nagerDateApiClient.getAvailableCountries())
                .thenReturn(Mono.just(mockCountries));

        doNothing().when(holidayService).updateHolidayList(any(HolidayUpdateForm.class));

        // When & Then: 수동 트리거가 정상적으로 동작해야 함
        assertDoesNotThrow(() -> {
            scheduler.manualSyncTrigger();
        });

        // Verify: 내부적으로 syncYearlyDataAsync가 호출되므로 동일한 검증
        verify(nagerDateApiClient, times(1)).getAvailableCountries();
        verify(holidayService, times(6)).updateHolidayList(any(HolidayUpdateForm.class));
    }

    @Test
    @DisplayName("🔍 HolidayUpdateForm 올바른 파라미터 설정 테스트")
    void syncYearlyDataAsync_CorrectParameters() {
        // Given
        when(nagerDateApiClient.getAvailableCountries())
                .thenReturn(Mono.just(List.of(createCountryDto("KR", "대한민국"))));

        doNothing().when(holidayService).updateHolidayList(any(HolidayUpdateForm.class));

        // When
        scheduler.syncYearlyDataAsync();

        // Then: HolidayUpdateForm의 파라미터가 올바르게 설정되었는지 확인
        verify(holidayService, times(2)).updateHolidayList(argThat(form -> {
            return "KR".equals(form.getCountryCode()) &&
                    "대한민국".equals(form.getCountryName()) &&
                    (form.getYear().equals(previousYear) || form.getYear().equals(currentYear));
        }));
    }

    @Test
    @DisplayName("🌐 비동기 Mono 동작 검증 테스트")
    void getAvailableCountriesAsync_ReactorTest() {
        // Given: Reactor StepVerifier를 사용한 비동기 테스트
        when(nagerDateApiClient.getAvailableCountries())
                .thenReturn(Mono.just(mockCountries));

        // When & Then: Mono가 올바르게 동작하는지 검증
        StepVerifier.create(nagerDateApiClient.getAvailableCountries())
                .expectNext(mockCountries)
                .verifyComplete();
    }

    @Test
    @DisplayName("💥 심각한 예외 발생 테스트")
    void syncYearlyDataAsync_SevereException() {
        // Given: 예상치 못한 심각한 예외 발생
        when(nagerDateApiClient.getAvailableCountries())
                .thenThrow(new OutOfMemoryError("메모리 부족"));

        // When & Then: 심각한 예외도 처리되어야 함
        assertDoesNotThrow(() -> {
            scheduler.syncYearlyDataAsync();
        });
    }

    @Test
    @DisplayName("🔄 재시도 시나리오 테스트")
    void syncYearlyDataAsync_RetryScenario() {
        // Given: 첫 번째 호출은 실패, 두 번째 호출은 성공
        when(nagerDateApiClient.getAvailableCountries())
                .thenReturn(Mono.error(new RuntimeException("일시적 오류")))
                .thenReturn(Mono.just(mockCountries));

        // When: 첫 번째 실행 (실패)
        scheduler.syncYearlyDataAsync();

        // When: 두 번째 실행 (성공)
        scheduler.syncYearlyDataAsync();

        // Then: 총 2번의 API 호출과 1번의 성공적인 서비스 호출
        verify(nagerDateApiClient, times(2)).getAvailableCountries();
        verify(holidayService, times(6)).updateHolidayList(any(HolidayUpdateForm.class));
    }

    // 🛠️ 헬퍼 메서드들

    private CountryDto createCountryDto(String countryCode, String name) {
        return CountryDto.builder()
                .countryCode(countryCode)
                .name(name)
                .build();
    }

    /**
     * 🎯 성능 테스트 - 대량 국가 처리
     */
    @Test
    @DisplayName("🚀 대량 국가 동기화 성능 테스트")
    void syncYearlyDataAsync_PerformanceTest() {
        // Given: 100개국 데이터
        List<CountryDto> manyCountries = createManyCountries(100);
        when(nagerDateApiClient.getAvailableCountries())
                .thenReturn(Mono.just(manyCountries));

        doNothing().when(holidayService).updateHolidayList(any(HolidayUpdateForm.class));

        long startTime = System.currentTimeMillis();

        // When
        scheduler.syncYearlyDataAsync();

        // Then: 10초 이내에 완료되어야 함 (성능 기준)
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 10000, "대량 데이터 처리가 10초를 초과했습니다: " + duration + "ms");

        // Verify: 100개국 × 2년 = 200번 호출
        verify(holidayService, times(200)).updateHolidayList(any(HolidayUpdateForm.class));
    }

    private List<CountryDto> createManyCountries(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createCountryDto("C" + String.format("%02d", i), "Country " + i))
                .toList();
    }
}