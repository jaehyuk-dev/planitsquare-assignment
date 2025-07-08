package com.planitsquare.assignment_jaehyuk.repository;

import com.planitsquare.assignment_jaehyuk.dto.request.HolidaySearchCondition;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HolidayRepositoryCustom {
    Page<HolidayResponse> searchHolidayListWithSearchCondition(HolidaySearchCondition searchCondition, Pageable pageable);

    Long deleteByCountryCodeAndYear(String countryCode, int year);
}
