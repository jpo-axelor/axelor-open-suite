package com.axelor.apps.timecard.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.timecard.service.FrequencyService;
import com.axelor.apps.timecard.service.FrequencyServiceImpl;

public class TimeCardModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(FrequencyService.class).to(FrequencyServiceImpl.class);
    }

}
