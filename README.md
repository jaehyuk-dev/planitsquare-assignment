# Holiday Keeper - 전세계 공휴일 관리 시스템

> **Nager.Date API**를 활용한 전세계 공휴일 데이터 저장·조회·관리 Mini Service

## 📋 프로젝트 개요

- **목적**: 외부 API를 활용하여 최근 5년(2020~2025) 전세계 공휴일 데이터를 저장, 조회, 관리
- **기술 스택**: Java 21, Spring Boot 3.4.7, Spring WebFlux, JPA(Hibernate), H2 Database, QueryDSL
- **외부 API**: [Nager.Date API](https://date.nager.at/api/v3)

## 🚀 빌드 & 실행 방법

### 1. 프로젝트 빌드
```bash
./gradlew clean build
```

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 3. 테스트 실행
```bash
./gradlew clean test
```

**테스트 실행 결과:**
<img width="1278" height="533" alt="Image" src="https://github.com/user-attachments/assets/9892a194-7598-41f6-95c7-fd587a5a5a34" />
- 테스트 상세 결과: `build/reports/tests/test/index.html`

## 🌐 REST API 명세 요약

### 1. 공휴일 기본 검색
```http
GET /api/holiday/{countryCode}/{year}?page=0&size=10
```
- **설명**: 특정 국가의 특정 연도 공휴일 조회 (페이징 지원)
- **파라미터**: 
  - `countryCode`: 국가 코드 (ISO 2자리, 예: KR, US, JP)
  - `year`: 조회할 연도 (예: 2024)
  - `page`, `size`: 페이징 정보 (선택사항)
- **응답**: 페이징된 공휴일 목록

### 2. 공휴일 상세 조회
```http
GET /api/holiday/{id}
```
- **설명**: 공휴일 ID로 상세 정보 조회
- **응답**: 공휴일 상세 정보 (타입, 지역, 생성/수정일 등)

### 3. 공휴일 고급 검색
```http
GET /api/holiday/?countryCode=KR&year=2024&name=크리스마스
```
- **설명**: 다양한 조건으로 공휴일 검색 (국가, 연도, 이름, 기간 등)
- **파라미터**: 모든 검색 조건 선택사항
- **응답**: 조건에 맞는 페이징된 공휴일 목록

### 4. 공휴일 데이터 새로고침
```http
PUT /api/holiday/refresh
Content-Type: application/json

{
  "countryCode": "KR",
  "countryName": "대한민국",
  "year": 2024
}
```
- **설명**: 특정 국가의 특정 연도 데이터를 외부 API에서 재동기화
- **응답**: "success"

### 5. 공휴일 데이터 삭제
```http
DELETE /api/holiday/
Content-Type: application/json

{
  "countryCode": "KR",
  "year": 2024
}
```
- **설명**: 특정 국가의 특정 연도 공휴일 데이터 전체 삭제
- **응답**: "success"

## 📊 응답 예시

### 공휴일 기본 검색 응답
```json
{
  "content": [
    {
      "id": 1,
      "countryCode": "KR",
      "countryName": "대한민국",
      "date": "2024-01-01",
      "localName": "신정",
      "name": "New Year's Day"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 15,
  "totalPages": 2
}
```

### 공휴일 상세 조회 응답
```json
{
  "id": 1,
  "countryCode": "KR",
  "countryName": "대한민국",
  "date": "2024-01-01",
  "localName": "신정",
  "name": "New Year's Day",
  "fixed": true,
  "global": true,
  "launchYear": null,
  "types": ["Public"],
  "counties": [],
  "createdAt": "2024-12-01T10:00:00",
  "updatedAt": "2024-12-01T10:00:00"
}
```

## 📖 Swagger UI 및 OpenAPI 문서

### Swagger UI 접속
```
http://localhost:8080/swagger-ui.html
```

### OpenAPI JSON 스펙
```
http://localhost:8080/v3/api-docs
```

**Swagger UI에서 확인 가능한 기능:**
- 모든 API 엔드포인트 상세 명세
- 실시간 API 테스트 도구
- 요청/응답 스키마 확인
- 파라미터 설명 및 예시값

## 🗄️ 데이터베이스 정보

### H2 Console 접속
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:demo
Username: admin
Password: admin
```

### 테이블 구조
```sql
CREATE TABLE holiday (
    id BIGINT PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    country_name VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    local_name VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    is_fixed BOOLEAN,
    is_global BOOLEAN,
    launch_year INTEGER,
    types TEXT,
    counties TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

## 🔄 자동 배치 작업

### 스케줄링 정보
- **실행 시간**: 매년 1월 2일 01:00 KST
- **작업 내용**: 전년도 및 현재 연도 공휴일 데이터 자동 동기화
- **처리 방식**: 비동기 처리로 성능 최적화 (최대 30개국 동시 처리)

### 초기 데이터 로딩
- **애플리케이션 시작 시**: 2020~2025년 전세계 공휴일 데이터 자동 적재
- **처리 성능**: 약 9,000개 이상의 공휴일 데이터를 5초 내외로 처리 (기존 145초 → 5초로 96.6% 성능 향상)
- **저장 방식**: JDBC 배치 처리를 통한 고성능 대량 저장
- **비동기 처리**: Spring WebFlux를 활용한 reactive programming으로 동시성 극대화

## 🎯 주요 기능 특징

### 1. 고성능 비동기 처리
- **Spring WebFlux**: Reactive Programming으로 초기 데이터 로딩 시간 96.6% 단축 (145초 → 5초)
- **비동기 스케줄러**: 다중 국가 동시 처리로 성능 최적화 (최대 30개국 동시 처리)
- **외부 API 호출**: 재시도 로직과 타임아웃 처리로 안정성 확보
- **JDBC 배치**: 대량 데이터 저장 시 성능 향상

### 2. 트랜잭션 최적화
- **외부 API 호출 분리**: 트랜잭션 범위 최적화로 성능 향상
- **논리적 단위 분할**: 업데이트, 추가, 삭제 작업의 원자성 보장

### 3. 포괄적인 에러 처리
- **글로벌 예외 처리**: 일관된 에러 응답 형식
- **비즈니스 예외**: 명확한 에러 코드와 메시지
- **외부 API 장애 처리**: 재시도 및 폴백 메커니즘

### 4. 확장 가능한 검색 기능
- **기본 검색**: 국가/연도별 빠른 검색
- **고급 검색**: QueryDSL 기반 동적 쿼리 지원
- **페이징 처리**: 대량 데이터 효율적 처리

## 💡 기술적 특징

### 아키텍처 설계
- **레이어드 아키텍처**: Controller → Service → Repository 분리
- **비동기 처리**: Spring WebFlux + Reactor 패턴
- **트랜잭션 관리**: 선언적 트랜잭션과 최적화된 범위 설정

### 성능 최적화
- **Reactive Programming**: Spring WebFlux로 비동기 처리, 초기 데이터 로딩 시간 96.6% 단축
- **데이터베이스**: 시퀀스 기반 ID 생성, 배치 처리
- **외부 API**: 연결 풀링, 재시도 로직, 동시성 제어 (최대 30개국 동시 처리)
- **메모리**: 스트림 기반 처리, 효율적 데이터 변환

## 📝 개발자 노트

### 설정 관리
- **환경별 설정**: application.yml을 통한 설정 관리
- **외부 API 설정**: 타임아웃, 재시도, 동시성 설정 가능
- **배치 설정**: 초기화 연도 범위, 처리 방식 선택 가능
