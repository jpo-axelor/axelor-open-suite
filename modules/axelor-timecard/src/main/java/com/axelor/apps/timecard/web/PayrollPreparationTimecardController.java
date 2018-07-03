/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
