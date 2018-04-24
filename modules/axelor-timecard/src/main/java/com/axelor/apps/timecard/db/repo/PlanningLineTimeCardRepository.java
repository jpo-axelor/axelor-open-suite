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
