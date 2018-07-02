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
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.EmployeeSuggestion;
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.exception.AxelorException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public interface TimecardLineService {

  /** Generates a {@link TimecardLine} with given arguments. */
  TimecardLine generateTimecardLine(
      Employee employee,
      Project project,
      LocalDate date,
      LocalTime startTime,
      LocalTime endTime,
      String lineType,
      boolean isDeletable);

  /**
   * Retrieves list of scheduled TimecardLine for given {@link Project} and {@link Employee}
   *
   * @return List of scheduled TimecardLine for given {@link Project} and {@link Employee}
   */
  List<TimecardLine> getScheduledTimecardLine(
      @Nullable Project project, @Nullable Employee employee);

  /** Returns total duration between night hours range. */
  BigDecimal getDurationNight(LocalTime startTime, LocalTime endTime, Company payCompany)
      throws AxelorException;

  /** Generates 'extra' {@link TimecardLine}s with given arguments. */
  int generateExtraTCL(
      Employee oldEmployee,
      Employee newEmployee,
      List<Project> projects,
      LocalDate startDate,
      LocalDate endDate,
      boolean isContractual)
      throws AxelorException;

  /** Computes and returns total contractual hours for given employee within given range. */
  BigDecimal getTotalContractualHours(
      Long timecardId, Employee employee, LocalDate startDate, LocalDate endDate);

  /** Computes and returns total extra hours for given employee within given range. */
  BigDecimal getTotalExtraHours(
      Long timecardId, Employee employee, LocalDate startDate, LocalDate endDate);

  /** Computes and returns total absence hours for given employee within given range. */
  BigDecimal getTotalAbsenceHours(
      Long timecardId, Employee employee, LocalDate startDate, LocalDate endDate);

  /** Computes and returns total 'not paid leaves' hours for given employee within given range. */
  BigDecimal getTotalNotPaidLeavesHours(
      Long timecardId, Employee employee, LocalDate startDate, LocalDate endDate);

  /** Returns total substitution hours in given {@link TimecardLine}. */
  BigDecimal getSubstitutionsDuration(TimecardLine timecardLine);

  /** Returns the code for given {@code typeSelect}. */
  String getTypeSelectCode(String typeSelect);

  /** Returns a set of {@link EmployeeSuggestion}s for given arguments. */
  Set<EmployeeSuggestion> suggestEmployee(
      Long projectId, Long employeeToReplaceId, @Nullable LocalDate date);
}
