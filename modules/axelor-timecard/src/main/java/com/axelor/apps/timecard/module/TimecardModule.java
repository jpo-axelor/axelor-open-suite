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
import com.axelor.apps.base.service.PeriodServiceImpl;
import com.axelor.apps.hr.service.PayrollPreparationService;
import com.axelor.apps.hr.service.batch.BatchPayrollPreparationGeneration;
import com.axelor.apps.hr.service.batch.HrBatchService;
import com.axelor.apps.hr.service.leave.LeaveServiceImpl;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.db.repo.PlanningLineTimecardRepository;
import com.axelor.apps.timecard.db.repo.TempTimecardLineRepository;
import com.axelor.apps.timecard.db.repo.TempTimecardLineTimecardRepository;
import com.axelor.apps.timecard.db.repo.TimecardLineRepository;
import com.axelor.apps.timecard.db.repo.TimecardLineTimecardRepository;
import com.axelor.apps.timecard.db.repo.TimecardRepository;
import com.axelor.apps.timecard.db.repo.TimecardTimecardRepository;
import com.axelor.apps.timecard.service.FrequencyService;
import com.axelor.apps.timecard.service.FrequencyServiceImpl;
import com.axelor.apps.timecard.service.LeaveServiceTimecardImpl;
import com.axelor.apps.timecard.service.PayrollPreparationTimecardServiceImpl;
import com.axelor.apps.timecard.service.PeriodTimecardServiceImpl;
import com.axelor.apps.timecard.service.PlanningLineService;
import com.axelor.apps.timecard.service.PlanningLineServiceImpl;
import com.axelor.apps.timecard.service.TempTimecardLineService;
import com.axelor.apps.timecard.service.TempTimecardLineServiceImpl;
import com.axelor.apps.timecard.service.TimecardLineService;
import com.axelor.apps.timecard.service.TimecardLineServiceImpl;
import com.axelor.apps.timecard.service.TimecardService;
import com.axelor.apps.timecard.service.TimecardServiceImpl;
import com.axelor.apps.timecard.service.app.AppTimecardService;
import com.axelor.apps.timecard.service.app.AppTimecardServiceImpl;
import com.axelor.apps.timecard.service.batch.BatchPayrollPreparationTimecardGeneration;
import com.axelor.apps.timecard.service.batch.HrBatchTimecardService;

public class TimecardModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(AppTimecardService.class).to(AppTimecardServiceImpl.class);
    bind(PlanningLineRepository.class).to(PlanningLineTimecardRepository.class);
    bind(FrequencyService.class).to(FrequencyServiceImpl.class);
    bind(PlanningLineService.class).to(PlanningLineServiceImpl.class);
    bind(TimecardService.class).to(TimecardServiceImpl.class);
    bind(TimecardRepository.class).to(TimecardTimecardRepository.class);
    bind(TimecardLineService.class).to(TimecardLineServiceImpl.class);
    bind(TimecardLineRepository.class).to(TimecardLineTimecardRepository.class);
    bind(TempTimecardLineRepository.class).to(TempTimecardLineTimecardRepository.class);
    bind(TempTimecardLineService.class).to(TempTimecardLineServiceImpl.class);
    bind(LeaveServiceImpl.class).to(LeaveServiceTimecardImpl.class);
    bind(HrBatchService.class).to(HrBatchTimecardService.class);
    bind(PayrollPreparationService.class).to(PayrollPreparationTimecardServiceImpl.class);
    bind(PeriodServiceImpl.class).to(PeriodTimecardServiceImpl.class);
    bind(BatchPayrollPreparationGeneration.class)
        .to(BatchPayrollPreparationTimecardGeneration.class);
  }
}
