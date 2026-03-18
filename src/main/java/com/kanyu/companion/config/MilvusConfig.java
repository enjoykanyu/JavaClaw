package com.kanyu.companion.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {
    
    private String host = "localhost";
    
    private int port = 19530;
    
    private String database = "default";
    
    private long connectTimeout = 10;
    
    private long keepAliveTime = 60;
    
    private long keepAliveTimeout = 20;
    
    private boolean secure = false;
    
    @Bean
    public MilvusServiceClient milvusServiceClient() {
        log.info("Initializing Milvus client: {}:{}", host, port);
        
        ConnectParam connectParam = ConnectParam.newBuilder()
            .withHost(host)
            .withPort(port)
            .withDatabaseName(database)
            .withConnectTimeout(connectTimeout, TimeUnit.SECONDS)
            .withKeepAliveTime(keepAliveTime, TimeUnit.SECONDS)
            .withKeepAliveTimeout(keepAliveTimeout, TimeUnit.SECONDS)
            .withSecure(secure)
            .build();
        
        return new MilvusServiceClient(connectParam);
    }
}
