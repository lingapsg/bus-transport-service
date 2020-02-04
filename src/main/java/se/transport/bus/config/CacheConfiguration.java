package se.transport.bus.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastInstanceFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@EnableCaching
@Configuration
public class CacheConfiguration {

    @Bean
    @Primary
    public HazelcastInstance hazelcastInstance(Config config) {
        return new HazelcastInstanceFactory(config).getHazelcastInstance();
    }

    @Bean
    public Config hazelcastConfig() {
        Config config = new Config();
        config.setInstanceName("bus-cache")
                .addMapConfig(
                        new MapConfig()
                                .setName("busLine")
                                .setMaxSizeConfig(new MaxSizeConfig(100, MaxSizeConfig.MaxSizePolicy.FREE_HEAP_SIZE))
                                .setEvictionPolicy(EvictionPolicy.LRU)
                                .setTimeToLiveSeconds(-1));
        return config;
    }

}
