package com.axelor.apps.timecard.service;

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.timecard.db.Frequency;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class PlanningLineServiceImpl implements PlanningLineService {

    protected PlanningLineRepository planningLineRepo;
    protected FrequencyService frequencyService;
    protected AppBaseService appBaseService;

    @Inject
    public PlanningLineServiceImpl(PlanningLineRepository planningLineRepo, FrequencyService frequencyService, AppBaseService appBaseService) {
        this.planningLineRepo = planningLineRepo;
        this.frequencyService = frequencyService;
        this.appBaseService = appBaseService;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void computeMonthlyWage(PlanningLine planningLine) {
        Frequency frequency = planningLine.getFrequency();

        List<LocalDate> dates = frequencyService.getDates(frequency);

        double lineDuration = Duration.between(planningLine.getStartTime(), planningLine.getEndTime()).toMinutes() / 60.0;
        planningLine.setMensualisation(BigDecimal.valueOf((dates.size() * lineDuration / 12)));
        planningLineRepo.save(planningLine);
    }
}
