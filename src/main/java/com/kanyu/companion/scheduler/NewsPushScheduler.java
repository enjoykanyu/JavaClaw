package com.kanyu.companion.scheduler;

import com.kanyu.companion.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsPushScheduler {
    
    private final NewsService newsService;
    
    @Scheduled(cron = "${companion.news.push-cron:0 0 * * * ?}")
    public void pushNews() {
        log.info("Executing news push scheduler");
        newsService.pushNewsToAllUsers();
    }
}
