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
import com.axelor.apps.timecard.db.TempTimeCardLine;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.TempTimeCardLineRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TempTimeCardLineServiceImpl implements TempTimeCardLineService {

    protected TempTimeCardLineRepository tempTimeCardLineRepo;
    protected PlanningLineService planningLineService;
    protected FrequencyService frequencyService;
    protected TimeCardLineService timeCardLineService;

    @Inject
    public TempTimeCardLineServiceImpl(TempTimeCardLineRepository tempTimeCardLineRepo, PlanningLineService planningLineService,
                                       FrequencyService frequencyService, TimeCardLineService timeCardLineService) {
        this.tempTimeCardLineRepo = tempTimeCardLineRepo;
        this.planningLineService = planningLineService;
        this.frequencyService = frequencyService;
        this.timeCardLineService = timeCardLineService;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void invalidateTempTimeCardLines() {
        tempTimeCardLineRepo.all().delete();
        tempTimeCardLineRepo.flush();
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public List<TempTimeCardLine> generateTempTimeCardLines(Project project, Employee employee, LocalDate startDate, LocalDate endDate) {
        invalidateTempTimeCardLines();

        List<TempTimeCardLine> tempTimeCardLines = new ArrayList<>();

        // Get planning lines
        List<PlanningLine> planningLines = planningLineService.getPlanningLines(project, employee);
        // Generate temp time card lines
        for (PlanningLine planningLine : planningLines) {
            List<LocalDate> dates = frequencyService.getDates(planningLine.getFrequency(), null);
            for (LocalDate date : dates) {
                if (date.equals(startDate) || date.equals(endDate) || date.isAfter(startDate) && date.isBefore(endDate)) {
                    TempTimeCardLine tempTimeCardLine = generateTempTimeCardLine(planningLine.getEmployee(), planningLine.getProject(), date, planningLine.getStartTime(), planningLine.getEndTime(), TempTimeCardLineRepository.TYPE_CONTRACTUAL);

                    tempTimeCardLines.add(tempTimeCardLine);
                    tempTimeCardLineRepo.save(tempTimeCardLine);
                }
            }
        }


        // Get scheduled time card lines
        List<TimeCardLine> timeCardLines = timeCardLineService.getScheduledTimeCardLine(project, employee);
        // Generate temp time card lines
        for (TimeCardLine timeCardLine : timeCardLines) {
            LocalDate date = timeCardLine.getDate();
            if (date.equals(startDate) || date.equals(endDate) || date.isAfter(startDate) && date.isBefore(endDate)) {
                TempTimeCardLine tempTimeCardLine = generateTempTimeCardLine(timeCardLine.getEmployee(), timeCardLine.getProject(), timeCardLine.getDate(), timeCardLine.getStartTime(), timeCardLine.getEndTime(), timeCardLine.getTypeSelect());

                tempTimeCardLines.add(tempTimeCardLine);
                tempTimeCardLineRepo.save(tempTimeCardLine);
            }
        }

        return tempTimeCardLines;
    }

    public TempTimeCardLine generateTempTimeCardLine(Employee employee, Project project, LocalDate date, LocalTime startTime, LocalTime endTime, String lineType) {
        TempTimeCardLine tempTimeCardLine = new TempTimeCardLine();

        tempTimeCardLine.setEmployee(employee);
        tempTimeCardLine.setProject(project);
        tempTimeCardLine.setTypeSelect(lineType);

        tempTimeCardLine.setStartDateTime(LocalDateTime.of(date, startTime));
        tempTimeCardLine.setEndDateTime(LocalDateTime.of(date, endTime));

        return tempTimeCardLine;
    }

}
