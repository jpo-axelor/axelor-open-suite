package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.TempTimeCardLine;

import java.time.LocalDate;
import java.util.List;

public interface TempTimeCardLineService {

    void invalidateTempTimeCardLines();

    List<TempTimeCardLine> generateTempTimeCardLines(Project project, Employee employee, LocalDate startDate, LocalDate endDate);

}
