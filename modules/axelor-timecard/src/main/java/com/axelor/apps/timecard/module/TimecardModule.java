/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.timecard.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.hr.service.batch.HrBatchService;
import com.axelor.apps.hr.service.leave.LeaveServiceImpl;
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
import com.axelor.apps.timecard.service.app.AppTimecardService;
import com.axelor.apps.timecard.service.app.AppTimecardServiceImpl;
import com.axelor.apps.timecard.service.batch.HrBatchTimeCardService;

public class TimecardModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(AppTimecardService.class).to(AppTimecardServiceImpl.class);
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
        bind(HrBatchService.class).to(HrBatchTimeCardService.class);
    }

}
