package com.axelor.apps.timecard.service;

import com.axelor.apps.timecard.db.TimeCard;

public interface TimeCardService {

    /**
     * Generates {@code TimeCardLine}s for given {@code TimeCard}, after deleting existing ones.
     *
     * @param timeCard
     */
    void generateTimeCardLines(TimeCard timeCard);

}
