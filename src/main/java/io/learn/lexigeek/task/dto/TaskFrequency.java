package io.learn.lexigeek.task.dto;

/**
 * Task regeneration frequency options.
 * - DAILY: Runs every day at specified time
 * - EVERY_N_DAYS: Runs every N days (N specified in frequencyValue)
 * - WEEKLY: Runs on a specific day of week (1-7, Monday-Sunday, specified in frequencyValue)
 * - MONTHLY: Runs on a specific day of month (1-31, specified in frequencyValue)
 */
public enum TaskFrequency {
    DAILY,
    EVERY_N_DAYS,
    WEEKLY,
    MONTHLY
}
