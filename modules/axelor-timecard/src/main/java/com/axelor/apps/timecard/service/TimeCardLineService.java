package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.TimeCardLine;

import javax.annotation.Nullable;
import java.util.List;

public interface TimeCardLineService {

    /**
     * Retrieves list of scheduled TimeCardLine for given {@code project} and {@code employee}
     *
     * @param project
     * @param employee
     * @return List of scheduled TimeCardLine for given {@code project} and {@code employee}
     */
    List<TimeCardLine> getScheduledTimeCardLine(@Nullable Project project, @Nullable Employee employee);

}
