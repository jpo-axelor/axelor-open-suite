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
package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;

import javax.annotation.Nullable;
import java.util.List;

public interface PlanningLineService {

    /**
     * Computes monthly wage of given {@code PlanningLine}
     *
     * @param planningLine PlanningLine to compute monthly wage
     */
    void computeMonthlyWage(PlanningLine planningLine);

    /**
     * Retrieves list of PlanningLine for given {@code project} and {@code employee}
     *
     * @param project
     * @param employee
     * @return List of PlanningLine for given {@code project} and {@code employee}
     */
    List<PlanningLine> getPlanningLines(@Nullable Project project, @Nullable Employee employee);

}
