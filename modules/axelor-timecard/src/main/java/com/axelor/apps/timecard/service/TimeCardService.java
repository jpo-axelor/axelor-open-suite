package com.axelor.apps.timecard.service;

import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.exception.AxelorException;

public interface TimeCardService {

    /**
     * Generates {@code TimeCardLine}s for given {@code TimeCard}, after deleting existing ones.
     *
     * @param timeCard
     */
    void generateTimeCardLines(TimeCard timeCard);

    /**
     * Attaches previously (orphan) generated {@code TimeCardLine}s to given {@code TimeCard}.
     *
     * @param timeCard
     */
    void attachScheduledTimeCardLines(TimeCard timeCard);

    /**
     * Computes hours in given {@code TimeCard}.
     *
     * @param timeCard
     * @throws AxelorException
     */
    void computeHours(TimeCard timeCard) throws AxelorException;

}
