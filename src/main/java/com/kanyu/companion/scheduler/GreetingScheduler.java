package com.kanyu.companion.scheduler;

import com.kanyu.companion.service.GreetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GreetingScheduler {
    
    private final GreetingService greetingService;
    
    @Scheduled(cron = "${companion.greeting.morning-cron:0 0 8 * * ?}")
    public void sendMorningGreetings() {
        log.info("Executing morning greeting scheduler");
        greetingService.sendGreetingToAllUsers("morning");
    }
    
    @Scheduled(cron = "${companion.greeting.evening-cron:0 0 22 * * ?}")
    public void sendEveningGreetings() {
        log.info("Executing evening greeting scheduler");
        greetingService.sendGreetingToAllUsers("evening");
    }
}
