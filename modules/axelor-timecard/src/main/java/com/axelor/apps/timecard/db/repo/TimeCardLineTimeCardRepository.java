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
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.service.LeaveServiceTimeCardImpl;
import com.axelor.apps.timecard.service.TimeCardLineService;
import com.axelor.inject.Beans;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class TimeCardLineTimeCardRepository extends TimeCardLineRepository {

    @Override
    public TimeCardLine save(TimeCardLine timeCardLine) {
        // Full name
        StringBuilder fullName = new StringBuilder();

        String typeSelect = timeCardLine.getTypeSelect();
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

        Employee employee = timeCardLine.getEmployee();
        if (employee != null) {
            fullName.append(employee.getName());
            fullName.append(" ");
        }

        Project project = timeCardLine.getProject();
        if (project != null) {
            fullName.append("- ");
            fullName.append(project.getName());
        }

        timeCardLine.setFullName(fullName.toString());


        LocalTime startTime = timeCardLine.getStartTime();
        LocalTime endTime = timeCardLine.getEndTime();

        timeCardLine.setStartDateTime(LocalDateTime.of(timeCardLine.getDate(), startTime));
        timeCardLine.setEndDateTime(LocalDateTime.of(timeCardLine.getDate(), endTime));

        timeCardLine.setDuration(BigDecimal.valueOf(Duration.between(startTime, endTime).toMinutes() / 60.0));

        timeCardLine.setDurationNight(Beans.get(TimeCardLineService.class).getDurationNight(startTime, endTime, timeCardLine.getEmployee().getMainEmploymentContract().getPayCompany()));

        List<TimeCardLine> tcls = timeCardLine.getSubstitutionTimeCardLineList();
        if (tcls != null) {
            BigDecimal totalSubstitution = BigDecimal.ZERO;
            for (TimeCardLine tcl : tcls) {
                totalSubstitution = totalSubstitution.add(tcl.getDuration());
            }
            timeCardLine.setTotalSubstitutionHours(totalSubstitution);

            Beans.get(LeaveServiceTimeCardImpl.class).computeDurationTotals(timeCardLine.getLeaveRequest());
        }

        return super.save(timeCardLine);
    }
}
