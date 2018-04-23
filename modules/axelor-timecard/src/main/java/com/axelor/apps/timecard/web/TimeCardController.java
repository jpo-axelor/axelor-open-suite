package com.axelor.apps.timecard.web;

import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.apps.timecard.db.repo.TimeCardRepository;
import com.axelor.apps.timecard.service.TimeCardService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class TimeCardController {

    /**
     * Generates {@code TimeCardLine}s for {@code TimeCard} in context.
     *
     * @param request
     * @param response
     */
    public void generateTimeCardLines(ActionRequest request, ActionResponse response) {
        TimeCard timeCard = Beans.get(TimeCardRepository.class).find(request.getContext().asType(TimeCard.class).getId());
        Beans.get(TimeCardService.class).generateTimeCardLines(timeCard);
        Beans.get(TimeCardService.class).attachScheduledTimeCardLines(timeCard);
        response.setReload(true);
    }

    /**
     * Computes different hours in {@code TimeCard}.
     *
     * @param request
     * @param response
     */
    public void computeHours(ActionRequest request, ActionResponse response) {
        TimeCard timeCard = Beans.get(TimeCardRepository.class).find(request.getContext().asType(TimeCard.class).getId());

        try {
            Beans.get(TimeCardService.class).computeHours(timeCard);
        } catch (AxelorException e) {
            TraceBackService.trace(e, "TimeCard, compute hours");
            response.setError(e.getMessage());
        }

        response.setReload(true);
    }

}
