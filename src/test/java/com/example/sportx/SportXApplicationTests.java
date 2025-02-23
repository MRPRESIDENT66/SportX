package com.example.sportx;

import com.example.sportx.Entity.User;
import com.example.sportx.Mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
class SportXApplicationTests {

	@Autowired
	private UserMapper userMapper;

	@Test
	void contextLoads() {
		User user = new User();
		user.setId(1);
		user.setName("John Doe");
		user.setEmail("johndoe@example.com");
		user.setAge(25);
		user.setCreatedAt(LocalDateTime.now()); // 当前时间
		userMapper.insert(user);
	}

	@Test
	public void test() {
	}

}
