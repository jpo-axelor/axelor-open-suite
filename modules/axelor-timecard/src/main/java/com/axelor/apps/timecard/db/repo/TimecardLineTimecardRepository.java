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

import com.axelor.apps.base.db.Company;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.Timecard;
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.apps.timecard.service.TimecardLineService;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.persistence.PersistenceException;

public class TimecardLineTimecardRepository extends TimecardLineRepository {

  @Override
  public TimecardLine save(TimecardLine timecardLine) {
    TimecardLineService timecardLineService = Beans.get(TimecardLineService.class);

    // Full name
    StringBuilder fullName = new StringBuilder();

    String typeSelect = timecardLine.getTypeSelect();
    if (typeSelect != null) {
      fullName.append(timecardLineService.getTypeSelectCode(typeSelect));
    }

    Employee employee = timecardLine.getEmployee();
    if (employee != null) {
      fullName.append(" ");
      fullName.append(employee.getName());
    }

    Project project = timecardLine.getProject();
    if (project != null) {
      fullName.append(" - ");
      fullName.append(project.getName());
    }

    timecardLine.setFullName(fullName.toString());

    LocalTime startTime = timecardLine.getStartTime();
    LocalTime endTime = timecardLine.getEndTime();

    timecardLine.setStartDateTime(LocalDateTime.of(timecardLine.getDate(), startTime));
    timecardLine.setEndDateTime(LocalDateTime.of(timecardLine.getDate(), endTime));

    timecardLine.setDuration(
        BigDecimal.valueOf(Duration.between(startTime, endTime).toMinutes() / 60.0));

    if (timecardLine.getSubstitutionTimecardLineList() != null) {
      BigDecimal totalSubstitutionsHours = BigDecimal.ZERO;
      for (TimecardLine line : timecardLine.getSubstitutionTimecardLineList()) {
        totalSubstitutionsHours = totalSubstitutionsHours.add(line.getDuration());
      }
      timecardLine.setTotalSubstitutionHours(totalSubstitutionsHours);
    }

    if (timecardLine.getIsSubstitution() && timecardLine.getTimecard() == null) {
      TimecardLine absenceTimeCardLine = timecardLine.getAbsenceTimecardLine();

      Company company = null;
      if (absenceTimeCardLine != null) {
        if (absenceTimeCardLine.getTimecard() != null) {
          company = absenceTimeCardLine.getTimecard().getCompany();
        } else if (absenceTimeCardLine.getLeaveRequest() != null) {
          company = absenceTimeCardLine.getLeaveRequest().getCompany();
        } else if (absenceTimeCardLine.getEmployee().getMainEmploymentContract() != null) {
          company = absenceTimeCardLine.getEmployee().getMainEmploymentContract().getPayCompany();
        }
      }

      Timecard timecard =
          Beans.get(TimecardRepository.class)
              .all()
              .filter(
                  "company = :company AND employee = :employee AND fromDate <= :date AND toDate >= :date AND statusSelect = :draftStatus")
              .bind("company", company)
              .bind("employee", timecardLine.getEmployee())
              .bind("date", timecardLine.getDate())
              .bind("draftStatus", TimecardRepository.STATUS_DRAFT)
              .fetchOne();

      timecardLine.setTimecard(timecard);
    }

    if (employee != null) {
      try {
        timecardLine.setDurationNight(
            timecardLineService.getDurationNight(
                startTime, endTime, employee.getMainEmploymentContract().getPayCompany()));
      } catch (NullPointerException e) {
        throw new PersistenceException(
            I18n.get("Please configure a main employement contract for employee")
                + " "
                + employee.getName()
                + ".",
            e);
      } catch (AxelorException e) {
        throw new PersistenceException(e);
      }
    }

    return super.save(timecardLine);
  }
}
