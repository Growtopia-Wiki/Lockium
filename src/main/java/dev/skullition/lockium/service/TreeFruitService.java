package dev.skullition.lockium.service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class TreeFruitService {
    private volatile Map<Integer, Integer> maxDrops = Map.of();

    @PostConstruct
    public void load() {
        Map<Integer, Integer> map = new HashMap<>();
        var resource = new ClassPathResource("data/TreeFruitMaxDrop.txt");

        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            reader.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("//"))
                    .forEach(line -> {
                        String[] parts = line.split("\\|");
                        if (parts.length == 2) {
                            try {
                                int id = Integer.parseInt(parts[0].trim());
                                int drops = Integer.parseInt(parts[1].trim());
                                map.put(id, drops);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load TreeFruitMaxDrop.txt", e);
        }

        maxDrops = Collections.unmodifiableMap(map);
    }

    public int getMaxDrop(int itemId) {
        return maxDrops.getOrDefault(itemId, 4); // 4 = default not in file.
    }

    public void reload() {
        load();
    }

    public int size() {
        return maxDrops.size();
    }
}