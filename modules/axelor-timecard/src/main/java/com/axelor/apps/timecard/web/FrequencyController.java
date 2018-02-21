package com.axelor.apps.timecard.web;

import com.axelor.apps.timecard.db.Frequency;
import com.axelor.apps.timecard.service.FrequencyService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class FrequencyController {

    public void computeSummary(ActionRequest request, ActionResponse response) {
        Frequency frequency = request.getContext().asType(Frequency.class);
        response.setValue("summary", Beans.get(FrequencyService.class).computeSummary(frequency));
    }

}
