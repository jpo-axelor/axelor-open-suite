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
package com.axelor.apps.timecard.service;

import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.account.service.PeriodServiceAccountImpl;
import com.axelor.apps.account.service.move.MoveValidateService;
import com.axelor.apps.base.db.Period;
import com.axelor.apps.base.db.repo.PeriodRepository;
import com.axelor.apps.base.service.AdjustHistoryService;
import com.axelor.apps.hr.db.PayrollPreparation;
import com.axelor.apps.hr.db.repo.PayrollPreparationRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.util.List;

public class PeriodTimecardServiceImpl extends PeriodServiceAccountImpl {

  protected final PayrollPreparationRepository payrollPreparationRepo;
  protected final PayrollPreparationTimecardServiceImpl payrollPreparationService;

  @Inject
  public PeriodTimecardServiceImpl(
      PeriodRepository periodRepo,
      AdjustHistoryService adjustHistoryService,
      MoveValidateService moveValidateService,
      MoveRepository moveRepository,
      PayrollPreparationRepository payrollPreparationRepo,
      PayrollPreparationTimecardServiceImpl payrollPreparationService) {
    super(periodRepo, adjustHistoryService, moveValidateService, moveRepository);
    this.payrollPreparationRepo = payrollPreparationRepo;
    this.payrollPreparationService = payrollPreparationService;
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, RuntimeException.class})
  public void close(Period period) {
    super.close(period);

    List<PayrollPreparation> payrollPreparations =
        payrollPreparationRepo.all().filter("self.period.id = ?", period.getId()).fetch();

    for (PayrollPreparation payrollPreparation : payrollPreparations) {
      payrollPreparationService.close(payrollPreparation);
      payrollPreparationRepo.save(payrollPreparation);
    }
  }
}
