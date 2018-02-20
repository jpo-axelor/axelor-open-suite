package com.axelor.apps.tn.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.tn.service.FrequencyService;
import com.axelor.apps.tn.service.FrequencyServiceImpl;

public class TraversierModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(FrequencyService.class).to(FrequencyServiceImpl.class);
    }

}
