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
import com.axelor.apps.timecard.db.TempTimeCardLine;

import java.time.LocalDate;
import java.util.List;

public interface TempTimeCardLineService {

    /**
     * Deletes all existing {@code TempTimeCardLine}.
     */
    void invalidateTempTimeCardLines();

    /**
     * Generates {@code TempTimeCardLine}s for given {@code project}, {@code employee}
     * between inclusive {@code startDate} and {@code endDate}, after deleting existing ones.
     *
     * @param project
     * @param employee
     * @param startDate
     * @param endDate
     * @return
     */
    List<TempTimeCardLine> generateTempTimeCardLines(Project project, Employee employee, LocalDate startDate, LocalDate endDate);

}
