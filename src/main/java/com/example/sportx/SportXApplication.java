package com.example.sportx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.sportx.Mapper")
public class SportXApplication {

	public static void main(String[] args) {
		SpringApplication.run(SportXApplication.class, args);
	}

}
