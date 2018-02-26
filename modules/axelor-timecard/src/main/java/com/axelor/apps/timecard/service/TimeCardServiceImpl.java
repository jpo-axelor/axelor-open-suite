package com.axelor.apps.timecard.service;

import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TimeCardServiceImpl implements TimeCardService {

    protected TimeCardRepository timeCardRepo;
    protected PlanningLineRepository planningLineRepo;
    protected FrequencyService frequencyService;

    @Inject
    public TimeCardServiceImpl(TimeCardRepository timeCardRepo, PlanningLineRepository planningLineRepo, FrequencyService frequencyService) {
        this.timeCardRepo = timeCardRepo;
        this.planningLineRepo = planningLineRepo;
        this.frequencyService = frequencyService;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void generateTimeCardLines(TimeCard timeCard) {
        timeCard.clearTimeCardLineList();
        timeCardRepo.flush();

        LocalDate fromDate = timeCard.getFromDate();
        LocalDate toDate = timeCard.getToDate();

        List<PlanningLine> planningLines = planningLineRepo.findByEmployee(timeCard.getEmployee()).fetch();
        for (PlanningLine planningLine : planningLines) {
            List<LocalDate> dates = frequencyService.getDates(planningLine.getFrequency());
            for (LocalDate date : dates) {
                if (date.equals(fromDate) || date.equals(toDate) || date.isAfter(fromDate) && date.isBefore(toDate)) {
                    TimeCardLine timeCardLine = new TimeCardLine();
                    timeCardLine.setEmployee(planningLine.getEmployee());
                    timeCardLine.setProject(planningLine.getProject());
                    timeCardLine.setWeekDay(date.getDayOfWeek().getValue());


                    timeCardLine.setDate(date);
                    timeCardLine.setStartTime(planningLine.getStartTime());
                    timeCardLine.setEndTime(planningLine.getEndTime());

                    timeCardLine.setStartDateTime(LocalDateTime.of(date, planningLine.getStartTime()));
                    timeCardLine.setEndDateTime(LocalDateTime.of(date, planningLine.getEndTime()));


                    timeCardLine.setDuration(BigDecimal.valueOf(Duration.between(planningLine.getStartTime(), planningLine.getEndTime()).toMinutes() / 60.0));
                    timeCardLine.setTypeSelect(TimeCardLineRepository.TYPE_CONTRACTUAL);

                    timeCard.addTimeCardLineListItem(timeCardLine);
                }
            }
        }

        timeCardRepo.save(timeCard);
    }
}
