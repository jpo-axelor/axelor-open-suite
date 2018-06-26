package com.axelor.apps.timecard.web;

import com.axelor.apps.hr.db.PayrollPreparation;
import com.axelor.apps.hr.db.repo.PayrollPreparationRepository;
import com.axelor.apps.timecard.service.PayrollPreparationTimecardServiceImpl;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class PayrollPreparationTimecardController {

  public void calculate(ActionRequest request, ActionResponse response) {
    PayrollPreparation payrollPrep =
        Beans.get(PayrollPreparationRepository.class)
            .find(request.getContext().asType(PayrollPreparation.class).getId());

    try {
      Beans.get(PayrollPreparationTimecardServiceImpl.class).calculate(payrollPrep);
    } catch (AxelorException e) {
      TraceBackService.trace(response, e);
    }

    response.setReload(true);
  }
  
  public void close(ActionRequest request, ActionResponse response) {
    PayrollPreparation payrollPrep =
        Beans.get(PayrollPreparationRepository.class)
            .find(request.getContext().asType(PayrollPreparation.class).getId());

    Beans.get(PayrollPreparationTimecardServiceImpl.class).close(payrollPrep);

    response.setReload(true);
  }
}
