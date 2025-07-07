package com.planitsquare.assignment_jaehyuk.serivce;

import com.planitsquare.assignment_jaehyuk.client.NagerDateApiClient;
import com.planitsquare.assignment_jaehyuk.dto.external.HolidayDto;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidaySearchCondition;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidayUpdateForm;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final NagerDateApiClient nagerDateApiClient;

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
    public Page<HolidayResponse> searchHolidayListWithSearchCondition(HolidaySearchCondition searchCondition, Pageable pageable) {
        return holidayRepository.searchHolidayListWithSearchCondition(searchCondition, pageable);
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


    @Transactional
    public void updateHolidayList(HolidayUpdateForm updateForm) {
        LocalDate startDate = LocalDate.of(updateForm.getYear(), 1, 1);
        LocalDate endDate = LocalDate.of(updateForm.getYear(), 12, 31);
        List<Holiday> existingHolidaysList = holidayRepository.findByCountryCodeAndCountryNameAndDateBetween(updateForm.getCountryCode(), updateForm.getCountryName(), startDate, endDate);

        List<HolidayDto> latestHolidayList = nagerDateApiClient.getPublicHolidays(updateForm.getCountryCode(), updateForm.getYear());

        Map<LocalDate, Holiday> existingHolidayMap = existingHolidaysList.stream().collect(Collectors.toMap(Holiday::getDate, Function.identity()));

        Map<LocalDate, HolidayDto> latestHolidayMap = latestHolidayList.stream().collect(Collectors.toMap(HolidayDto::getDate, Function.identity()));

        int updatedCount = 0;
        int addedCount = 0;
        int deletedCount = 0;

        for (HolidayDto latestDto : latestHolidayList) {
            Holiday existingHoliday = existingHolidayMap.get(latestDto.getDate());

            if (existingHoliday != null) {
                existingHoliday.updateHoliday(
                        latestDto.getDate(),
                        latestDto.getLocalName(),
                        latestDto.getName(),
                        latestDto.getFixed(),
                        latestDto.getGlobal(),
                        latestDto.getLaunchYear(),
                        StringArrayUtils.joinFromList(latestDto.getTypes()),
                        StringArrayUtils.joinFromList(latestDto.getCounties()),
                        existingHoliday.getCreatedAt() // 생성일은 유지
                );
                updatedCount++;
                log.debug("공휴일 업데이트 - 날짜: {}, 이름: {}", latestDto.getDate(), latestDto.getName());

            } else {
                holidayRepository.save(convertToHolidayEntity(latestDto, updateForm.getCountryName()));
                addedCount++;
                log.debug("새 공휴일 추가 - 날짜: {}, 이름: {}", latestDto.getDate(), latestDto.getName());
            }
        }
        List<Long> idsToDelete = new ArrayList<>();

        for (Holiday existingHoliday : existingHolidaysList) {
            if (!latestHolidayMap.containsKey(existingHoliday.getDate())) {
                idsToDelete.add(existingHoliday.getId());
                log.debug("삭제 대상 공휴일 - 날짜: {}, 이름: {}, ID: {}", existingHoliday.getDate(), existingHoliday.getName(), existingHoliday.getId());
            }
        }

        if (!idsToDelete.isEmpty()) {
            holidayRepository.deleteAllByIdInBatch(idsToDelete);
            deletedCount = idsToDelete.size();
            log.info("🗑️ 배치 삭제 완료 - 삭제된 공휴일 수: {}", deletedCount);
        }

        log.info("공휴일 데이터 새로고침 완료 - 국가: {}, 연도: {}, 업데이트: {}, 추가: {}, 삭제: {}", updateForm.getCountryCode(), updateForm.getYear(), updatedCount, addedCount, deletedCount);
    }
}
