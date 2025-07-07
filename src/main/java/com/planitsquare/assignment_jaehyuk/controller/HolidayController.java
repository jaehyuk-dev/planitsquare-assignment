package com.planitsquare.assignment_jaehyuk.controller;

import com.planitsquare.assignment_jaehyuk.dto.request.HolidayDeleteForm;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidaySearchCondition;
import com.planitsquare.assignment_jaehyuk.dto.request.HolidayUpdateForm;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayDetailResponse;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayResponse;
import com.planitsquare.assignment_jaehyuk.serivce.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "공휴일 관리", description = "공휴일 조회, 검색, 업데이트, 삭제 API")
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(
            summary = "공휴일 기본 검색",
            description = "국가 코드와 연도를 기준으로 공휴일 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{countryCode}/{year}")
    public ResponseEntity<Page<HolidayResponse>> searchHolidayList(
            @Parameter(description = "국가 코드 (ISO 2자리)", example = "KR", required = true)
            @PathVariable String countryCode,
            @Parameter(description = "조회할 연도", example = "2024", required = true)
            @PathVariable int year,
            @Parameter(description = "페이징 정보 (page, size, sort)")
            @PageableDefault(size = 10, sort = "date") Pageable pageable) {

        log.info("공휴일 기본 검색 요청 - 국가: {}, 연도: {}, 페이지: {}", countryCode, year, pageable.getPageNumber());

        return ResponseEntity.ok(holidayService.searchHolidayList(countryCode, year, pageable));
    }

    @Operation(
            summary = "공휴일 상세 조회",
            description = "공휴일 ID를 기준으로 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = HolidayDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "공휴일을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<HolidayDetailResponse> searchHolidayDetail(
            @Parameter(description = "공휴일 고유 ID", example = "1", required = true)
            @PathVariable Long id) {

        log.info("공휴일 상세 검색 요청 - id: {}", id);

        return ResponseEntity.ok(holidayService.searchHolidayDetail(id));
    }

    @Operation(
            summary = "공휴일 데이터 새로고침",
            description = "외부 API에서 최신 공휴일 데이터를 가져와 데이터베이스를 업데이트합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "새로고침 성공",
                    content = @Content(schema = @Schema(implementation = String.class, example = "success"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/refresh")
    public ResponseEntity<String> refreshHolidayData(
            @Parameter(description = "새로고침 요청 정보", required = true)
            @RequestBody @Valid HolidayUpdateForm updateForm) {

        log.info("공휴일 데이터 새로고침 요청 - 국가: {}, 연도: {}", updateForm.getCountryCode(), updateForm.getYear());
        holidayService.updateHolidayList(updateForm);

        return ResponseEntity.ok("success");
    }

    @Operation(
            summary = "공휴일 고급 검색",
            description = "다양한 조건을 사용하여 공휴일을 검색합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 검색 조건",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/")
    public ResponseEntity<Page<HolidayResponse>> searchHolidayList(
            @Parameter(description = "고급 검색 조건")
            @Valid HolidaySearchCondition searchCondition,
            @Parameter(description = "페이징 정보 (page, size, sort)")
            @PageableDefault(size = 10, sort = "date") Pageable pageable) {

        log.info("공휴일 고급 검색 요청");

        return ResponseEntity.ok(holidayService.searchHolidayListWithSearchCondition(searchCondition, pageable));
    }

    @Operation(
            summary = "공휴일 데이터 삭제",
            description = "특정 국가의 특정 연도 공휴일 데이터를 모두 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = String.class, example = "success"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @DeleteMapping("/")
    public ResponseEntity<String> deleteHolidayData(
            @Parameter(description = "삭제 요청 정보", required = true)
            @RequestBody @Valid HolidayDeleteForm deleteForm) {

        log.info("공휴일 데이터 삭제 요청 - 국가: {}, 연도: {}", deleteForm.getCountryCode(), deleteForm.getYear());
        holidayService.deleteHoliday(deleteForm);

        return ResponseEntity.ok("success");
    }
}