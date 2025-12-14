package io.learn.lexigeek.statistics.controller;

import io.learn.lexigeek.statistics.StatisticsFacade;
import io.learn.lexigeek.statistics.dto.UserStatDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class StatisticsController {

    private static final class Routes {
        private static final String STATISTICS = "/statistics";
    }

    private final StatisticsFacade statisticsFacade;

    @GetMapping(Routes.STATISTICS)
    List<UserStatDto> getUserStatistics(@RequestParam(required = false) final String startDate,
                                        @RequestParam(required = false) final String endDate,
                                        @RequestParam(required = false) final List<UUID> languageUuids,
                                        @RequestParam(required = false) final Boolean showTotal,
                                        @RequestParam(required = false) final Boolean showStars) {
        return statisticsFacade.getUserStatistics(startDate, endDate, languageUuids, showTotal, showStars);
    }
}
