package com.planitsquare.assignment_jaehyuk.repository;

import com.planitsquare.assignment_jaehyuk.dto.request.HolidaySearchCondition;
import com.planitsquare.assignment_jaehyuk.dto.response.HolidayResponse;
import com.planitsquare.assignment_jaehyuk.dto.response.QHolidayResponse;
import com.planitsquare.assignment_jaehyuk.util.DateUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static com.planitsquare.assignment_jaehyuk.entity.QHoliday.holiday;
import static org.springframework.util.StringUtils.hasText;

@RequiredArgsConstructor
public class HolidayRepositoryImpl implements HolidayRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<HolidayResponse> searchHolidayListWithSearchCondition(HolidaySearchCondition searchCondition, Pageable pageable) {

        // 정렬 조건 구성
        OrderSpecifier<?> orderSpecifier = buildOrderSpecifier(searchCondition);

        List<HolidayResponse> holidayResponseList = queryFactory
                .select(
                        new QHolidayResponse(
                                holiday.id,
                                holiday.countryCode,
                                holiday.countryName,
                                holiday.date,
                                holiday.localName,
                                holiday.name
                        )
                )
                .from(holiday)
                .where(
                        buildSearchCondition(searchCondition)
                )
                .orderBy(orderSpecifier != null ? orderSpecifier : holiday.date.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(holiday.count())
                .from(holiday)
                .where(buildSearchCondition(searchCondition))
                .fetchOne();

        long total = totalCount != null ? totalCount : 0L;

        return new PageImpl<>(holidayResponseList, pageable, total);
    }

    @Override
    public Long deleteByCountryCodeAndYear(String countryCode, int year) {
        DateUtils.DateRange yearRange = DateUtils.getYearRange(year);

        return queryFactory
                .delete(holiday)
                .where(holiday.countryCode.eq(countryCode)
                        .and(holiday.date.between(yearRange.startDate(), yearRange.endDate())))
                .execute();
    }

    private BooleanBuilder buildSearchCondition(HolidaySearchCondition searchCondition) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(hasCountryName(searchCondition.getCountryName()))
                .and(hasDateRange(searchCondition.getStartDate(), searchCondition.getEndDate()))
                .and(hasLocalName(searchCondition.getLocalName()))
                .and(hasName(searchCondition.getName()))
                .and(hasGlobal(searchCondition.getGlobal()))
                .and(hasLaunchYear(searchCondition.getLaunchYear()));

        return booleanBuilder;
    }

    private BooleanExpression hasCountryName(String countryName) {
        return hasText(countryName) ? holiday.countryName.contains(countryName) : null;
    }

    private BooleanExpression hasDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return holiday.date.between(startDate, endDate);
        } else if (startDate != null) {
            return holiday.date.goe(startDate);
        } else if (endDate != null) {
            return holiday.date.loe(endDate);
        }
        return null;
    }

    private BooleanExpression hasLocalName(String localName) {
        return hasText(localName) ? holiday.localName.contains(localName) : null;
    }

    private BooleanExpression hasName(String name) {
        return hasText(name) ? holiday.name.contains(name) : null;
    }

    private BooleanExpression hasGlobal(Boolean global) {
        return global != null ? holiday.global.eq(global) : null;
    }

    private BooleanExpression hasLaunchYear(Integer launchYear) {
        return launchYear != null ? holiday.launchYear.eq(launchYear) : null;
    }


    /**
     * 정렬 조건 구성
     */
    private OrderSpecifier<?> buildOrderSpecifier(HolidaySearchCondition searchForm) {

        if(!hasText(searchForm.getSortBy())) return null;

        Order order = "desc".equalsIgnoreCase(searchForm.getSortDirection())
                ? Order.DESC : Order.ASC;

        return switch (searchForm.getSortBy().toLowerCase()) {
            case "date" -> new OrderSpecifier<>(order, holiday.date);
            case "countryname" -> new OrderSpecifier<>(order, holiday.countryName);
            case "name" -> new OrderSpecifier<>(order, holiday.name);
            case "launchyear" -> new OrderSpecifier<>(order, holiday.launchYear);
            default -> new OrderSpecifier<>(Order.DESC, holiday.date); // 기본값
        };
    }
}
