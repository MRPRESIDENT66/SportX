package com.example.sportx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.sportx.Mapper")
@EnableScheduling
public class SportXApplication {

	public static void main(String[] args) {
		SpringApplication.run(SportXApplication.class, args);
	}

}
