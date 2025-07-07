package com.planitsquare.assignment_jaehyuk.serivce;

import com.planitsquare.assignment_jaehyuk.dto.HolidayDto;
import com.planitsquare.assignment_jaehyuk.entity.Holiday;
import com.planitsquare.assignment_jaehyuk.repository.HolidayRepository;
import com.planitsquare.assignment_jaehyuk.util.StringArrayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
