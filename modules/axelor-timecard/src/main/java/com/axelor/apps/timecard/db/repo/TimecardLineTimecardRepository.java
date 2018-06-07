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

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.apps.timecard.service.LeaveServiceTimecardImpl;
import com.axelor.apps.timecard.service.TimecardLineService;
import com.axelor.inject.Beans;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class TimecardLineTimecardRepository extends TimecardLineRepository {

  @Override
  public TimecardLine save(TimecardLine timecardLine) {
    // Full name
    StringBuilder fullName = new StringBuilder();

    String typeSelect = timecardLine.getTypeSelect();
    if (typeSelect != null) {
      switch (typeSelect) {
        case TYPE_CONTRACTUAL:
          fullName.append("[C]");
          break;
        case TYPE_EXTRA:
          fullName.append("[+]");
          break;
        case TYPE_ABSENCE:
          fullName.append("[A]");
          break;
      }

      fullName.append(" ");
    }

    Employee employee = timecardLine.getEmployee();
    if (employee != null) {
      fullName.append(employee.getName());
      fullName.append(" ");
    }

    Project project = timecardLine.getProject();
    if (project != null) {
      fullName.append("- ");
      fullName.append(project.getName());
    }

    timecardLine.setFullName(fullName.toString());

    LocalTime startTime = timecardLine.getStartTime();
    LocalTime endTime = timecardLine.getEndTime();

    timecardLine.setStartDateTime(LocalDateTime.of(timecardLine.getDate(), startTime));
    timecardLine.setEndDateTime(LocalDateTime.of(timecardLine.getDate(), endTime));

    timecardLine.setDuration(
        BigDecimal.valueOf(Duration.between(startTime, endTime).toMinutes() / 60.0));

    timecardLine.setDurationNight(
        Beans.get(TimecardLineService.class)
            .getDurationNight(
                startTime,
                endTime,
                timecardLine.getEmployee().getMainEmploymentContract().getPayCompany()));

    List<TimecardLine> tcls = timecardLine.getSubstitutionTimecardLineList();
    if (tcls != null) {
      BigDecimal totalSubstitution = BigDecimal.ZERO;
      for (TimecardLine tcl : tcls) {
        totalSubstitution = totalSubstitution.add(tcl.getDuration());
      }
      timecardLine.setTotalSubstitutionHours(totalSubstitution);

      Beans.get(LeaveServiceTimecardImpl.class)
          .computeDurationTotals(timecardLine.getLeaveRequest());
    }

    return super.save(timecardLine);
  }
}
