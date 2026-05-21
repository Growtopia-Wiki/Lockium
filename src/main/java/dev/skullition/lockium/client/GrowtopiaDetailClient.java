package dev.skullition.lockium.client;

import dev.skullition.lockium.model.GrowtopiaDetail;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface GrowtopiaDetailClient {
    @GetExchange
    GrowtopiaDetail getGrowtopiaDetail();
}
