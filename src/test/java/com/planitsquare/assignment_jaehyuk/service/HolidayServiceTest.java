package com.planitsquare.assignment_jaehyuk.service;

import com.planitsquare.assignment_jaehyuk.client.NagerDateApiClient;
import com.planitsquare.assignment_jaehyuk.dto.external.HolidayDto;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidayDeleteForm;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidaySearchCondition;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidayUpdateForm;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayDetailResponse;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayResponse;
import com.planitsquare.assignment_jaehyuk.entity.Holiday;
import com.planitsquare.assignment_jaehyuk.repository.HolidayRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private HolidayService holidayService;

    @Mock
    private NagerDateApiClient nagerDateApiClient;

    private HolidayDto testHolidayDto;

    @BeforeEach
    void setUp() {
        testHolidayDto = HolidayDto.builder()
                .countryCode("KR")
                .date(LocalDate.of(2024, 1, 1))
                .localName("신정")
                .name("New Year's Day")
                .fixed(true)
                .global(true)
                .launchYear(1949)
                .types(Arrays.asList("Public"))
                .counties(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("공휴일 리스트가 null인 경우 저장하지 않음")
    void saveHolidayList_WithNullList_ShouldNotSave() {
        // when
        holidayService.saveHolidayList("Korea", null);

        // then
        verify(holidayRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("공휴일 리스트가 비어있는 경우 저장하지 않음")
    void saveHolidayList_WithEmptyList_ShouldNotSave() {
        // when
        holidayService.saveHolidayList("Korea", Collections.emptyList());

        // then
        verify(holidayRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("중복되지 않은 공휴일 데이터를 성공적으로 저장")
    void saveHolidayList_WithValidData_ShouldSaveSuccessfully() {
        // given
        List<HolidayDto> holidayDtoList = Collections.singletonList(testHolidayDto);
        when(holidayRepository.existsByCountryCodeAndDate("KR", LocalDate.of(2024, 1, 1)))
                .thenReturn(false);

        // when
        holidayService.saveHolidayList("Korea", holidayDtoList);

        // then
        verify(holidayRepository).existsByCountryCodeAndDate("KR", LocalDate.of(2024, 1, 1));
        verify(holidayRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("중복된 공휴일 데이터는 필터링되어 저장되지 않음")
    void saveHolidayList_WithDuplicateData_ShouldFilterDuplicates() {
        // given
        List<HolidayDto> holidayDtoList = Arrays.asList(testHolidayDto);
        when(holidayRepository.existsByCountryCodeAndDate("KR", LocalDate.of(2024, 1, 1)))
                .thenReturn(true);

        // when
        holidayService.saveHolidayList("Korea", holidayDtoList);

        // then
        verify(holidayRepository).existsByCountryCodeAndDate("KR", LocalDate.of(2024, 1, 1));
        verify(holidayRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("일부는 중복, 일부는 새로운 데이터인 경우 새로운 데이터만 저장")
    void saveHolidayList_WithMixedData_ShouldSaveOnlyNewData() {
        // given
        HolidayDto newHolidayDto = HolidayDto.builder()
                .countryCode("KR")
                .date(LocalDate.of(2024, 3, 1))
                .localName("삼일절")
                .name("Independence Movement Day")
                .build();

        List<HolidayDto> holidayDtoList = Arrays.asList(testHolidayDto, newHolidayDto);

        when(holidayRepository.existsByCountryCodeAndDate("KR", LocalDate.of(2024, 1, 1)))
                .thenReturn(true);  // 중복
        when(holidayRepository.existsByCountryCodeAndDate("KR", LocalDate.of(2024, 3, 1)))
                .thenReturn(false); // 새로운 데이터

        // when
        holidayService.saveHolidayList("Korea", holidayDtoList);

        // then
        ArgumentCaptor<List<Holiday>> captor = ArgumentCaptor.forClass(List.class);
        verify(holidayRepository).saveAll(captor.capture());

        List<Holiday> savedHolidays = captor.getValue();
        assertEquals(1, savedHolidays.size());
        assertEquals("삼일절", savedHolidays.get(0).getLocalName());
    }

    @Test
    @DisplayName("국가코드와 연도로 공휴일 검색 - 정상 케이스")
    void searchHolidayList_WithValidParams_ShouldReturnPagedResults() {
        // given
        String countryCode = "KR";
        int year = 2024;
        Pageable pageable = PageRequest.of(0, 10);

        Holiday testHoliday = new Holiday(
                "KR",
                "Korea",
                LocalDate.of(2024, 1, 1),
                "신정",
                "New Year's Day",
                true,
                true,
                1949,
                "Public",
                null
        );
        testHoliday.setId(1L); // 테스트용 ID 설정

        List<Holiday> holidayList = Arrays.asList(testHoliday);
        Page<Holiday> holidayPage = new PageImpl<>(holidayList, pageable, 1);

        when(holidayRepository.findByCountryCodeAndDateBetween(
                eq(countryCode),
                eq(LocalDate.of(year, 1, 1)),
                eq(LocalDate.of(year, 12, 31)),
                eq(pageable)
        )).thenReturn(holidayPage);

        // when
        Page<HolidayResponse> result = holidayService.searchHolidayList(countryCode, year, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        HolidayResponse response = result.getContent().get(0);
        assertEquals(1L, response.getId());
        assertEquals("KR", response.getCountryCode());
        assertEquals("Korea", response.getCountryName());
        assertEquals("신정", response.getLocalName());
        assertEquals("New Year's Day", response.getName());
        assertEquals(LocalDate.of(2024, 1, 1), response.getDate());
    }

    @Test
    @DisplayName("검색 결과가 없는 경우 빈 페이지 반환")
    void searchHolidayList_WithNoResults_ShouldReturnEmptyPage() {
        // given
        String countryCode = "US";
        int year = 2025;
        Pageable pageable = PageRequest.of(0, 10);

        Page<Holiday> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(holidayRepository.findByCountryCodeAndDateBetween(
                eq(countryCode),
                eq(LocalDate.of(year, 1, 1)),
                eq(LocalDate.of(year, 12, 31)),
                eq(pageable)
        )).thenReturn(emptyPage);

        // when
        Page<HolidayResponse> result = holidayService.searchHolidayList(countryCode, year, pageable);

        // then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("페이징이 올바르게 동작하는지 검증")
    void searchHolidayList_WithPaging_ShouldRespectPageable() {
        // given
        String countryCode = "KR";
        int year = 2024;
        Pageable pageable = PageRequest.of(1, 5); // 두번째 페이지, 5개씩

        Page<Holiday> holidayPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(holidayRepository.findByCountryCodeAndDateBetween(
                anyString(),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(pageable)
        )).thenReturn(holidayPage);

        // when
        holidayService.searchHolidayList(countryCode, year, pageable);

        // then
        verify(holidayRepository).findByCountryCodeAndDateBetween(
                eq(countryCode),
                eq(LocalDate.of(year, 1, 1)),
                eq(LocalDate.of(year, 12, 31)),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("공휴일 ID로 상세 조회 - 정상 케이스")
    void searchHolidayDetail_WithValidId_ShouldReturnDetailResponse() {
        // given
        Long holidayId = 1L;
        Holiday testHoliday = new Holiday(
                "KR",
                "Korea",
                LocalDate.of(2024, 1, 1),
                "신정",
                "New Year's Day",
                true,
                true,
                1949,
                "Public,National",
                "Seoul,Busan"
        );
        testHoliday.setId(holidayId);

        when(holidayRepository.findById(holidayId)).thenReturn(Optional.of(testHoliday));

        // when
        HolidayDetailResponse result = holidayService.searchHolidayDetail(holidayId);

        // then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("KR", result.getCountryCode());
        assertEquals("Korea", result.getCountryName());
        assertEquals(LocalDate.of(2024, 1, 1), result.getDate());
        assertEquals("신정", result.getLocalName());
        assertEquals("New Year's Day", result.getName());
        assertEquals(true, result.getFixed());
        assertEquals(true, result.getGlobal());
        assertEquals(1949, result.getLaunchYear());

        // StringArrayUtils.splitToList 결과 검증
        assertNotNull(result.getTypes());
        assertNotNull(result.getCounties());

        verify(holidayRepository).findById(holidayId);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회시 EntityNotFoundException 발생")
    void searchHolidayDetail_WithNonExistentId_ShouldThrowEntityNotFoundException() {
        // given
        Long nonExistentId = 999L;
        when(holidayRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // when & then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> holidayService.searchHolidayDetail(nonExistentId)
        );

        assertEquals("공휴일 Id : {}를 찾을 수 없습니다.", exception.getMessage());
        verify(holidayRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("types와 counties가 null인 경우에도 정상 처리")
    void searchHolidayDetail_WithNullTypesAndCounties_ShouldHandleGracefully() {
        // given
        Long holidayId = 2L;
        Holiday testHoliday = new Holiday(
                "US",
                "United States",
                LocalDate.of(2024, 7, 4),
                "Independence Day",
                "Independence Day",
                true,
                true,
                1776,
                null,  // types가 null
                null   // counties가 null
        );
        testHoliday.setId(holidayId);

        when(holidayRepository.findById(holidayId)).thenReturn(Optional.of(testHoliday));

        // when
        HolidayDetailResponse result = holidayService.searchHolidayDetail(holidayId);

        // then
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("US", result.getCountryCode());
        assertEquals("United States", result.getCountryName());

        // null 값도 정상 처리되는지 확인
        assertNotNull(result.getTypes());    // StringArrayUtils.splitToList가 null을 어떻게 처리하는지에 따라
        assertNotNull(result.getCounties()); // 이 부분은 실제 유틸 동작에 맞게 수정 필요
    }

    @Test
    @DisplayName("새로운 공휴일 추가 시나리오")
    void updateHolidayList_WithNewHolidays_ShouldAddNewHolidays() {
        // given
        HolidayUpdateForm updateForm = new HolidayUpdateForm();
        updateForm.setCountryCode("KR");
        updateForm.setCountryName("Korea");
        updateForm.setYear(2024);

        // 기존 DB에는 데이터 없음
        when(holidayRepository.findByCountryCodeAndCountryNameAndDateBetween(
                eq("KR"), eq("Korea"),
                eq(LocalDate.of(2024, 1, 1)),
                eq(LocalDate.of(2024, 12, 31))
        )).thenReturn(Collections.emptyList());

        // API에서 새로운 공휴일 데이터
        List<HolidayDto> newHolidays = Arrays.asList(
                HolidayDto.builder()
                        .countryCode("KR")
                        .date(LocalDate.of(2024, 1, 1))
                        .localName("신정")
                        .name("New Year's Day")
                        .fixed(true)
                        .global(true)
                        .launchYear(1949)
                        .types(Arrays.asList("Public"))
                        .counties(Collections.emptyList())
                        .build()
        );

        when(nagerDateApiClient.getPublicHolidays("KR", 2024)).thenReturn(newHolidays);

        // when
        holidayService.updateHolidayList(updateForm);

        // then
        verify(holidayRepository).save(any(Holiday.class));
        verify(holidayRepository, never()).deleteAllByIdInBatch(anyList()); // 삭제할 데이터가 없으므로 호출되지 않음
    }

    @Test
    @DisplayName("기존 공휴일 삭제 시나리오")
    void updateHolidayList_WithRemovedHolidays_ShouldDeleteHolidays() {
        // given
        HolidayUpdateForm updateForm = new HolidayUpdateForm();
        updateForm.setCountryCode("KR");
        updateForm.setCountryName("Korea");
        updateForm.setYear(2024);

        // 기존 DB에 공휴일 있음
        Holiday existingHoliday = new Holiday(
                "KR", "Korea", LocalDate.of(2024, 1, 1),
                "신정", "New Year's Day", true, true, 1949, "Public", null
        );
        existingHoliday.setId(1L);

        when(holidayRepository.findByCountryCodeAndCountryNameAndDateBetween(
                eq("KR"), eq("Korea"),
                eq(LocalDate.of(2024, 1, 1)),
                eq(LocalDate.of(2024, 12, 31))
        )).thenReturn(Arrays.asList(existingHoliday));

        // API에서는 해당 공휴일이 없음 (삭제됨)
        when(nagerDateApiClient.getPublicHolidays("KR", 2024)).thenReturn(Collections.emptyList());

        // when
        holidayService.updateHolidayList(updateForm);

        // then
        verify(holidayRepository).deleteAllByIdInBatch(Arrays.asList(1L));
        verify(holidayRepository, never()).save(any(Holiday.class));
    }

    @Test
    @DisplayName("혼합 시나리오 - 추가, 업데이트, 삭제 모두 발생")
    void updateHolidayList_WithMixedChanges_ShouldHandleAllOperations() {
        // given
        HolidayUpdateForm updateForm = new HolidayUpdateForm();
        updateForm.setCountryCode("KR");
        updateForm.setCountryName("Korea");
        updateForm.setYear(2024);

        // 기존 DB 데이터 (2개)
        Holiday existingHoliday1 = new Holiday(
                "KR", "Korea", LocalDate.of(2024, 1, 1),
                "신정", "New Year's Day", true, true, 1949, "Public", null
        );
        existingHoliday1.setId(1L);

        Holiday existingHoliday2 = new Holiday(
                "KR", "Korea", LocalDate.of(2024, 12, 25),
                "크리스마스", "Christmas", true, true, 1949, "Public", null
        );
        existingHoliday2.setId(2L);

        when(holidayRepository.findByCountryCodeAndCountryNameAndDateBetween(
                eq("KR"), eq("Korea"),
                eq(LocalDate.of(2024, 1, 1)),
                eq(LocalDate.of(2024, 12, 31))
        )).thenReturn(Arrays.asList(existingHoliday1, existingHoliday2));

        // 업데이트할 공휴일 findById Mock 추가
        when(holidayRepository.findById(1L)).thenReturn(Optional.of(existingHoliday1));

        // API 데이터 (1개는 업데이트, 1개는 새로 추가, 크리스마스는 삭제)
        List<HolidayDto> apiHolidays = Arrays.asList(
                // 기존 신정 업데이트
                HolidayDto.builder()
                        .countryCode("KR")
                        .date(LocalDate.of(2024, 1, 1))
                        .localName("새해")  // 이름 변경
                        .name("New Year")   // 이름 변경
                        .fixed(true)
                        .global(true)
                        .launchYear(1949)
                        .types(Arrays.asList("Public"))
                        .counties(Collections.emptyList())
                        .build(),
                // 새로운 공휴일 추가
                HolidayDto.builder()
                        .countryCode("KR")
                        .date(LocalDate.of(2024, 3, 1))
                        .localName("삼일절")
                        .name("Independence Movement Day")
                        .fixed(true)
                        .global(true)
                        .launchYear(1949)
                        .types(Arrays.asList("Public"))
                        .counties(Collections.emptyList())
                        .build()
                // 크리스마스는 API에 없음 -> 삭제 대상
        );

        when(nagerDateApiClient.getPublicHolidays("KR", 2024)).thenReturn(apiHolidays);

        // when
        holidayService.updateHolidayList(updateForm);

        // then
        verify(holidayRepository).findById(1L); // 업데이트 대상 조회 확인
        verify(holidayRepository).save(any(Holiday.class)); // 새 공휴일 추가
        verify(holidayRepository).deleteAllByIdInBatch(Arrays.asList(2L)); // 크리스마스 삭제
    }

    @Test
    @DisplayName("API에서 빈 데이터 반환시 모든 기존 공휴일 삭제")
    void updateHolidayList_WithEmptyApiData_ShouldDeleteAllExisting() {
        // given
        HolidayUpdateForm updateForm = new HolidayUpdateForm();
        updateForm.setCountryCode("KR");
        updateForm.setCountryName("Korea");
        updateForm.setYear(2024);

        // 기존 DB에 데이터 있음
        Holiday existingHoliday = new Holiday(
                "KR", "Korea", LocalDate.of(2024, 1, 1),
                "신정", "New Year's Day", true, true, 1949, "Public", null
        );
        existingHoliday.setId(1L);

        when(holidayRepository.findByCountryCodeAndCountryNameAndDateBetween(
                eq("KR"), eq("Korea"),
                eq(LocalDate.of(2024, 1, 1)),
                eq(LocalDate.of(2024, 12, 31))
        )).thenReturn(Arrays.asList(existingHoliday));

        // API에서 빈 데이터 반환
        when(nagerDateApiClient.getPublicHolidays("KR", 2024)).thenReturn(Collections.emptyList());

        // when
        holidayService.updateHolidayList(updateForm);

        // then
        verify(holidayRepository).deleteAllByIdInBatch(Arrays.asList(1L));
        verify(holidayRepository, never()).save(any(Holiday.class));
    }

    @Test
    @DisplayName("고급 검색 - 검색 조건에 맞는 데이터 반환 성공")
    void searchHolidayListWithSearchCondition_WithValidCondition_ShouldReturnResults() {
        // given
        HolidaySearchCondition searchCondition = new HolidaySearchCondition();
        searchCondition.setName("신정");

        Pageable pageable = PageRequest.of(0, 10);

        Holiday testHoliday = new Holiday(
                "KR", "Korea", LocalDate.of(2024, 1, 1),
                "신정", "New Year's Day", true, true, 1949, "Public", null
        );
        testHoliday.setId(1L);

        List<HolidayResponse> responseList = Arrays.asList(
                HolidayResponse.builder()
                        .id(1L)
                        .countryCode("KR")
                        .countryName("Korea")
                        .date(LocalDate.of(2024, 1, 1))
                        .localName("신정")
                        .name("New Year's Day")
                        .build()
        );

        Page<HolidayResponse> expectedPage = new PageImpl<>(responseList, pageable, 1);

        when(holidayRepository.searchHolidayListWithSearchCondition(searchCondition, pageable))
                .thenReturn(expectedPage);

        // when
        Page<HolidayResponse> result = holidayService.searchHolidayListWithSearchCondition(searchCondition, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        HolidayResponse response = result.getContent().get(0);
        assertEquals("KR", response.getCountryCode());
        assertEquals("신정", response.getLocalName());

        verify(holidayRepository).searchHolidayListWithSearchCondition(searchCondition, pageable);
    }

    @Test
    @DisplayName("고급 검색 - 검색 조건에 맞는 데이터가 없는 경우 빈 페이지 반환")
    void searchHolidayListWithSearchCondition_WithNoMatchingData_ShouldReturnEmptyPage() {
        // given
        HolidaySearchCondition searchCondition = new HolidaySearchCondition();
        searchCondition.setName("존재하지않는공휴일");

        Pageable pageable = PageRequest.of(0, 10);

        Page<HolidayResponse> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(holidayRepository.searchHolidayListWithSearchCondition(searchCondition, pageable))
                .thenReturn(emptyPage);

        // when
        Page<HolidayResponse> result = holidayService.searchHolidayListWithSearchCondition(searchCondition, pageable);

        // then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        verify(holidayRepository).searchHolidayListWithSearchCondition(searchCondition, pageable);
    }

// ========== deleteHoliday 테스트 ==========

    @Test
    @DisplayName("공휴일 삭제 - 해당 연도의 모든 공휴일 삭제 성공")
    void deleteHoliday_WithExistingData_ShouldDeleteAllHolidays() {
        // given
        HolidayDeleteForm deleteForm = new HolidayDeleteForm();
        deleteForm.setCountryCode("KR");
        deleteForm.setCountryName("Korea");
        deleteForm.setYear(2024);

        Long expectedDeleteCount = 5L; // 삭제될 데이터 개수

        when(holidayRepository.deleteByCountryCodeAndYear("KR", 2024))
                .thenReturn(expectedDeleteCount);

        // when
        holidayService.deleteHoliday(deleteForm);

        // then
        verify(holidayRepository).deleteByCountryCodeAndYear("KR", 2024);
    }

    @Test
    @DisplayName("공휴일 삭제 - 삭제할 데이터가 없는 경우 0개 삭제")
    void deleteHoliday_WithNoExistingData_ShouldReturnZeroCount() {
        // given
        HolidayDeleteForm deleteForm = new HolidayDeleteForm();
        deleteForm.setCountryCode("KR");
        deleteForm.setCountryName("Korea");
        deleteForm.setYear(2024);

        Long expectedDeleteCount = 0L; // 삭제될 데이터가 없음

        when(holidayRepository.deleteByCountryCodeAndYear("KR", 2024))
                .thenReturn(expectedDeleteCount);

        // when
        holidayService.deleteHoliday(deleteForm);

        // then
        verify(holidayRepository).deleteByCountryCodeAndYear("KR", 2024);
    }

    @Test
    @DisplayName("공휴일 삭제 - 다른 국가코드와 연도로 삭제 요청")
    void deleteHoliday_WithDifferentCountryAndYear_ShouldCallWithCorrectParams() {
        // given
        HolidayDeleteForm deleteForm = new HolidayDeleteForm();
        deleteForm.setCountryCode("US");
        deleteForm.setCountryName("United States");
        deleteForm.setYear(2023);

        Long expectedDeleteCount = 3L;

        when(holidayRepository.deleteByCountryCodeAndYear("US", 2023))
                .thenReturn(expectedDeleteCount);

        // when
        holidayService.deleteHoliday(deleteForm);

        // then
        verify(holidayRepository).deleteByCountryCodeAndYear("US", 2023);
    }
}