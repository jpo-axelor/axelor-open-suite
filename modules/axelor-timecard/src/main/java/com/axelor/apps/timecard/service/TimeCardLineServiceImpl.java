package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

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

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void generateExtraTCL(Employee oldEmployee, Employee newEmployee, List<Project> projects, LocalDate startDate, LocalDate endDate, boolean isContractual) {
        List<TimeCardLine> timeCardLines = timeCardLineRepo.all().filter("employee = ? AND date >= ? AND date <= ? AND typeSelect = ?", oldEmployee, startDate, endDate, TimeCardLineRepository.TYPE_ABSENCE).fetch();
        for (TimeCardLine timeCardLine : timeCardLines) {
            TimeCardLine tcl = generateTimeCardLine(newEmployee,
                                                    timeCardLine.getProject(),
                                                    timeCardLine.getDate(),
                                                    timeCardLine.getStartTime(),
                                                    timeCardLine.getEndTime(),
                                                    TimeCardLineRepository.TYPE_EXTRA,
                                                    false);

            tcl.setIsSubstitution(true);
            tcl.setIsContractual(isContractual);

            timeCardLine.addSubstitutionTimeCardLineListItem(tcl);
            timeCardLineRepo.save(tcl);
            timeCardLineRepo.save(timeCardLine);
        }
    }
}
