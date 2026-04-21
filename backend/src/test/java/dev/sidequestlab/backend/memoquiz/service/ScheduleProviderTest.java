package dev.sidequestlab.backend.memoquiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleProviderTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ScheduleProvider scheduleProvider;

    @Test
    void boxesForDayWhenScheduleNotLoadedThrows() {
        assertThatThrownBy(() -> scheduleProvider.boxesForDay(1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Memoquiz schedule not loaded");
    }

    @Test
    void scheduleLengthWhenScheduleNotLoadedThrows() {
        assertThatThrownBy(scheduleProvider::scheduleLength)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Memoquiz schedule not loaded");
    }

    @Test
    void loadScheduleWhenObjectMapperFailsWrapsIOException() throws IOException {
        when(objectMapper.readValue(any(InputStream.class), anyScheduleTypeReference()))
            .thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> scheduleProvider.loadSchedule())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to load memoquiz schedule")
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void loadScheduleWithNullMapLeavesProviderUnavailable() throws IOException {
        when(objectMapper.readValue(any(InputStream.class), anyScheduleTypeReference())).thenReturn(null);

        scheduleProvider.loadSchedule();

        assertThatThrownBy(scheduleProvider::scheduleLength)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Memoquiz schedule not loaded");
    }

    @Test
    void boxesForDayWhenLoadedScheduleIsEmptyThrows() throws IOException {
        when(objectMapper.readValue(any(InputStream.class), anyScheduleTypeReference())).thenReturn(null);

        scheduleProvider.loadSchedule();

        assertThatThrownBy(() -> scheduleProvider.boxesForDay(1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Memoquiz schedule not loaded");
    }

    @Test
    void boxesForDayWithInvalidIndexThrows() throws IOException {
        Map<Integer, List<Integer>> loaded = Map.of(
            1, List.of(1, 2),
            2, List.of(1, 3)
        );
        when(objectMapper.readValue(any(InputStream.class), anyScheduleTypeReference())).thenReturn(loaded);
        scheduleProvider.loadSchedule();

        assertThatThrownBy(() -> scheduleProvider.boxesForDay(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("dayIndex must be between 1 and 2");
        assertThatThrownBy(() -> scheduleProvider.boxesForDay(3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("dayIndex must be between 1 and 2");
    }

    @Test
    void boxesForDayWhenDayHasNoConfiguredEntryThrows() throws IOException {
        Map<Integer, List<Integer>> loaded = Map.of(2, List.of(4));
        when(objectMapper.readValue(any(InputStream.class), anyScheduleTypeReference())).thenReturn(loaded);
        scheduleProvider.loadSchedule();

        assertThatThrownBy(() -> scheduleProvider.boxesForDay(1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No boxes configured for day 1");
    }

    @Test
    void boxesForDayWhenDayHasEmptyBoxesThrows() throws IOException {
        Map<Integer, List<Integer>> loaded = Map.of(1, List.of());
        when(objectMapper.readValue(any(InputStream.class), anyScheduleTypeReference())).thenReturn(loaded);
        scheduleProvider.loadSchedule();

        assertThatThrownBy(() -> scheduleProvider.boxesForDay(1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No boxes configured for day 1");
    }

    @Test
    void boxesForDayReturnsImmutableCopy() throws IOException {
        List<Integer> configured = new ArrayList<>(List.of(1, 3, 5));
        Map<Integer, List<Integer>> loaded = Map.of(1, configured);
        when(objectMapper.readValue(any(InputStream.class), anyScheduleTypeReference())).thenReturn(loaded);
        scheduleProvider.loadSchedule();

        List<Integer> returned = scheduleProvider.boxesForDay(1);
        configured.add(8);

        assertThat(returned).containsExactly(1, 3, 5);
        assertThatThrownBy(() -> returned.add(9))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void scheduleLengthReturnsLoadedSize() throws IOException {
        Map<Integer, List<Integer>> loaded = Map.of(
            1, List.of(1),
            2, List.of(2),
            3, List.of(1, 3)
        );
        when(objectMapper.readValue(any(InputStream.class), anyScheduleTypeReference())).thenReturn(loaded);
        scheduleProvider.loadSchedule();

        assertThat(scheduleProvider.scheduleLength()).isEqualTo(3);
    }

    @SuppressWarnings("unchecked")
    private TypeReference<Map<Integer, List<Integer>>> anyScheduleTypeReference() {
        return (TypeReference<Map<Integer, List<Integer>>>) any(TypeReference.class);
    }
}
