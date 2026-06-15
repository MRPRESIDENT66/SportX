package com.example.sportx.Search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时确保 spots 索引按 IK 映射存在。
 *
 * <p>容错处理：ES 不可用（如本地未启动）时仅记录警告、不阻断应用启动，
 * 让不依赖搜索的功能照常工作；待 ES 就绪后可调用 /spots/search/reindex 重建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotIndexInitializer implements ApplicationRunner {

    private final SpotSearchService spotSearchService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            spotSearchService.ensureIndex();
        } catch (Exception e) {
            log.warn("Skipped ES spot index init (Elasticsearch unavailable?): {}", e.getMessage());
        }
    }
}
