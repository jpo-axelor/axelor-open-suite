package com.axelor.apps.timecard.service;

import com.axelor.apps.timecard.db.PlanningLine;

public interface PlanningLineService {

    void computeMonthlyWage(PlanningLine planningLine);

}
