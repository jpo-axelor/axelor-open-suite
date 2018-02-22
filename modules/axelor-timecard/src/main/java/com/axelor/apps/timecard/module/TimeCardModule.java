package com.axelor.apps.timecard.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.timecard.service.FrequencyService;
import com.axelor.apps.timecard.service.FrequencyServiceImpl;
import com.axelor.apps.timecard.service.PlanningLineService;
import com.axelor.apps.timecard.service.PlanningLineServiceImpl;
import com.axelor.apps.timecard.service.TempTimeCardLineService;
import com.axelor.apps.timecard.service.TempTimeCardLineServiceImpl;
import com.axelor.apps.timecard.service.TimeCardService;
import com.axelor.apps.timecard.service.TimeCardServiceImpl;

public class TimeCardModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(FrequencyService.class).to(FrequencyServiceImpl.class);
        bind(PlanningLineService.class).to(PlanningLineServiceImpl.class);
        bind(TimeCardService.class).to(TimeCardServiceImpl.class);
        bind(TempTimeCardLineService.class).to(TempTimeCardLineServiceImpl.class);
    }

}
