package com.planitsquare.assignment_jaehyuk.controller;

import com.planitsquare.assignment_jaehyuk.dto.request.HolidaySearchCondition;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidayUpdateForm;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayDetailResponse;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayResponse;
import com.planitsquare.assignment_jaehyuk.serivce.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/holiday")
public class HolidayController {

    private final HolidayService holidayService;

    /**
     * 공휴일 기본 검색 API
     * @param countryCode 국가 코드 (예: KR, US, JP)
     * @param year 연도 (예: 2024)
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 페이징된 공휴일 목록
     */
    @GetMapping("/{countryCode}/{year}")
    public ResponseEntity<Page<HolidayResponse>> searchHolidayList(
            @PathVariable String countryCode,
            @PathVariable int year,
            @PageableDefault(size = 10, sort = "date") Pageable pageable) {

        log.info("공휴일 기본 검색 요청 - 국가: {}, 연도: {}, 페이지: {}", countryCode, year, pageable.getPageNumber());

        return ResponseEntity.ok(holidayService.searchHolidayList(countryCode, year, pageable));
    }

    /**
     * 공휴일 상세 검색
     * @param id 공휴일 id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<HolidayDetailResponse> searchHolidayDetail(@PathVariable Long id) {

        log.info("공휴일 상세 검색 요청 - id: {}", id);

        return ResponseEntity.ok(holidayService.searchHolidayDetail(id));
    }

    /**
     * 공휴일 데이터 새로고침 API
     * @param updateForm 새로고침 요청 데이터
     * @return 새로고침 결과
     */
    @PutMapping("/refresh")
    public ResponseEntity<String> refreshHolidayData(
            @RequestBody @Valid HolidayUpdateForm updateForm) {

        log.info("공휴일 데이터 새로고침 요청 - 국가: {}, 연도: {}", updateForm.getCountryCode(), updateForm.getYear());
        holidayService.updateHolidayList(updateForm);

        return ResponseEntity.ok("success");
    }

    /**
     * 공휴일 고급 검색 API
     * @param searchCondition 검색조건
     * @param pageable 페이징 정보 (page, size, sort)
     * @return
     */
    @GetMapping("/")
    public ResponseEntity<Page<HolidayResponse>> searchHolidayList(
            @Valid HolidaySearchCondition searchCondition,
            @PageableDefault(size = 10, sort = "date") Pageable pageable) {

        log.info("공휴일 고급 검색 요청");

        return ResponseEntity.ok(holidayService.searchHolidayListWithSearchCondition(searchCondition, pageable));
    }
}
