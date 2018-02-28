package com.axelor.apps.timecard.db.repo;

import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.service.FrequencyService;
import com.axelor.inject.Beans;

public class PlanningLineTimeCardRepository extends PlanningLineRepository {

    @Override
    public PlanningLine save(PlanningLine planningLine) {
        planningLine.getFrequency().setSummary(Beans.get(FrequencyService.class).computeSummary(planningLine.getFrequency()));

        return super.save(planningLine);
    }
}
