package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.TempTimeCardLine;
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
    protected PlanningLineService planningLineService;
    protected FrequencyService frequencyService;

    @Inject
    public TempTimeCardLineServiceImpl(TempTimeCardLineRepository tempTimeCardLineRepo, PlanningLineService planningLineService, FrequencyService frequencyService) {
        this.tempTimeCardLineRepo = tempTimeCardLineRepo;
        this.planningLineService = planningLineService;
        this.frequencyService = frequencyService;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void invalidateTempTimeCardLines() {
        tempTimeCardLineRepo.all().delete();
        tempTimeCardLineRepo.flush();
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public List<TempTimeCardLine> generateTempTimeCardLines(Project project, Employee employee, LocalDate startDate, LocalDate endDate) {
        invalidateTempTimeCardLines();

        List<TempTimeCardLine> tempTimeCardLines = new ArrayList<>();

        // Get planning lines
        List<PlanningLine> planningLines = planningLineService.getPlanningLines(project, employee);

        // Generate temp time card lines
        for (PlanningLine planningLine : planningLines) {
            List<LocalDate> dates = frequencyService.getDates(planningLine.getFrequency());
            for (LocalDate date : dates) {
                if (date.equals(startDate) || date.equals(endDate) || date.isAfter(startDate) && date.isBefore(endDate)) {
                    TempTimeCardLine tempTimeCardLine = new TempTimeCardLine();
                    tempTimeCardLine.setEmployee(planningLine.getEmployee());
                    tempTimeCardLine.setProject(planningLine.getProject());
                    tempTimeCardLine.setStartDateTime(LocalDateTime.of(date, planningLine.getStartTime()));
                    tempTimeCardLine.setEndDateTime(LocalDateTime.of(date, planningLine.getEndTime()));

                    tempTimeCardLines.add(tempTimeCardLine);
                    tempTimeCardLineRepo.save(tempTimeCardLine);
                }
            }
        }

        return tempTimeCardLines;
    }

}
