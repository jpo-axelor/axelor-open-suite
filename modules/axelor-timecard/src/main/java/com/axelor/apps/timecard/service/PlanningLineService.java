package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;

import javax.annotation.Nullable;
import java.util.List;

public interface PlanningLineService {

    /**
     * Computes monthly wage of given {@code PlanningLine}
     *
     * @param planningLine PlanningLine to compute monthly wage
     */
    void computeMonthlyWage(PlanningLine planningLine);

    /**
     * Retrieves list of PlanningLine for given {@code project} and {@code employee}
     *
     * @param project
     * @param employee
     * @return List of PlanningLine for given {@code project} and {@code employee}
     */
    List<PlanningLine> getPlanningLines(@Nullable Project project, @Nullable Employee employee);

}
