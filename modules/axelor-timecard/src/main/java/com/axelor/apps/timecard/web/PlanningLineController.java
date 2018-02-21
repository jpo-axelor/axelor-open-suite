package com.axelor.apps.timecard.web;

import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.service.PlanningLineService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class PlanningLineController {

    public void computeMonthlyWage(ActionRequest request, ActionResponse response) {
        PlanningLine planningLine = Beans.get(PlanningLineRepository.class).find(request.getContext().asType(PlanningLine.class).getId());
        Beans.get(PlanningLineService.class).computeMonthlyWage(planningLine);
        response.setReload(true);
    }

}
