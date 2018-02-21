package com.axelor.apps.timecard.service;

import com.axelor.apps.timecard.db.Frequency;
import com.axelor.apps.timecard.db.repo.FrequencyRepository;

public class FrequencyServiceImpl implements FrequencyService {

    @Override
    public String computeSummary(Frequency frequency) {
        StringBuilder summary = new StringBuilder();

        // Frequency
        if (frequency.getFrequencyTypeSelect().equals(FrequencyRepository.FREQUENCY_TYPE_EVERY_N_WEEKS)) {
            if (frequency.getEveryNWeeks() == 1) {
                summary.append("Toutes les semaines");
            } else {
                summary.append("Toutes les ").append(frequency.getEveryNWeeks()).append(" semaines");
            }
            summary.append(", les");
        } else if (frequency.getFrequencyTypeSelect().equals(FrequencyRepository.FREQUENCY_TYPE_MONTH_DAYS)) {
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
            if (frequency.getFifth()) {
                if (frequency.getFirst() || frequency.getSecond() || frequency.getThird() || frequency.getFourth()) {
                    summary.append(", ");
                }
                summary.append("cinquièmes");
            }
            if (frequency.getLast()) {
                if (frequency.getFirst() || frequency.getSecond() || frequency.getThird() || frequency.getFourth() || frequency.getFifth()) {
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
}
