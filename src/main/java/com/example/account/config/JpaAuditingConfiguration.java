package com.example.account.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration //스프링 부트 뜰 때 오토 스케일링 됨
@EnableJpaAuditing //jpa auditing 기능 활성화, DB 업데이트시 자동 값 변경됨
public class JpaAuditingConfiguration {
}
