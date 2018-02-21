package com.axelor.apps.timecard.web;

import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.apps.timecard.db.repo.TimeCardRepository;
import com.axelor.apps.timecard.service.TimeCardService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class TimeCardController {

    public void generateTimeCardLines(ActionRequest request, ActionResponse response) {
        TimeCard timeCard = Beans.get(TimeCardRepository.class).find(request.getContext().asType(TimeCard.class).getId());
        Beans.get(TimeCardService.class).generateTimeCardLines(timeCard);
        response.setReload(true);
    }

}
