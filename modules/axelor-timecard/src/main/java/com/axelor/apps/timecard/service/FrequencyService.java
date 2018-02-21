package com.axelor.apps.timecard.service;

import com.axelor.apps.timecard.db.Frequency;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface FrequencyService {

    String computeSummary(Frequency frequency);

    List<LocalDate> getDates(Frequency frequency);

    Set<Integer> getYears(LocalDate start, LocalDate end);

    List<Integer> getMonths(Frequency frequency);

    List<Integer> getDays(Frequency frequency);

    List<Integer> getOccurences(Frequency frequency);

}
