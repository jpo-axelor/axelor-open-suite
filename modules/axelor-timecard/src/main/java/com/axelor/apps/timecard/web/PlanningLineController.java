package com.axelor.apps.timecard.web;

import com.axelor.apps.timecard.db.Frequency;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlanningLineController {

    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void computeMensu(ActionRequest request, ActionResponse response) {
        PlanningLine planningLine = request.getContext().asType(PlanningLine.class);

        LocalDate startDate = LocalDate.parse((String)request.getContext().get("startDate"));
        LocalDate endDate = LocalDate.parse((String)request.getContext().get("endDate"));
        Frequency frequency = planningLine.getFrequency();

        Set<Integer> years = getYears(startDate, endDate);
        List<Integer> months = getMonths(frequency);
        List<Integer> days = getDays(frequency);
        List<Integer> occurences = getOccurences(frequency);

        List<LocalDate> dates = new ArrayList<LocalDate>();

        for (Integer year : years) {
            for (Integer month : months) {
                for (Integer day : days) {
                    for (Integer occurence : occurences) {
                        dates.add(getDay(day, occurence, year, month));
                    }
                }
            }
        }

        dates.removeIf(d -> d.isBefore(startDate) || d.isAfter(endDate));

        Integer mensu = 0;

        // TODO: calcul mensu

        response.setValue("mensu", mensu);
    }

    public static LocalDate getDay(int dayOfWeek, int dayOfWeekInMonth, int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        return LocalDate.of(year, month, cal.get(Calendar.DATE));
    }

    public Set<Integer> getYears(LocalDate start, LocalDate end) {
        Set<Integer> years = new HashSet<Integer>();

        LocalDate startDate = LocalDate.from(start);
        LocalDate endDate = LocalDate.from(end);

        while(startDate.isBefore(endDate)) {
            years.add(startDate.getYear());
            startDate = startDate.plusMonths(1);
        }

        return years;
    }

    public List<Integer> getMonths(Frequency frequency) {
        List<Integer> months = new ArrayList<Integer>();

        if (frequency.getJanuary()) { months.add(1); }
        if (frequency.getFebruary()) { months.add(2); }
        if (frequency.getMarch()) { months.add(3); }
        if (frequency.getApril()) { months.add(4); }
        if (frequency.getMay()) { months.add(5); }
        if (frequency.getJune()) { months.add(6); }
        if (frequency.getJuly()) { months.add(7); }
        if (frequency.getAugust()) { months.add(8); }
        if (frequency.getSeptember()) { months.add(9); }
        if (frequency.getOctober()) { months.add(10); }
        if (frequency.getNovember()) { months.add(11); }
        if (frequency.getDecember()) { months.add(12); }

        return months;
    }

    public List<Integer> getDays(Frequency frequency) {
        List<Integer> days = new ArrayList<Integer>();

        if (frequency.getSunday()) { days.add(1); }
        if (frequency.getMonday()) { days.add(2); }
        if (frequency.getTuesday()) { days.add(3); }
        if (frequency.getWednesday()) { days.add(4); }
        if (frequency.getThursday()) { days.add(5); }
        if (frequency.getFriday()) { days.add(6); }
        if (frequency.getSaturday()) { days.add(7); }

        return days;
    }

    public List<Integer> getOccurences(Frequency frequency) {
        List<Integer> occurences = new ArrayList<Integer>();

        // TODO: every N weeks

        if (frequency.getFirst()) { occurences.add(1); }
        if (frequency.getSecond()) { occurences.add(2); }
        if (frequency.getThird()) { occurences.add(3); }
        if (frequency.getFourth()) { occurences.add(4); }
        if (frequency.getFifth()) { occurences.add(5); }
        if (frequency.getLast()) { occurences.add(-1); }

        return occurences;
    }

}
