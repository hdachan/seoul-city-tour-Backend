package com.example.seoulcitytour;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@SpringBootApplication
@EnableScheduling
public class SeoulCityTourApplication {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void checkDb() throws Exception {
        System.out.println("=================================");
        System.out.println("DB URL = " + dataSource.getConnection().getMetaData().getURL());
        System.out.println("=================================");
    }

    public static void main(String[] args) {
        SpringApplication.run(SeoulCityTourApplication.class, args);
    }
}