package io.learn.lexigeek.statistics.dto;

import java.util.List;

public record StatisticsQueryParams(String startDate,
                                    String endDate,
                                    List<String> languageUuids,
                                    Boolean showTotal,
                                    Boolean showStars) {
}
