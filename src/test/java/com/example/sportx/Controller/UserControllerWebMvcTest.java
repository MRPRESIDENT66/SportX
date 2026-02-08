package com.example.sportx.Controller;

import com.example.sportx.Config.MvcConfig;
import com.example.sportx.Entity.vo.Result;
import com.example.sportx.Service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UserControllerWebMvcTest.TestApplication.class)
@AutoConfigureMockMvc
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // MvcConfig/RefreshTokenInterceptor 依赖该 Bean，测试里提供 mock 即可。
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("集成测试: /user/code 应返回统一成功响应结构")
    void sendCode_shouldReturnSuccessPayload() throws Exception {
        when(userService.sendCode(eq("13812345678"), any())).thenReturn(Result.success("123456"));

        mockMvc.perform(post("/user/code").param("phone", "13812345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").value("123456"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({UserController.class, MvcConfig.class})
    static class TestApplication {
    }
}
