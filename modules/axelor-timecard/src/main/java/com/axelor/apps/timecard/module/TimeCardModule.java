package com.axelor.apps.timecard.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.hr.db.repo.LeaveRequestRepository;
import com.axelor.apps.hr.service.leave.LeaveServiceImpl;
import com.axelor.apps.timecard.db.repo.LeaveRequestTimeCardRepository;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.db.repo.PlanningLineTimeCardRepository;
import com.axelor.apps.timecard.db.repo.TempTimeCardLineRepository;
import com.axelor.apps.timecard.db.repo.TempTimeCardLineTimeCardRepository;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardLineTimeCardRepository;
import com.axelor.apps.timecard.db.repo.TimeCardRepository;
import com.axelor.apps.timecard.db.repo.TimeCardTimeCardRepository;
import com.axelor.apps.timecard.service.FrequencyService;
import com.axelor.apps.timecard.service.FrequencyServiceImpl;
import com.axelor.apps.timecard.service.LeaveServiceTimeCardImpl;
import com.axelor.apps.timecard.service.PlanningLineService;
import com.axelor.apps.timecard.service.PlanningLineServiceImpl;
import com.axelor.apps.timecard.service.TempTimeCardLineService;
import com.axelor.apps.timecard.service.TempTimeCardLineServiceImpl;
import com.axelor.apps.timecard.service.TimeCardLineService;
import com.axelor.apps.timecard.service.TimeCardLineServiceImpl;
import com.axelor.apps.timecard.service.TimeCardService;
import com.axelor.apps.timecard.service.TimeCardServiceImpl;

public class TimeCardModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(PlanningLineRepository.class).to(PlanningLineTimeCardRepository.class);
        bind(FrequencyService.class).to(FrequencyServiceImpl.class);
        bind(PlanningLineService.class).to(PlanningLineServiceImpl.class);
        bind(TimeCardService.class).to(TimeCardServiceImpl.class);
        bind(TimeCardRepository.class).to(TimeCardTimeCardRepository.class);
        bind(TimeCardLineService.class).to(TimeCardLineServiceImpl.class);
        bind(TimeCardLineRepository.class).to(TimeCardLineTimeCardRepository.class);
        bind(TempTimeCardLineRepository.class).to(TempTimeCardLineTimeCardRepository.class);
        bind(TempTimeCardLineService.class).to(TempTimeCardLineServiceImpl.class);
        bind(LeaveServiceImpl.class).to(LeaveServiceTimeCardImpl.class);
        bind(LeaveRequestRepository.class).to(LeaveRequestTimeCardRepository.class);
    }

}
