package com.planitsquare.assignment_jaehyuk.repository;

import com.planitsquare.assignment_jaehyuk.entity.Holiday;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class HolidayBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int bulkInsert(List<Holiday> holidays) {
        if (holidays == null || holidays.isEmpty()) {
            return 0;
        }

        log.info("JDBC 배치 INSERT 시작: {} 개", holidays.size());

        String sql = """
            INSERT INTO holiday 
            (id, country_code, country_name, date, local_name, name, is_fixed, is_global, 
             launch_year, types, counties, created_at, updated_at) 
            VALUES (NEXT VALUE FOR holiday_seq, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        LocalDateTime now = LocalDateTime.now();

        try {
            // 🚀 Object[][]로 배치 파라미터 준비
            List<Object[]> batchArgs = holidays.stream()
                    .map(holiday -> new Object[]{
                            holiday.getCountryCode(),
                            holiday.getCountryName(),
                            holiday.getDate(),
                            holiday.getLocalName(),
                            holiday.getName(),
                            holiday.getFixed() != null ? holiday.getFixed() : false,
                            holiday.getGlobal() != null ? holiday.getGlobal() : false,
                            holiday.getLaunchYear(),
                            holiday.getTypes(),
                            holiday.getCounties(),
                            now,
                            now
                    })
                    .toList();

            // 🚀 진짜 배치 처리!
            int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);

            int totalInserted = results.length;
            log.info("JDBC 배치 INSERT 완료: {} 개", totalInserted);
            return totalInserted;

        } catch (Exception e) {
            log.error("JDBC 배치 INSERT 실패", e);
            throw new RuntimeException("배치 INSERT 실패: " + e.getMessage(), e);
        }
    }
}