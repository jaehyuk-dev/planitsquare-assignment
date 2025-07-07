package com.planitsquare.assignment_jaehyuk.repository;

import com.planitsquare.assignment_jaehyuk.entity.Holiday;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    boolean existsByCountryCodeAndDate(String countryCode, LocalDate date);

    Page<Holiday> findByCountryCodeAndDateBetween(String countryCode, LocalDate dateAfter, LocalDate dateBefore, Pageable pageable);

    List<Holiday> findByCountryCodeAndCountryNameAndDateBetween(String countryCode, String countryName, LocalDate startDate, LocalDate endDate);

}
