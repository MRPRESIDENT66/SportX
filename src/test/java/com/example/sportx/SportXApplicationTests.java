package com.example.sportx;

import com.example.sportx.Entity.User;
import com.example.sportx.Mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
@Slf4j
class SportXApplicationTests {


	@Test
	void contextLoads() {
		Integer userId = 1;
		log.error("++++++++++++++++ {}",userId);

	}

	@Test
	public void test() {
	}

}
