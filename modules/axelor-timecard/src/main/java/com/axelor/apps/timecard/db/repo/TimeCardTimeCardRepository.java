package com.axelor.apps.timecard.db.repo;

import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.inject.Beans;

public class TimeCardTimeCardRepository extends TimeCardRepository {

    @Override
    public TimeCard save(TimeCard timeCard) {
        for (TimeCardLine timeCardLine : timeCard.getTimeCardLineList()) {
            Beans.get(TimeCardLineRepository.class).save(timeCardLine);
        }

        return super.save(timeCard);
    }
}
