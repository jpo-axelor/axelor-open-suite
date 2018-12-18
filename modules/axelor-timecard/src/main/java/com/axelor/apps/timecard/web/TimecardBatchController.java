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

import com.axelor.apps.base.db.Batch;
import com.axelor.apps.timecard.db.TimecardBatch;
import com.axelor.apps.timecard.db.repo.TimecardBatchRepository;
import com.axelor.apps.timecard.service.batch.TimecardBatchService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TimecardBatchController {

  @Inject private TimecardBatchService timecardBatchService;

  @Inject private TimecardBatchRepository timecardBatchRepo;

  /**
   * Run the batch checking every timecard for monthly hours over 110% of the employee's theoretical
   * monthly hours.
   *
   * @param request
   * @param response
   */
  public void actionCheckMonthlyHours(ActionRequest request, ActionResponse response) {

    TimecardBatch timecardBatch = request.getContext().asType(TimecardBatch.class);

    Batch batch =
        timecardBatchService.checkMonthlyHours(timecardBatchRepo.find(timecardBatch.getId()));

    if (batch != null) response.setFlash(batch.getComments());
    response.setReload(true);
  }
}
