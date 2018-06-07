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

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.Planning;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.TempTimecardLine;
import com.axelor.apps.timecard.service.PlanningLineService;
import com.axelor.apps.timecard.service.TempTimecardLineService;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;

public class PlanningController {

  /**
   * Sets defaults values for {@code Planning} in context.
   *
   * @param request
   * @param response
   */
  public void setDefaults(ActionRequest request, ActionResponse response) {
    LocalDate today = Beans.get(AppBaseService.class).getTodayDate();

    Calendar cal = Calendar.getInstance();
    cal.set(today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth());
    cal.add(Calendar.MONTH, 1);

    cal.set(Calendar.DATE, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
    response.setValue(
        "startDate",
        LocalDate.of(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)));

    cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
    response.setValue(
        "endDate",
        LocalDate.of(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)));
  }

  /**
   * Computes the total monthly wage of the {@code Planning} in context.
   *
   * @param request
   * @param response
   */
  public void computeMonthlyWage(ActionRequest request, ActionResponse response) {
    PlanningLineService planningLineService = Beans.get(PlanningLineService.class);

    Planning planning = request.getContext().asType(Planning.class);
    Project project = planning.getProject();
    Employee employee = planning.getEmployee();

    BigDecimal monthlyWage = BigDecimal.ZERO;

    List<PlanningLine> planningLines = planningLineService.getPlanningLines(project, employee);
    for (PlanningLine planningLine : planningLines) {
      // update planning line monthly wage
      planningLineService.computeMonthlyWage(planningLine);

      // add the PL monthly wage to the total
      monthlyWage = monthlyWage.add(planningLine.getMonthlyWage());
    }

    response.setValue("monthlyWage", monthlyWage);
  }

  /**
   * Opens a calendar view for {@code Planning} in context.
   *
   * @param request
   * @param response
   */
  public void preview(ActionRequest request, ActionResponse response) {
    Planning planning = request.getContext().asType(Planning.class);
    Project project = planning.getProject();
    Employee employee = planning.getEmployee();

    List<TempTimecardLine> tempTimecardLines =
        Beans.get(TempTimecardLineService.class)
            .generateTempTimecardLines(
                project, employee, planning.getStartDate(), planning.getEndDate());
    if (tempTimecardLines.size() == 0) {
      response.setNotify("Pas d'évènements à afficher.");
      return;
    }

    ActionViewBuilder actionView = null;
    if (employee != null && project == null) {
      actionView =
          ActionView.define("Prévisualisation - " + employee.getName())
              .add("calendar", "temp-time-card-line-calendar-by-project")
              .domain("self.employee.id = :_employeeId")
              .context("_employeeId", employee.getId());
    } else if (employee == null && project != null) {
      actionView =
          ActionView.define("Prévisualisation - " + project.getName())
              .add("calendar", "temp-time-card-line-calendar-by-employee")
              .domain("self.project.id = :_projectId")
              .context("_projectId", project.getId());
    } else if (employee != null && project != null) {
      actionView =
          ActionView.define("Prévisualisation - " + employee.getName())
              .add("calendar", "temp-time-card-line-calendar-by-project")
              .domain("self.employee.id = :_employeeId AND self.project.id = :_projectId")
              .context("_employeeId", employee.getId())
              .context("_projectId", project.getId());
    }

    if (actionView != null) {
      response.setView(actionView.model(TempTimecardLine.class.getName()).map());
    }
  }
}
