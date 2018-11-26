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

import com.axelor.apps.timecard.db.Timecard;
import com.axelor.apps.timecard.db.repo.TimecardRepository;
import com.axelor.apps.timecard.service.TimecardService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class TimecardController {

  /** Generates {@code TimecardLine}s for {@code Timecard} in context. */
  public void generateTimecardLines(ActionRequest request, ActionResponse response) {
    Timecard timecard =
        Beans.get(TimecardRepository.class)
            .find(request.getContext().asType(Timecard.class).getId());

    TimecardService timecardService = Beans.get(TimecardService.class);
    timecardService.detachAbsenceTimecardLines(timecard);
    timecardService.generateTimecardLines(timecard);
    timecardService.attachScheduledTimecardLines(timecard);

    response.setReload(true);
  }

  /**
   * Computes different hours in {@code Timecard}.
   *
   * @param request
   * @param response
   */
  public void computeHours(ActionRequest request, ActionResponse response) {
    Timecard timecard =
        Beans.get(TimecardRepository.class)
            .find(request.getContext().asType(Timecard.class).getId());

    try {
      Beans.get(TimecardService.class).computeAll(timecard);
    } catch (AxelorException e) {
      TraceBackService.trace(e, "Timecard");
      response.setError(e.getMessage());
    }

    response.setReload(true);
  }

  public void send(ActionRequest request, ActionResponse response) {
    Timecard timecard =
        Beans.get(TimecardRepository.class)
            .find(request.getContext().asType(Timecard.class).getId());

    try {
      Beans.get(TimecardService.class).send(timecard);
    } catch (AxelorException e) {
      TraceBackService.trace(e, "Timecard");
      response.setError(e.getMessage());
    }

    response.setReload(true);
  }

  public void validate(ActionRequest request, ActionResponse response) {
    Timecard timecard =
        Beans.get(TimecardRepository.class)
            .find(request.getContext().asType(Timecard.class).getId());

    try {
      Beans.get(TimecardService.class).validate(timecard);
    } catch (AxelorException e) {
      TraceBackService.trace(e, "Timecard");
      response.setError(e.getMessage());
    }

    response.setReload(true);
  }

  public void refuse(ActionRequest request, ActionResponse response) {
    Timecard timecard =
        Beans.get(TimecardRepository.class)
            .find(request.getContext().asType(Timecard.class).getId());

    Beans.get(TimecardService.class).refuse(timecard);
  }
}
