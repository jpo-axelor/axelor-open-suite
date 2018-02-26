package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;

import javax.annotation.Nullable;
import java.util.List;

public interface PlanningLineService {

    void computeMonthlyWage(PlanningLine planningLine);

    List<PlanningLine> getPlanningLines(@Nullable Project project, @Nullable Employee employee);

}
