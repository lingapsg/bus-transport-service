package se.transport.bus.config;

import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulerConfiguration {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Scheduled(cron = "${trafiklab.clear-cache.cron-expression}")
    public void clearCache() {
        hazelcastInstance.getMap("bus-stops").clear();
        hazelcastInstance.getMap("journey-points").clear();
    }
}
