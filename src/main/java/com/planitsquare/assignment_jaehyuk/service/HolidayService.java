package com.planitsquare.assignment_jaehyuk.service;

import com.planitsquare.assignment_jaehyuk.client.NagerDateApiClient;
import com.planitsquare.assignment_jaehyuk.dto.external.HolidayDto;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidayDeleteForm;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidaySearchCondition;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidayUpdateForm;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayDetailResponse;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayResponse;
import com.planitsquare.assignment_jaehyuk.entity.Holiday;
import com.planitsquare.assignment_jaehyuk.error.ErrorCode;
import com.planitsquare.assignment_jaehyuk.error.exception.BusinessException;
import com.planitsquare.assignment_jaehyuk.repository.HolidayBulkRepository;
import com.planitsquare.assignment_jaehyuk.repository.HolidayRepository;
import com.planitsquare.assignment_jaehyuk.util.DateUtils;
import com.planitsquare.assignment_jaehyuk.util.StringArrayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final NagerDateApiClient nagerDateApiClient;
    private final HolidayBulkRepository holidayBulkRepository;


    @Transactional
    public List<HolidayDto> saveAllHolidaysBulk(List<HolidayDto> holidayDtos, Map<String, String> countryNameMap) {
        if (holidayDtos == null || holidayDtos.isEmpty()) {
            log.warn("저장할 공휴일 데이터가 없습니다");
            return List.of();
        }

        log.info("JDBC 벌크 저장 시작: {} 개 공휴일", holidayDtos.size());

        try {
            LocalDateTime now = LocalDateTime.now();

            // 🚀 DTO를 Entity로 변환 (시간 통일)
            List<Holiday> holidays = holidayDtos.stream()
                    .map(dto -> convertToEntityBulk(dto, countryNameMap, now))
                    .toList();

            // 🚀 JDBC 배치 INSERT 사용!
            int insertedCount = holidayBulkRepository.bulkInsert(holidays);

            log.info("JDBC 벌크 저장 완료: {} 개 공휴일이 저장되었습니다", insertedCount);

            return holidayDtos; // 원본 DTO 반환 (ID는 불필요)

        } catch (Exception e) {
            log.error("JDBC 벌크 저장 중 오류 발생", e);
            throw new BusinessException(ErrorCode.HOLIDAY_BULK_SAVE_FAILED);
        }
    }

    private Holiday convertToEntityBulk(HolidayDto dto, Map<String, String> countryNameMap, LocalDateTime timestamp) {
        if (dto == null) {
            return null;
        }

        String countryName = countryNameMap.get(dto.getCountryCode());
        if (countryName == null) {
            log.warn("국가 코드 {}에 대한 국가명을 찾을 수 없습니다", dto.getCountryCode());
            countryName = dto.getCountryCode() + " (Unknown)";
        }

        return Holiday.builder()
                .countryCode(dto.getCountryCode())
                .countryName(countryName)
                .date(dto.getDate())
                .localName(dto.getLocalName())
                .name(dto.getName())
                .fixed(dto.getFixed())
                .global(dto.getGlobal())
                .launchYear(dto.getLaunchYear())
                .counties(StringArrayUtils.joinFromList(dto.getCounties()))
                .types(StringArrayUtils.joinFromList(dto.getTypes()))
                .build();
    }

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
            try {
                holidayRepository.saveAll(holidayList);
                log.info("공휴일 데이터 저장 완료 - 국가: {}, 저장된 건수: {}", countryName, holidayList.size());
            } catch (Exception e) {
                log.error("공휴일 데이터 저장 실패 - 국가: {}", countryName, e);
                throw new BusinessException(ErrorCode.HOLIDAY_BULK_SAVE_FAILED);
            }
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
        return Holiday.builder()
                .countryCode(dto.getCountryCode())
                .countryName(countryName)
                .date(dto.getDate())
                .localName(dto.getLocalName())
                .name(dto.getName())
                .fixed(dto.getFixed())
                .global(dto.getGlobal())
                .launchYear(dto.getLaunchYear())
                .types(StringArrayUtils.joinFromList(dto.getTypes()))
                .counties(StringArrayUtils.joinFromList(dto.getCounties()))
                .build();
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
                DateUtils.getYearStartDate(year),
                DateUtils.getYearEndDate(year),
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
                () -> new BusinessException(ErrorCode.HOLIDAY_NOT_FOUND)
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
    public void updateHoliday(HolidayDto holidayDto, Long id) {
        holidayRepository.findById(id).ifPresentOrElse(
                existingHoliday -> {
                    existingHoliday.updateHoliday(
                            holidayDto.getDate(),
                            holidayDto.getLocalName(),
                            holidayDto.getName(),
                            holidayDto.getFixed(),
                            holidayDto.getGlobal(),
                            holidayDto.getLaunchYear(),
                            StringArrayUtils.joinFromList(holidayDto.getTypes()),
                            StringArrayUtils.joinFromList(holidayDto.getCounties()),
                            existingHoliday.getCreatedAt()
                    );

                    log.info("공휴일 업데이트 완료 - ID: {}", id);
                },
                () -> {
                    throw new BusinessException(ErrorCode.HOLIDAY_NOT_FOUND);
                }
        );
    }

    @Transactional
    public void saveHoliday(HolidayDto holidayDto, String countryName) {
        try {
            holidayRepository.save(
                    Holiday.builder()
                            .countryCode(holidayDto.getCountryCode())
                            .countryName(countryName)
                            .date(holidayDto.getDate())
                            .localName(holidayDto.getLocalName())
                            .name(holidayDto.getName())
                            .fixed(holidayDto.getFixed())
                            .global(holidayDto.getGlobal())
                            .launchYear(holidayDto.getLaunchYear())
                            .types(StringArrayUtils.joinFromList(holidayDto.getTypes()))
                            .counties(StringArrayUtils.joinFromList(holidayDto.getCounties()))
                            .build()
            );
        } catch (Exception e) {
            log.error("공휴일 저장 실패 - 국가: {}, 날짜: {}", countryName, holidayDto.getDate(), e);
            throw new BusinessException(ErrorCode.HOLIDAY_BULK_SAVE_FAILED);
        }
    }

    @Transactional
    public void updateHolidayList(HolidayUpdateForm updateForm){
        try {
            DateUtils.DateRange yearRange = DateUtils.getYearRange(updateForm.getYear());

            List<Holiday> existingHolidaysList = holidayRepository.findByCountryCodeAndCountryNameAndDateBetween(
                    updateForm.getCountryCode(), updateForm.getCountryName(), yearRange.startDate(), yearRange.endDate());

            List<HolidayDto> latestHolidayList = nagerDateApiClient.getPublicHolidays(updateForm.getCountryCode(), updateForm.getYear());

            // 3. 기존 데이터를 날짜 기준으로 Map으로 변환 (빠른 조회를 위해)
            Map<LocalDate, Holiday> existingHolidayMap = existingHolidaysList.stream()
                    .collect(Collectors.toMap(Holiday::getDate, Function.identity()));

            // 4. 최신 데이터를 날짜 기준으로 Map으로 변환
            Map<LocalDate, HolidayDto> latestHolidayMap = latestHolidayList.stream()
                    .collect(Collectors.toMap(HolidayDto::getDate, Function.identity()));

            int updatedCount = 0;
            int addedCount = 0;

            // 🔥 처리된 날짜들을 추적!
            Set<LocalDate> processedDates = new HashSet<>();

            // 5. 최신 데이터로 업데이트 또는 추가
            for (HolidayDto latestDto : latestHolidayList) {
                Holiday existingHoliday = existingHolidayMap.get(latestDto.getDate());

                if (existingHoliday != null) {
                    updateHoliday(latestDto, existingHoliday.getId());
                    updatedCount++;
                    log.debug("공휴일 업데이트 - 날짜: {}, 이름: {}", latestDto.getDate(), latestDto.getName());
                    existingHolidayMap.remove(latestDto.getDate());  // 🔥 처리된 것은 맵에서 제거!
                } else {
                    saveHoliday(latestDto, updateForm.getCountryName());
                    addedCount++;
                    log.debug("새 공휴일 추가 - 날짜: {}, 이름: {}", latestDto.getDate(), latestDto.getName());
                }
            }

            holidayRepository.deleteAllByIdInBatch(existingHolidayMap.values().stream()
                    .map(Holiday::getId)
                    .toList()
            );

            log.info("공휴일 업데이트 완료 - 국가: {}, 업데이트: {}, 추가: {}, 삭제: {}",
                    updateForm.getCountryName(), updatedCount, addedCount, existingHolidayMap.size());

        } catch (BusinessException e) {
            throw e; // BusinessException은 그대로 전파
        } catch (Exception e) {
            log.error("공휴일 업데이트 실패 - 국가: {}, 연도: {}", updateForm.getCountryName(), updateForm.getYear(), e);
            throw new BusinessException(ErrorCode.HOLIDAY_UPDATE_FAILED);
        }
    }

    @Transactional
    public void deleteHoliday(HolidayDeleteForm deleteForm) {
        try {
            Long deleteCount = holidayRepository.deleteByCountryCodeAndYear(deleteForm.getCountryCode(), deleteForm.getYear());

            if (deleteCount == 0) {
                log.warn("삭제할 공휴일 데이터가 없습니다 - 국가: {}, 연도: {}", deleteForm.getCountryCode(), deleteForm.getYear());
                throw new BusinessException(ErrorCode.HOLIDAY_NOT_FOUND);
            }

            log.info("삭제된 데이터 개수: {}", deleteCount);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("공휴일 삭제 실패 - 국가: {}, 연도: {}", deleteForm.getCountryCode(), deleteForm.getYear(), e);
            throw new BusinessException(ErrorCode.HOLIDAY_UPDATE_FAILED);
        }
    }
}
