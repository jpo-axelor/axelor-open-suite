package com.axelor.apps.timecard.service;

import com.axelor.apps.timecard.db.Frequency;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;

public interface FrequencyService {

    /**
     * Computes summary of given {@code Frequency}.
     *
     * @param frequency
     * @return Summary of given {@code Frequency}
     */
    String computeSummary(Frequency frequency);

    /**
     * Retrieves all possible dates for given {@code Frequency} and {@code year} (or current year if null).
     * If fourth and last day of week are checked in given {@code Frequency} and it is the same date,
     * it will only appear once in return list.
     *
     * @param frequency
     * @return
     */
    List<LocalDate> getDates(Frequency frequency, @Nullable Integer year);

    /**
     * Retrieves months checked in given {@code Frequency}.
     *
     * @param frequency
     * @return
     */
    List<Integer> getMonths(Frequency frequency);

    /**
     * Retrieves days of week checked in given {@code Frequency}.
     *
     * @param frequency
     * @return
     */
    List<Integer> getDays(Frequency frequency);

    /**
     * Retrieves occurences checked in given {@code Frequency}.
     *
     * @param frequency
     * @return
     */
    List<Integer> getOccurences(Frequency frequency);

}
