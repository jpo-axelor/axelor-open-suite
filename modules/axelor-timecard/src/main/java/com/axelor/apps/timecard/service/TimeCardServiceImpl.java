package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.time.LocalDate;
import java.util.List;

public class TimeCardServiceImpl implements TimeCardService {

    protected TimeCardRepository timeCardRepo;
    protected TimeCardLineRepository timeCardLineRepo;
    protected PlanningLineRepository planningLineRepo;
    protected FrequencyService frequencyService;
    protected TimeCardLineService timeCardLineService;

    @Inject
    public TimeCardServiceImpl(TimeCardRepository timeCardRepo, TimeCardLineRepository timeCardLineRepo,
                               PlanningLineRepository planningLineRepo, FrequencyService frequencyService,
                               TimeCardLineService timeCardLineService) {
        this.timeCardRepo = timeCardRepo;
        this.timeCardLineRepo = timeCardLineRepo;
        this.planningLineRepo = planningLineRepo;
        this.frequencyService = frequencyService;
        this.timeCardLineService = timeCardLineService;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void generateTimeCardLines(TimeCard timeCard) {
        timeCardLineRepo.all().filter("self.timeCard.id = ? AND self.isDeletable = true", timeCard.getId()).delete();
        timeCardLineRepo.flush();

        LocalDate fromDate = timeCard.getFromDate();
        LocalDate toDate = timeCard.getToDate();

        List<PlanningLine> planningLines = planningLineRepo.findByEmployee(timeCard.getEmployee()).fetch();
        for (PlanningLine planningLine : planningLines) {
            Employee employee = planningLine.getEmployee();
            Project project = planningLine.getProject();

            List<LocalDate> dates = frequencyService.getDates(planningLine.getFrequency(), timeCard.getFromDate().getYear());
            for (LocalDate date : dates) {
                if (date.equals(fromDate) || date.isAfter(fromDate) && date.isBefore(toDate) || date.equals(toDate)) {
                    TimeCardLine timeCardLine = timeCardLineService.generateTimeCardLine(employee, project, date, planningLine.getStartTime(), planningLine.getEndTime(), TimeCardLineRepository.TYPE_CONTRACTUAL, true);
                    timeCardLineRepo.save(timeCardLine);
                    timeCard.addTimeCardLineListItem(timeCardLine);
                }
            }
        }

        timeCardRepo.save(timeCard);
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void attachScheduledTimeCardLines(TimeCard timeCard) {
        List<TimeCardLine> orphanTimeCardLines = timeCardLineRepo.all().filter("self.timeCard IS NULL AND self.isDeletable = false").fetch();

        LocalDate startDate = timeCard.getFromDate();
        LocalDate endDate = timeCard.getToDate();

        for (TimeCardLine orphanTimeCardLine : orphanTimeCardLines) {
            LocalDate date = orphanTimeCardLine.getDate();
            if (date.equals(startDate) || date.equals(endDate) || date.isAfter(startDate) && date.isBefore(endDate)) {
                timeCard.addTimeCardLineListItem(orphanTimeCardLine);
            }
        }

        timeCardRepo.save(timeCard);
    }
}
