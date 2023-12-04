package com.atguigu.gulimall.order.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.servlet.FlashMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
public class MySessionConfig implements BeanClassLoaderAware {
    private ClassLoader loader;

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(CopyOnWriteArrayList.class, CopyOnWriteArrayListMixin.class);
        mapper.addMixIn(FlashMap.class, FlashMapMixin.class);
        mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
        return mapper;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.loader = classLoader;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setDomainName("gulimall.com");
        serializer.setCookieName("JSESSIONID");
        return serializer;
    }
}
