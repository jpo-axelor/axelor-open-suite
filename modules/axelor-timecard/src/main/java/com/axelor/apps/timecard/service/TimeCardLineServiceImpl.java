package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.google.inject.Inject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TimeCardLineServiceImpl implements TimeCardLineService {

    protected TimeCardLineRepository timeCardLineRepo;

    @Inject
    public TimeCardLineServiceImpl(TimeCardLineRepository timeCardLineRepo) {
        this.timeCardLineRepo = timeCardLineRepo;
    }

    @Override
    public List<TimeCardLine> getScheduledTimeCardLine(@Nullable Project project, @Nullable Employee employee) {
        List<TimeCardLine> timeCardLines = new ArrayList<>();

        if (employee != null && project == null) {
            timeCardLines = timeCardLineRepo.findByEmployee(employee).fetch();
        } else if (employee == null && project != null) {
            timeCardLines = timeCardLineRepo.findByProject(project).fetch();
        } else if (employee != null && project != null) {
            timeCardLines = timeCardLineRepo.findByEmployeeAndProject(employee, project).fetch();
        }

        timeCardLines.removeIf(tcl -> tcl.getIsDeletable() || tcl.getTimeCard() != null);

         return timeCardLines;
    }
}
