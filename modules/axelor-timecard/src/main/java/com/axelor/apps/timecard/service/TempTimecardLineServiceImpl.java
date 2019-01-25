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

import com.axelor.apps.base.service.FrequencyService;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.TempTimecardLine;
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.apps.timecard.db.repo.TempTimecardLineRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TempTimecardLineServiceImpl implements TempTimecardLineService {

  protected TempTimecardLineRepository tempTimecardLineRepo;
  protected PlanningLineService planningLineService;
  protected FrequencyService frequencyService;
  protected TimecardLineService timecardLineService;

  @Inject
  public TempTimecardLineServiceImpl(
      TempTimecardLineRepository tempTimecardLineRepo,
      PlanningLineService planningLineService,
      FrequencyService frequencyService,
      TimecardLineService timecardLineService) {
    this.tempTimecardLineRepo = tempTimecardLineRepo;
    this.planningLineService = planningLineService;
    this.frequencyService = frequencyService;
    this.timecardLineService = timecardLineService;
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void invalidateTempTimecardLines() {
    tempTimecardLineRepo.all().delete();
    tempTimecardLineRepo.flush();
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public List<TempTimecardLine> generateTempTimecardLines(
      Project project, Employee employee, LocalDate startDate, LocalDate endDate) {
    invalidateTempTimecardLines();

    List<TempTimecardLine> tempTimecardLines = new ArrayList<>();

    // Get planning lines
    List<PlanningLine> planningLines = planningLineService.getPlanningLines(project, employee);
    // Generate temp time card lines
    for (PlanningLine planningLine : planningLines) {
      List<LocalDate> dates =
          frequencyService.getDates(planningLine.getFrequency(), startDate, endDate);
      for (LocalDate date : dates) {
        if (date.equals(startDate)
            || date.equals(endDate)
            || date.isAfter(startDate) && date.isBefore(endDate)) {
          TempTimecardLine tempTimecardLine =
              generateTempTimecardLine(
                  planningLine.getEmployee(),
                  planningLine.getProject(),
                  date,
                  planningLine.getStartTime(),
                  planningLine.getEndTime(),
                  TempTimecardLineRepository.TYPE_CONTRACTUAL);

          tempTimecardLines.add(tempTimecardLine);
          tempTimecardLineRepo.save(tempTimecardLine);
        }
      }
    }

    // Get scheduled time card lines
    List<TimecardLine> timecardLines =
        timecardLineService.getScheduledTimecardLine(project, employee);
    // Generate temp time card lines
    for (TimecardLine timecardLine : timecardLines) {
      LocalDate date = timecardLine.getDate();
      if (date.equals(startDate)
          || date.equals(endDate)
          || date.isAfter(startDate) && date.isBefore(endDate)) {
        TempTimecardLine tempTimecardLine =
            generateTempTimecardLine(
                timecardLine.getEmployee(),
                timecardLine.getProject(),
                timecardLine.getDate(),
                timecardLine.getStartTime(),
                timecardLine.getEndTime(),
                timecardLine.getTypeSelect());

        tempTimecardLines.add(tempTimecardLine);
        tempTimecardLineRepo.save(tempTimecardLine);
      }
    }

    return tempTimecardLines;
  }

  public TempTimecardLine generateTempTimecardLine(
      Employee employee,
      Project project,
      LocalDate date,
      LocalTime startTime,
      LocalTime endTime,
      String lineType) {
    TempTimecardLine tempTimecardLine = new TempTimecardLine();

    tempTimecardLine.setEmployee(employee);
    tempTimecardLine.setProject(project);
    tempTimecardLine.setTypeSelect(lineType);

    tempTimecardLine.setStartDateTime(LocalDateTime.of(date, startTime));
    tempTimecardLine.setEndDateTime(LocalDateTime.of(date, endTime));

    return tempTimecardLine;
  }
}
