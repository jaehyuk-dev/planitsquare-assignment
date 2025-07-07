package com.planitsquare.assignment_jaehyuk.serivce;

import com.planitsquare.assignment_jaehyuk.dto.external.HolidayDto;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayDetailResponse;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayResponse;
import com.planitsquare.assignment_jaehyuk.entity.Holiday;
import com.planitsquare.assignment_jaehyuk.repository.HolidayRepository;
import com.planitsquare.assignment_jaehyuk.util.StringArrayUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;

    @Transactional
    public void saveHolidayList(String countryName, List<HolidayDto> holidayDtoList) {
        if (holidayDtoList == null || holidayDtoList.isEmpty()) {
            log.debug("저장할 공휴일 데이터가 없습니다 - 국가: {}", countryName);
            return;
        }

        log.debug("공휴일 데이터 저장 시작 - 국가: {}, 건수: {}", countryName, holidayDtoList.size());

        List<Holiday> holidayList = holidayDtoList.stream()
                .filter(dto -> isNotDuplicate(dto.getCountryCode(), dto.getDate()))
                .map(dto -> convertToHolidayEntity(dto, countryName))
                .toList();

        if (!holidayList.isEmpty()) {
            holidayRepository.saveAll(holidayList);
            log.info("공휴일 데이터 저장 완료 - 국가: {}, 저장된 건수: {}", countryName, holidayList.size());
        } else {
            log.debug("저장할 유효한 공휴일 데이터가 없습니다 - 국가: {}", countryName);
        }
    }

    private boolean isNotDuplicate(String countryCode, LocalDate date) {
        boolean exists = holidayRepository.existsByCountryCodeAndDate(countryCode, date);
        if (exists) {
            log.debug("중복 공휴일 데이터 - 국가: {}, 날짜: {}", countryCode, date);
        }
        return !exists;
    }

    private Holiday convertToHolidayEntity(HolidayDto dto, String countryName) {
        return new Holiday(
                dto.getCountryCode(),
                countryName,
                dto.getDate(),
                dto.getLocalName(),
                dto.getName(),
                dto.getFixed(),
                dto.getGlobal(),
                dto.getLaunchYear(),
                StringArrayUtils.joinFromList(dto.getTypes()),
                StringArrayUtils.joinFromList(dto.getCounties())
        );
    }


    /**
     * 기본 검색
     * @param countryCode
     * @param year
     * @param pageable
     * @return
     */
    public Page<HolidayResponse> searchHolidayList(String countryCode, int year, Pageable pageable) {
        Page<Holiday> holidayPage = holidayRepository.findByCountryCodeAndDateBetween(
                countryCode,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31),
                pageable
        );

        return holidayPage.map(holiday ->
                HolidayResponse.builder()
                        .id(holiday.getId())
                        .countryCode(holiday.getCountryCode())
                        .countryName(holiday.getCountryName())
                        .date(holiday.getDate())
                        .localName(holiday.getLocalName())
                        .name(holiday.getName())
                        .build()
        );
    }

    /**
     * 고급 검색
     * @return
     */
    public Page<HolidayResponse> searchHolidayListWithSearchCondition() {
        return Page.empty();
    }

    /**
     * 공휴일 상세검색
     * @param id
     * @return
     */
    public HolidayDetailResponse searchHolidayDetail(Long id){
        Holiday holiday = holidayRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("공휴일 Id : {}를 찾을 수 없습니다.")
        );

        return HolidayDetailResponse.builder()
                .id(holiday.getId())
                .countryCode(holiday.getCountryCode())
                .countryName(holiday.getCountryName())
                .date(holiday.getDate())
                .localName(holiday.getLocalName())
                .name(holiday.getName())
                .fixed(holiday.getFixed())
                .global(holiday.getGlobal())
                .launchYear(holiday.getLaunchYear())
                .types(StringArrayUtils.splitToList(holiday.getTypes()))
                .counties(StringArrayUtils.splitToList(holiday.getCounties()))
                .createdAt(holiday.getCreatedAt())
                .updatedAt(holiday.getUpdatedAt())
                .build();
    }
}
