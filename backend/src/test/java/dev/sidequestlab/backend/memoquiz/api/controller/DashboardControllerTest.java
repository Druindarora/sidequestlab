package dev.sidequestlab.backend.memoquiz.api.controller;

import dev.sidequestlab.backend.memoquiz.api.dto.TodayDashboardDto;
import dev.sidequestlab.backend.memoquiz.service.DashboardService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardControllerTest {

    private final StubDashboardService dashboardService = new StubDashboardService();
    private final DashboardController controller = new DashboardController(dashboardService);

    @Test
    void todayReturnsOkAndDelegatesToService() {
        TodayDashboardDto expected = new TodayDashboardDto(
            LocalDate.of(2026, 4, 21),
            3,
            true,
            List.of(1, 3),
            12,
            48,
            new TodayDashboardDto.LastSessionSummary(
                20,
                16,
                80.0,
                Instant.parse("2026-04-20T08:30:00Z"),
                480,
                2
            ),
            List.of(
                new TodayDashboardDto.BoxesOverviewItem(1, 10, true),
                new TodayDashboardDto.BoxesOverviewItem(2, 15, false)
            )
        );
        dashboardService.todayResult = expected;

        ResponseEntity<TodayDashboardDto> response = controller.today();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        assertThat(dashboardService.todayCallCount).isEqualTo(1);
    }

    @Test
    void todayPropagatesServiceExceptionBecauseControllerDoesNotHandleIt() {
        RuntimeException failure = new RuntimeException("service failure");
        dashboardService.todayException = failure;

        assertThatThrownBy(() -> controller.today()).isSameAs(failure);
        assertThat(dashboardService.todayCallCount).isEqualTo(1);
    }

    private static final class StubDashboardService extends DashboardService {
        private int todayCallCount;
        private TodayDashboardDto todayResult;
        private RuntimeException todayException;

        private StubDashboardService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public TodayDashboardDto today() {
            todayCallCount++;
            if (todayException != null) {
                throw todayException;
            }
            return todayResult;
        }
    }
}
