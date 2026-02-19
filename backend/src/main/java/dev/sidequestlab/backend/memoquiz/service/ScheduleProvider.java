package dev.sidequestlab.backend.memoquiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ScheduleProvider {

    private static final String RESOURCE_PATH = "memoquiz/study-schedule-64.json";

    private final ObjectMapper objectMapper;
    private Map<Integer, List<Integer>> schedule;
    private int scheduleLength;

    public ScheduleProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadSchedule() {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            Map<Integer, List<Integer>> loaded = objectMapper.readValue(
                inputStream,
                new TypeReference<Map<Integer, List<Integer>>>() {}
            );
            schedule = loaded == null ? Collections.emptyMap() : loaded;
            scheduleLength = schedule.size();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load memoquiz schedule", ex);
        }
    }

    public List<Integer> boxesForDay(int dayIndex) {
        if (schedule == null || schedule.isEmpty()) {
            throw new IllegalStateException("Memoquiz schedule not loaded");
        }
        if (dayIndex < 1 || dayIndex > scheduleLength) {
            throw new IllegalArgumentException("dayIndex must be between 1 and " + scheduleLength);
        }
        List<Integer> boxes = schedule.get(dayIndex);
        if (boxes == null || boxes.isEmpty()) {
            throw new IllegalStateException("No boxes configured for day " + dayIndex);
        }
        return List.copyOf(boxes);
    }

    public int scheduleLength() {
        if (schedule == null || schedule.isEmpty()) {
            throw new IllegalStateException("Memoquiz schedule not loaded");
        }
        return scheduleLength;
    }
}
