package com.example.ticket;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LangChainTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangChainTicketApplication.class, args);
    }

}
