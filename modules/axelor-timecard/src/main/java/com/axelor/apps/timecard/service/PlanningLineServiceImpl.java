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

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.Frequency;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class PlanningLineServiceImpl implements PlanningLineService {

  protected PlanningLineRepository planningLineRepo;
  protected FrequencyService frequencyService;
  protected AppBaseService appBaseService;

  @Inject
  public PlanningLineServiceImpl(
      PlanningLineRepository planningLineRepo,
      FrequencyService frequencyService,
      AppBaseService appBaseService) {
    this.planningLineRepo = planningLineRepo;
    this.frequencyService = frequencyService;
    this.appBaseService = appBaseService;
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void computeMonthlyHours(PlanningLine planningLine) {
    Frequency frequency = planningLine.getFrequency();

    List<LocalDate> dates = frequencyService.getDates(frequency, null);

    double lineDuration =
        Duration.between(planningLine.getStartTime(), planningLine.getEndTime()).toMinutes() / 60.0;
    planningLine.setMonthlyHours(BigDecimal.valueOf((dates.size() * lineDuration / 12)));
    planningLineRepo.save(planningLine);
  }

  @Override
  public List<PlanningLine> getPlanningLines(
      @Nullable Project project, @Nullable Employee employee) {
    List<PlanningLine> planningLines = new ArrayList<>();

    if (employee != null && project == null) {
      planningLines = planningLineRepo.findByEmployee(employee).fetch();
    } else if (employee == null && project != null) {
      planningLines = planningLineRepo.findByProject(project).fetch();
    } else if (employee != null && project != null) {
      planningLines = planningLineRepo.findByEmployeeAndProject(employee, project).fetch();
    }

    return planningLines;
  }
}
