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

import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.service.PlanningLineService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class PlanningLineController {

    /**
     * Computes monthly wage of {@code PlanningLine} in context.
     *
     * @param request
     * @param response
     */
    public void computeMonthlyWage(ActionRequest request, ActionResponse response) {
        PlanningLine planningLine = Beans.get(PlanningLineRepository.class).find(request.getContext().asType(PlanningLine.class).getId());
        Beans.get(PlanningLineService.class).computeMonthlyWage(planningLine);
        response.setReload(true);
    }

}
