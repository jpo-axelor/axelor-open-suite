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
import com.axelor.apps.timecard.db.TimeCardLine;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TimeCardLineService {

    /**
     * Generates a {@code TimeCardLine} with given arguments.
     *
     * @param employee
     * @param project
     * @param date
     * @param startTime
     * @param endTime
     * @param lineType
     * @param isDeletable
     * @return
     */
    TimeCardLine generateTimeCardLine(Employee employee, Project project, LocalDate date, LocalTime startTime, LocalTime endTime, String lineType, boolean isDeletable);

    /**
     * Retrieves list of scheduled TimeCardLine for given {@code project} and {@code employee}
     *
     * @param project
     * @param employee
     * @return List of scheduled TimeCardLine for given {@code project} and {@code employee}
     */
    List<TimeCardLine> getScheduledTimeCardLine(@Nullable Project project, @Nullable Employee employee);

    /**
     * Returns total duration between night hours range.
     *
     * @param startTime
     * @param endTime
     * @param payCompany
     * @return
     */
    BigDecimal getDurationNight(LocalTime startTime, LocalTime endTime, Company payCompany);

    /**
     * Generates 'extra' {@code TimeCardLine}s with given arguments.
     *
     * @param oldEmployee
     * @param newEmployee
     * @param projects
     * @param startDate
     * @param endDate
     * @param isContractual
     */
    void generateExtraTCL(Employee oldEmployee, Employee newEmployee, List<Project> projects, LocalDate startDate, LocalDate endDate, boolean isContractual);

    /**
     * Computes and returns total contractual hours for given employee within given range.
     *
     * @param employee
     * @param startDate
     * @param endDate
     * @return
     */
    BigDecimal getTotalContractualHours(Employee employee, LocalDate startDate, LocalDate endDate);

    /**
     * Computes and returns total extra hours for given employee within given range.
     *
     * @param employee
     * @param startDate
     * @param endDate
     * @return
     */
    BigDecimal getTotalExtraHours(Employee employee, LocalDate startDate, LocalDate endDate);

    /**
     * Computes and returns total 'not paid leaves' hours for given employee within given range.
     *
     * @param employee
     * @param startDate
     * @param endDate
     * @return
     */
    BigDecimal getTotalNotPaidLeavesHours(Employee employee, LocalDate startDate, LocalDate endDate);

}
