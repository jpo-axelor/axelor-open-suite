package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.google.inject.Inject;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TimeCardLineServiceImpl implements TimeCardLineService {

    protected TimeCardLineRepository timeCardLineRepo;

    @Inject
    public TimeCardLineServiceImpl(TimeCardLineRepository timeCardLineRepo) {
        this.timeCardLineRepo = timeCardLineRepo;
    }

    @Override
    public TimeCardLine generateTimeCardLine(Employee employee, Project project, LocalDate date, LocalTime startTime, LocalTime endTime, String lineType, boolean isDeletable) {
        TimeCardLine timeCardLine = new TimeCardLine();
        timeCardLine.setIsDeletable(isDeletable);

        timeCardLine.setEmployee(employee);
        timeCardLine.setProject(project);
        timeCardLine.setWeekDay(date.getDayOfWeek().getValue());

        timeCardLine.setDate(date);
        timeCardLine.setStartTime(startTime);
        timeCardLine.setEndTime(endTime);

        timeCardLine.setTypeSelect(lineType);

        return timeCardLine;
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
