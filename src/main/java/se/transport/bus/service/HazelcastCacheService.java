package se.transport.bus.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

@RequiredArgsConstructor
@Service
public class HazelcastCacheService<E> {


    private final HazelcastInstance hazelcastInstance;

    private Mono<E> saveDataToCache(String cacheName, String key, E data) {
        if (data != null) {
            hazelcastInstance.getMap(cacheName).set(key, data);
            return Mono.just(data);
        }
        return Mono.empty();
    }

    public Mono<E> findFromCache(String cacheName, String key, Mono<E> fallBackMono) {
        return CacheMono
                .lookup(k -> Mono
                        .fromCallable(() -> {
                            IMap<String, E> map = hazelcastInstance.getMap(cacheName);
                            return map.get(key);
                        })
                        .map(Signal::next), key)
                .onCacheMissResume(() -> fallBackMono)
                .andWriteWith((k, sig) -> Mono.fromRunnable(() ->
                        saveDataToCache(cacheName, key, sig.get())));
    }
}
