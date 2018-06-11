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
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.apps.timecard.db.repo.TimecardLineRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class TimecardLineServiceImpl implements TimecardLineService {

  protected TimecardLineRepository timecardLineRepo;

  @Inject
  public TimecardLineServiceImpl(TimecardLineRepository timecardLineRepo) {
    this.timecardLineRepo = timecardLineRepo;
  }

  @Override
  public TimecardLine generateTimecardLine(
      Employee employee,
      Project project,
      LocalDate date,
      LocalTime startTime,
      LocalTime endTime,
      String lineType,
      boolean isDeletable) {
    TimecardLine timecardLine = new TimecardLine();
    timecardLine.setIsDeletable(isDeletable);

    timecardLine.setEmployee(employee);
    timecardLine.setProject(project);
    timecardLine.setWeekDay(date.getDayOfWeek().getValue());

    timecardLine.setDate(date);
    timecardLine.setStartTime(startTime);
    timecardLine.setEndTime(endTime);

    timecardLine.setTypeSelect(lineType);

    return timecardLine;
  }

  @Override
  public List<TimecardLine> getScheduledTimecardLine(
      @Nullable Project project, @Nullable Employee employee) {
    List<TimecardLine> timecardLines = new ArrayList<>();

    if (employee != null && project == null) {
      timecardLines = timecardLineRepo.findByEmployee(employee).fetch();
    } else if (employee == null && project != null) {
      timecardLines = timecardLineRepo.findByProject(project).fetch();
    } else if (employee != null && project != null) {
      timecardLines = timecardLineRepo.findByEmployeeAndProject(employee, project).fetch();
    }

    timecardLines.removeIf(tcl -> tcl.getIsDeletable() || tcl.getTimecard() != null);

    return timecardLines;
  }

  @Override
  public BigDecimal getDurationNight(LocalTime startTime, LocalTime endTime, Company payCompany) {
    if (startTime != null && endTime != null) {
      HRConfig hrConfig = payCompany.getHrConfig();
      LocalTime startNight = hrConfig.getStartNightHours();
      LocalTime endNight = hrConfig.getEndNightHours();

      LocalTime start = LocalTime.now();
      LocalTime end = LocalTime.now();
      if (startTime.isBefore(endNight)
          && (endTime.isBefore(endNight) || endTime.equals(endNight))) {
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
      } else if ((startTime.equals(startNight) || startTime.isAfter(startNight))
          && endTime.isAfter(startNight)) {
        start = startTime;
        end = endTime;
      }

      BigDecimal durationNight = BigDecimal.ZERO;
      if (start != null && end != null) {
        durationNight = BigDecimal.valueOf(Duration.between(start, end).toMinutes() / 60.0);
      }

      return durationNight;
    }

    return BigDecimal.ZERO;
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public int generateExtraTCL(
      Employee oldEmployee,
      Employee newEmployee,
      List<Project> projects,
      LocalDate startDate,
      LocalDate endDate,
      boolean isContractual) {
    int totalGenerated = 0;

    List<TimecardLine> timecardLines =
        timecardLineRepo
            .all()
            .filter(
                "employee = ? AND date >= ? AND date <= ? AND typeSelect = ?",
                oldEmployee,
                startDate,
                endDate,
                TimecardLineRepository.TYPE_ABSENCE)
            .fetch();
    for (TimecardLine timecardLine : timecardLines) {
      TimecardLine tcl =
          generateTimecardLine(
              newEmployee,
              timecardLine.getProject(),
              timecardLine.getDate(),
              timecardLine.getStartTime(),
              timecardLine.getEndTime(),
              TimecardLineRepository.TYPE_EXTRA,
              false);

      tcl.setIsSubstitution(true);
      tcl.setIsContractual(isContractual);

      timecardLine.addSubstitutionTimecardLineListItem(tcl);
      timecardLineRepo.save(tcl);
      timecardLineRepo.save(timecardLine);

      totalGenerated++;
    }

    return totalGenerated;
  }

  @Override
  public BigDecimal getTotalContractualHours(
      Employee employee, LocalDate startDate, LocalDate endDate) {
    return getTotalHours(
        employee.getId(), startDate, endDate, TimecardLineRepository.TYPE_CONTRACTUAL);
  }

  @Override
  public BigDecimal getTotalExtraHours(Employee employee, LocalDate startDate, LocalDate endDate) {
    return getTotalHours(employee.getId(), startDate, endDate, TimecardLineRepository.TYPE_EXTRA);
  }

  @Override
  public BigDecimal getTotalAbsenceHours(
      Employee employee, LocalDate startDate, LocalDate endDate) {
    return getTotalHours(employee.getId(), startDate, endDate, TimecardLineRepository.TYPE_ABSENCE);
  }

  @Override
  public BigDecimal getTotalNotPaidLeavesHours(
      Employee employee, LocalDate startDate, LocalDate endDate) {
    List<TimecardLine> timecardLines =
        timecardLineRepo
            .all()
            .filter(
                "self.typeSelect = ? AND self.employee.id = ? AND self.date >= ? AND self.date <= ?",
                TimecardLineRepository.TYPE_ABSENCE,
                employee.getId(),
                startDate,
                endDate)
            .fetch();

    BigDecimal total = BigDecimal.ZERO;
    for (TimecardLine timecardLine : timecardLines) {
      if (!timecardLine.getLeaveLine().getLeaveReason().getPaidLeave()) {
        total = total.add(timecardLine.getDuration());
      }
    }

    return total;
  }

  protected BigDecimal getTotalHours(
      Long employeeId, LocalDate startDate, LocalDate endDate, String typeLine) {
    List<TimecardLine> timecardLines =
        timecardLineRepo
            .all()
            .filter(
                "self.typeSelect = ? AND self.employee.id = ? AND self.date >= ? AND self.date <= ?",
                typeLine,
                employeeId,
                startDate,
                endDate)
            .fetch();

    BigDecimal total = BigDecimal.ZERO;
    for (TimecardLine timecardLine : timecardLines) {
      total = total.add(timecardLine.getDuration());
    }

    return total;
  }

  @Override
  public BigDecimal getSubstitutionsDuration(TimecardLine timecardLine) {
    List<TimecardLine> tcls = timecardLine.getSubstitutionTimecardLineList();

    BigDecimal totalSubstitution = BigDecimal.ZERO;
    if (tcls != null) {
      for (TimecardLine tcl : tcls) {
        totalSubstitution = totalSubstitution.add(tcl.getDuration());
      }
    }

    return totalSubstitution;
  }

  @Override
  public String getTypeSelectCode(String typeSelect) {
    String code;

    switch (typeSelect) {
      case TimecardLineRepository.TYPE_CONTRACTUAL:
        code = "[C]";
        break;

      case TimecardLineRepository.TYPE_EXTRA:
        code = "[+]";
        break;

      case TimecardLineRepository.TYPE_ABSENCE:
        code = "[A]";
        break;

      default:
        code = "";
        break;
    }

    return code;
  }
}
