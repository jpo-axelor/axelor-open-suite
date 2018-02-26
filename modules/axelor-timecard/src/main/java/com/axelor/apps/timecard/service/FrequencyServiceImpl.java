package com.axelor.apps.timecard.service;

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.timecard.db.Frequency;
import com.axelor.apps.timecard.db.repo.FrequencyRepository;
import com.google.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FrequencyServiceImpl implements FrequencyService {

    protected AppBaseService appBaseService;

    @Inject
    public FrequencyServiceImpl(AppBaseService appBaseService) {
        this.appBaseService = appBaseService;
    }

    @Override
    public String computeSummary(Frequency frequency) {
        StringBuilder summary = new StringBuilder();

        // Frequency
        if (frequency.getFrequencyTypeSelect().equals(FrequencyRepository.TYPE_EVERY_N_WEEKS)) {
            if (frequency.getEveryNWeeks() == 1) {
                summary.append("Toutes les semaines");
            } else {
                summary.append("Toutes les ").append(frequency.getEveryNWeeks()).append(" semaines");
            }
            summary.append(", les");
        } else if (frequency.getFrequencyTypeSelect().equals(FrequencyRepository.TYPE_MONTH_DAYS)) {
            summary.append("Tous les ");
            if (frequency.getFirst()) {
                summary.append("premiers");
            }
            if (frequency.getSecond()) {
                if (frequency.getFirst()) {
                    summary.append(", ");
                }
                summary.append("deuxièmes");
            }
            if (frequency.getThird()) {
                if (frequency.getFirst() || frequency.getSecond()) {
                    summary.append(", ");
                }
                summary.append("troisièmes");
            }
            if (frequency.getFourth()) {
                if (frequency.getFirst() || frequency.getSecond() || frequency.getThird()) {
                    summary.append(", ");
                }
                summary.append("quatrièmes");
            }
            if (frequency.getLast()) {
                if (frequency.getFirst() || frequency.getSecond() || frequency.getThird() || frequency.getFourth()) {
                    summary.append(", ");
                }
                summary.append("derniers");
            }
        }

        summary.append(" ");

        // Days
        if (frequency.getMonday() && frequency.getTuesday() && frequency.getWednesday() && frequency.getThursday() && frequency.getFriday() && !(frequency.getSaturday() || frequency.getSunday())) {
            summary.append("jours de semaine");
        } else if (frequency.getSaturday() && frequency.getSunday() && !(frequency.getMonday() || frequency.getTuesday() || frequency.getWednesday() || frequency.getThursday() || frequency.getFriday())) {
            summary.append("jours de week-end");
        } else if (frequency.getMonday() && frequency.getTuesday() && frequency.getWednesday() && frequency.getThursday() && frequency.getFriday() && frequency.getSaturday() && frequency.getSunday()) {
            summary.append("jours");
        } else {
            if (frequency.getMonday()) {
                summary.append("lundis");
            }
            if (frequency.getTuesday()) {
                if (frequency.getMonday()) {
                    summary.append(", ");
                }
                summary.append("mardis");
            }
            if (frequency.getWednesday()) {
                if (frequency.getMonday() || frequency.getTuesday()) {
                    summary.append(", ");
                }
                summary.append("mercredis");
            }
            if (frequency.getThursday()) {
                if (frequency.getMonday() || frequency.getTuesday() || frequency.getWednesday()) {
                    summary.append(", ");
                }
                summary.append("jeudis");
            }
            if (frequency.getFriday()) {
                if (frequency.getMonday() || frequency.getTuesday() || frequency.getWednesday() || frequency.getThursday()) {
                    summary.append(", ");
                }
                summary.append("vendredis");
            }
            if (frequency.getSaturday()) {
                if (frequency.getMonday() || frequency.getTuesday() || frequency.getWednesday() || frequency.getThursday() || frequency.getFriday()) {
                    summary.append(", ");
                }
                summary.append("samedis");
            }
            if (frequency.getSunday()) {
                if (frequency.getMonday() || frequency.getTuesday() || frequency.getWednesday() || frequency.getThursday() || frequency.getFriday() || frequency.getSaturday()) {
                    summary.append(", ");
                }
                summary.append("dimanches");
            }
        }

        summary.append(" de ");

        // Months
        if (frequency.getJanuary() && frequency.getFebruary() && frequency.getMarch() && frequency.getApril() && frequency.getMay() && frequency.getJune() &&
            frequency.getJuly() && frequency.getAugust() && frequency.getSeptember() && frequency.getOctober() && frequency.getNovember() && frequency.getDecember()) {
            summary.append("chaque mois");
        } else {
            if (frequency.getJanuary()) {
                summary.append("janvier");
            }
            if (frequency.getFebruary()) {
                if (frequency.getJanuary()) {
                    summary.append(", ");
                }
                summary.append("février");
            }
            if (frequency.getMarch()) {
                if (frequency.getJanuary() || frequency.getFebruary()) {
                    summary.append(", ");
                }
                summary.append("mars");
            }
            if (frequency.getApril()) {
                if (frequency.getJanuary() || frequency.getFebruary() || frequency.getMarch()) {
                    summary.append(", ");
                }
                summary.append("avril");
            }
            if (frequency.getMay()) {
                if (frequency.getJanuary() || frequency.getFebruary() || frequency.getMarch() || frequency.getApril()) {
                    summary.append(", ");
                }
                summary.append("mai");
            }
            if (frequency.getJune()) {
                if (frequency.getJanuary() || frequency.getFebruary() || frequency.getMarch() || frequency.getApril() || frequency.getMay()) {
                    summary.append(", ");
                }
                summary.append("juin");
            }
            if (frequency.getJuly()) {
                if (frequency.getJanuary() || frequency.getFebruary() || frequency.getMarch() || frequency.getApril() || frequency.getMay() || frequency.getJune()) {
                    summary.append(", ");
                }
                summary.append("juillet");
            }
            if (frequency.getAugust()) {
                if (frequency.getJanuary() || frequency.getFebruary() || frequency.getMarch() || frequency.getApril() || frequency.getMay() || frequency.getJune() ||
                    frequency.getJuly()) {
                    summary.append(", ");
                }
                summary.append("août");
            }
            if (frequency.getSeptember()) {
                if (frequency.getJanuary() || frequency.getFebruary() || frequency.getMarch() || frequency.getApril() || frequency.getMay() || frequency.getJune() ||
                    frequency.getJuly() || frequency.getAugust()) {
                    summary.append(", ");
                }
                summary.append("septembre");
            }
            if (frequency.getOctober()) {
                if (frequency.getJanuary() || frequency.getFebruary() || frequency.getMarch() || frequency.getApril() || frequency.getMay() || frequency.getJune() ||
                    frequency.getJuly() || frequency.getAugust() || frequency.getSeptember()) {
                    summary.append(", ");
                }
                summary.append("octobre");
            }
            if (frequency.getNovember()) {
                if (frequency.getJanuary() || frequency.getFebruary() || frequency.getMarch() || frequency.getApril() || frequency.getMay() || frequency.getJune() ||
                    frequency.getJuly() || frequency.getAugust() || frequency.getSeptember() || frequency.getOctober()) {
                    summary.append(", ");
                }
                summary.append("novembre");
            }
            if (frequency.getDecember()) {
                if (frequency.getJanuary() || frequency.getFebruary() || frequency.getMarch() || frequency.getApril() || frequency.getMay() || frequency.getJune() ||
                    frequency.getJuly() || frequency.getAugust() || frequency.getSeptember() || frequency.getOctober() || frequency.getNovember()) {
                    summary.append(", ");
                }
                summary.append("décembre");
            }
        }

        summary.append(".");

        return summary.toString();
    }

    @Override
    public List<LocalDate> getDates(Frequency frequency) {
        int year = appBaseService.getTodayDate().getYear();

        List<Integer> months = getMonths(frequency);
        List<Integer> days = getDays(frequency);
        List<Integer> occurences = getOccurences(frequency);

        Set<LocalDate> dates = new HashSet<>();

        for (Integer month : months) {
            for (Integer day : days) {
                for (Integer occurence : occurences) {
                    dates.add(getDay(day, occurence, year, month));
                }
            }
        }

        return new ArrayList<>(dates);
    }

    /**
     * Retrieves a LocalDate instance of given date in arguments.
     *
     * @param dayOfWeek
     * @param dayOfWeekInMonth
     * @param year
     * @param month
     * @return
     */
    public LocalDate getDay(int dayOfWeek, int dayOfWeekInMonth, int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        return LocalDate.of(year, month, cal.get(Calendar.DATE));
    }

    @Override
    public List<Integer> getMonths(Frequency frequency) {
        List<Integer> months = new ArrayList<>();

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

    @Override
    public List<Integer> getDays(Frequency frequency) {
        List<Integer> days = new ArrayList<>();

        if (frequency.getSunday()) { days.add(1); }
        if (frequency.getMonday()) { days.add(2); }
        if (frequency.getTuesday()) { days.add(3); }
        if (frequency.getWednesday()) { days.add(4); }
        if (frequency.getThursday()) { days.add(5); }
        if (frequency.getFriday()) { days.add(6); }
        if (frequency.getSaturday()) { days.add(7); }

        return days;
    }

    @Override
    public List<Integer> getOccurences(Frequency frequency) {
        List<Integer> occurences = new ArrayList<>();

        // TODO: every N weeks

        if (frequency.getFirst()) { occurences.add(1); }
        if (frequency.getSecond()) { occurences.add(2); }
        if (frequency.getThird()) { occurences.add(3); }
        if (frequency.getFourth()) { occurences.add(4); }
        if (frequency.getLast()) { occurences.add(-1); }

        return occurences;
    }
}
