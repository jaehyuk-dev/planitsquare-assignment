package com.planitsquare.assignment_jaehyuk.serivce;

import com.planitsquare.assignment_jaehyuk.dto.external.HolidayDto;
import com.planitsquare.assignment_jaehyuk.entity.Holiday;
import com.planitsquare.assignment_jaehyuk.repository.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private HolidayService holidayService;

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
        List<HolidayDto> holidayDtoList = Arrays.asList(testHolidayDto);
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
}