package dev.skullition.lockium.service.client;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface WikiClient {
    //TODO: Hit /items endpoint
    
    @GetExchange("/health")
    void health();
}
