package com.example.allinmarket.common.redis;

import com.example.allinmarket.common.redis.enums.RedisChannels;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String scheme = sslEnabled ? "rediss" : "redis";
        config.useSingleServer()
                .setAddress(scheme + "://" + redisHost + ":" + redisPort);
        return Redisson.create(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        template.setConnectionFactory(redisConnectionFactory);

        template.setKeySerializer(new StringRedisSerializer());

        template.setValueSerializer(RedisSerializer.json());

        template.setHashKeySerializer(new StringRedisSerializer());

        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter restockListenerAdapter,
            MessageListenerAdapter orderListenerAdapter
    ) {
        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                restockListenerAdapter,
                new PatternTopic(RedisChannels.RESTOCK_NOTIFICATION)
        );
        container.addMessageListener(
                orderListenerAdapter,
                new PatternTopic(RedisChannels.ORDER_STATUS_UPDATE_NOTIFICATION)
        );

        return container;
    }

    @Bean
    public MessageListenerAdapter restockListenerAdapter(
            RedisSubscriber subscriber
    ) {
        MessageListenerAdapter adapter =
                new MessageListenerAdapter(subscriber, "onRestockMessage");
        adapter.setSerializer(RedisSerializer.json());
        return adapter;
    }

    @Bean
    public MessageListenerAdapter orderListenerAdapter(
            RedisSubscriber subscriber
    ) {
        MessageListenerAdapter adapter =
                new MessageListenerAdapter(subscriber, "onOrderStatusMessage");
        adapter.setSerializer(RedisSerializer.json());
        return adapter;

    }
}
