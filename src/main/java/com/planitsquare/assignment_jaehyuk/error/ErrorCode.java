package com.planitsquare.assignment_jaehyuk.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_HOLIDAY_DATA(HttpStatus.BAD_REQUEST, "40001", "잘못된 공휴일 데이터입니다."),
    INVALID_COUNTRY_CODE(HttpStatus.BAD_REQUEST, "40002", "지원하지 않는 국가 코드입니다."),
    INVALID_YEAR_RANGE(HttpStatus.BAD_REQUEST, "40003", "잘못된 연도 범위입니다."),

    HOLIDAY_NOT_FOUND(HttpStatus.NOT_FOUND, "40401", "공휴일을 찾을 수 없습니다."),

    EXTERNAL_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "40801", "외부 API 요청 시간이 초과되었습니다."),

    HOLIDAY_BULK_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "50001", "공휴일 벌크 저장에 실패했습니다."),
    HOLIDAY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "50002", "공휴일 업데이트에 실패했습니다."),
    BULK_INSERT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "50003", "대량 데이터 저장에 실패했습니다."),

    COUNTRY_API_CALL_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "50301", "국가 목록 조회 API 호출에 실패했습니다."),
    HOLIDAY_API_CALL_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "50302", "공휴일 조회 API 호출에 실패했습니다."),

    HOLIDAY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "50004", "공휴일 삭제에 실패했습니다."),

    ;
    private HttpStatus httpStatus;
    private String errorCode;
    private String message;

    ErrorCode(HttpStatus httpStatus, String errorCode, String message) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.message = message;
    }
}
