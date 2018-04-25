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

import com.axelor.apps.base.db.Company;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.HRConfig;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TimeCardLineServiceImpl implements TimeCardLineService {

    protected TimeCardLineRepository timeCardLineRepo;

    @Inject
    public TimeCardLineServiceImpl(TimeCardLineRepository timeCardLineRepo) {
        this.timeCardLineRepo = timeCardLineRepo;
    }

    @Override
    public TimeCardLine generateTimeCardLine(Employee employee, Project project, LocalDate date, LocalTime startTime, LocalTime endTime, String lineType, boolean isDeletable) {
        TimeCardLine timeCardLine = new TimeCardLine();
        timeCardLine.setIsDeletable(isDeletable);

        timeCardLine.setEmployee(employee);
        timeCardLine.setProject(project);
        timeCardLine.setWeekDay(date.getDayOfWeek().getValue());

        timeCardLine.setDate(date);
        timeCardLine.setStartTime(startTime);
        timeCardLine.setEndTime(endTime);

        timeCardLine.setTypeSelect(lineType);

        return timeCardLine;
    }

    @Override
    public List<TimeCardLine> getScheduledTimeCardLine(@Nullable Project project, @Nullable Employee employee) {
        List<TimeCardLine> timeCardLines = new ArrayList<>();

        if (employee != null && project == null) {
            timeCardLines = timeCardLineRepo.findByEmployee(employee).fetch();
        } else if (employee == null && project != null) {
            timeCardLines = timeCardLineRepo.findByProject(project).fetch();
        } else if (employee != null && project != null) {
            timeCardLines = timeCardLineRepo.findByEmployeeAndProject(employee, project).fetch();
        }

        timeCardLines.removeIf(tcl -> tcl.getIsDeletable() || tcl.getTimeCard() != null);

        return timeCardLines;
    }

    @Override
    public BigDecimal getDurationNight(LocalTime startTime, LocalTime endTime, Company payCompany) {
        HRConfig hrConfig = payCompany.getHrConfig();
        LocalTime startNight = hrConfig.getStartNightHours();
        LocalTime endNight = hrConfig.getEndNightHours();

        LocalTime start = LocalTime.now();
        LocalTime end = LocalTime.now();
        if (startTime.isBefore(endNight) && (endTime.isBefore(endNight) || endTime.equals(endNight))) {
            start = startTime;
            end = endTime;
        } else if (startTime.isBefore(endNight) && endTime.isAfter(endNight)) {
            start = startTime;
            end = endNight;
        } else if (startTime.isAfter(endTime) && endTime.isBefore(startNight)) {
            start = null;
            end = null;
        } else if (startTime.isBefore(startNight) && endTime.isAfter(startNight)) {
            start = startNight;
            end = endTime;
        } else if ((startTime.equals(startNight) || startTime.isAfter(startNight)) && endTime.isAfter(startNight)) {
            start = startTime;
            end = endTime;
        }

        BigDecimal durationNight = BigDecimal.ZERO;
        if (start != null && end != null) {
            durationNight = BigDecimal.valueOf(Duration.between(start, end).toMinutes() / 60.0);
        }

        return durationNight;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public int generateExtraTCL(Employee oldEmployee, Employee newEmployee, List<Project> projects, LocalDate startDate, LocalDate endDate, boolean isContractual) {
        int totalGenerated = 0;

        List<TimeCardLine> timeCardLines = timeCardLineRepo.all().filter("employee = ? AND date >= ? AND date <= ? AND typeSelect = ?", oldEmployee, startDate, endDate, TimeCardLineRepository.TYPE_ABSENCE).fetch();
        for (TimeCardLine timeCardLine : timeCardLines) {
            TimeCardLine tcl = generateTimeCardLine(newEmployee,
                                                    timeCardLine.getProject(),
                                                    timeCardLine.getDate(),
                                                    timeCardLine.getStartTime(),
                                                    timeCardLine.getEndTime(),
                                                    TimeCardLineRepository.TYPE_EXTRA,
                                                    false);

            tcl.setIsSubstitution(true);
            tcl.setIsContractual(isContractual);

            timeCardLine.addSubstitutionTimeCardLineListItem(tcl);
            timeCardLineRepo.save(tcl);
            timeCardLineRepo.save(timeCardLine);

            totalGenerated++;
        }

        return totalGenerated;
    }

    @Override
    public BigDecimal getTotalContractualHours(Employee employee, LocalDate startDate, LocalDate endDate) {
        return getTotalHours(employee.getId(), startDate, endDate, TimeCardLineRepository.TYPE_CONTRACTUAL);
    }

    @Override
    public BigDecimal getTotalExtraHours(Employee employee, LocalDate startDate, LocalDate endDate) {
        return getTotalHours(employee.getId(), startDate, endDate, TimeCardLineRepository.TYPE_EXTRA);
    }

    @Override
    public BigDecimal getTotalAbsenceHours(Employee employee, LocalDate startDate, LocalDate endDate) {
        return getTotalHours(employee.getId(), startDate, endDate, TimeCardLineRepository.TYPE_ABSENCE);
    }

    @Override
    public BigDecimal getTotalNotPaidLeavesHours(Employee employee, LocalDate startDate, LocalDate endDate) {
        List<TimeCardLine> timeCardLines = timeCardLineRepo.all().filter("self.typeSelect = ? AND self.employee.id = ? AND self.date >= ? AND self.date <= ?", TimeCardLineRepository.TYPE_ABSENCE, employee.getId(), startDate, endDate).fetch();

        BigDecimal total = BigDecimal.ZERO;
        for (TimeCardLine timeCardLine : timeCardLines) {
            if (!timeCardLine.getLeaveLine().getLeaveReason().getPaidLeave()) {
                total = total.add(timeCardLine.getDuration());
            }
        }

        return total;
    }

    protected BigDecimal getTotalHours(Long employeeId, LocalDate startDate, LocalDate endDate, String typeLine) {
        List<TimeCardLine> timeCardLines = timeCardLineRepo.all().filter("self.typeSelect = ? AND self.employee.id = ? AND self.date >= ? AND self.date <= ?", typeLine, employeeId, startDate, endDate).fetch();

        BigDecimal total = BigDecimal.ZERO;
        for (TimeCardLine timeCardLine : timeCardLines) {
            total = total.add(timeCardLine.getDuration());
        }

        return total;
    }

    @Override
    public BigDecimal getSubstitutionsDuration(TimeCardLine timeCardLine) {
        List<TimeCardLine> tcls = timeCardLine.getSubstitutionTimeCardLineList();

        BigDecimal totalSubstitution = BigDecimal.ZERO;
        if (tcls != null) {
            for (TimeCardLine tcl : tcls) {
                totalSubstitution = totalSubstitution.add(tcl.getDuration());
            }
        }

        return totalSubstitution;
    }
}
