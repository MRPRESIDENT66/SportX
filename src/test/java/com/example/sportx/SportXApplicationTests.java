package com.example.sportx;

import com.example.sportx.Utils.RedisIDWorker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@Slf4j
class SportXApplicationTests {


	@Resource
	private RedisIDWorker redisWorker;

	private ExecutorService es = Executors.newFixedThreadPool(500);

	@Test
	void contextLoads() {
		Integer userId = 1;
		log.error("++++++++++++++++ {}",userId);

	}

	@Test
	void testIdWorker(){
		Runnable task= ()->{
			for(int i=0; i<100; i++){
				long id = redisWorker.nextID("order");
				System.out.println("id: " + id);
			}
		};
		for(int i=0; i<300; i++){
			es.submit(task);
		}
	}
}
