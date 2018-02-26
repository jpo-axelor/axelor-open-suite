package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.TempTimeCardLine;

import java.time.LocalDate;
import java.util.List;

public interface TempTimeCardLineService {

    /**
     * Deletes all existing {@code TempTimeCardLine}.
     */
    void invalidateTempTimeCardLines();

    /**
     * Generates {@code TempTimeCardLine}s for given {@code project}, {@code employee}
     * between inclusive {@code startDate} and {@code endDate}, after deleting existing ones.
     *
     * @param project
     * @param employee
     * @param startDate
     * @param endDate
     * @return
     */
    List<TempTimeCardLine> generateTempTimeCardLines(Project project, Employee employee, LocalDate startDate, LocalDate endDate);

}
