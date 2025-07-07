package com.planitsquare.assignment_jaehyuk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("플랜잇스퀘어 백엔드 개발자 채용 과제")
                                .description("외부 API를 사용하여 최근 5 년(2020 ~ 2025)의 전 세계 공휴일 데이터를 **저장·조회·관리**하는 Mini Service")
                                .contact(
                                        new Contact()
                                                .name("이재혁")
                                                .email("jaehyuk.dev@gmail.com")
                                                .url("https://github.com/jaehyuk-dev")
                                )

                );
    }
}
