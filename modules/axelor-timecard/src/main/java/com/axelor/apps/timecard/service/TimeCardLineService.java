package com.axelor.apps.timecard.service;

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
