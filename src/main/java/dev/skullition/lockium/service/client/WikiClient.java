package dev.skullition.lockium.service.client;

import dev.skullition.lockium.model.ItemDetailResponse;
import dev.skullition.lockium.model.ItemsResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface WikiClient {
    
    @GetExchange("/health")
    void health();

    @GetExchange("/v1/items")
    ItemsResponse getItems();
    
    @GetExchange("/v1/items/{id}")
    ItemDetailResponse getItemDetail(@PathVariable int id);
}
