package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.TempTimeCardLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.db.repo.TempTimeCardLineRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TempTimeCardLineServiceImpl implements TempTimeCardLineService {

    protected TempTimeCardLineRepository tempTimeCardLineRepo;
    protected PlanningLineRepository planningLineRepo;
    protected FrequencyService frequencyService;

    @Inject
    public TempTimeCardLineServiceImpl(TempTimeCardLineRepository tempTimeCardLineRepo, PlanningLineRepository planningLineRepo, FrequencyService frequencyService) {
        this.tempTimeCardLineRepo = tempTimeCardLineRepo;
        this.planningLineRepo = planningLineRepo;
        this.frequencyService = frequencyService;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void invalidateTempTimeCardLines() {
        tempTimeCardLineRepo.all().delete();
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public boolean generateTempTimeCardLines(Project project, Employee employee, LocalDate startDate, LocalDate endDate) {
        invalidateTempTimeCardLines();

        // Get planning lines
        List<PlanningLine> planningLines = new ArrayList<PlanningLine>();
        if (employee != null && project == null) {
            planningLines = planningLineRepo.findByEmployee(employee).fetch();
        } else if (employee == null && project != null) {
            planningLines = planningLineRepo.findByProject(project).fetch();
        } else if (employee != null && project != null) {
            planningLines = planningLineRepo.findByEmployeeAndProject(employee, project).fetch();
        }

        // Generate temp time card lines
        for (PlanningLine planningLine : planningLines) {
            List<LocalDate> dates = frequencyService.getDates(planningLine.getFrequency());
            for (LocalDate date : dates) {
                if (date.isAfter(startDate) && date.isBefore(endDate)) {
                    TempTimeCardLine tempTimeCardLine = new TempTimeCardLine();
                    tempTimeCardLine.setEmployee(planningLine.getEmployee());
                    tempTimeCardLine.setProject(planningLine.getProject());
                    tempTimeCardLine.setStartDateTime(LocalDateTime.of(date, planningLine.getStartTime()));
                    tempTimeCardLine.setEndDateTime(LocalDateTime.of(date, planningLine.getEndTime()));

                    tempTimeCardLineRepo.save(tempTimeCardLine);
                }
            }
        }

        return planningLines.size() > 0;
    }

}
