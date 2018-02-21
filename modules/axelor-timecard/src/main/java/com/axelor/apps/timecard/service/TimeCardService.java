package com.axelor.apps.timecard.service;

import com.axelor.apps.timecard.db.TimeCard;

public interface TimeCardService {

    void generateTimeCardLines(TimeCard timeCard);

}
