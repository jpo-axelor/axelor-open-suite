package com.axelor.apps.timecard.service;

import com.axelor.apps.base.db.Period;
import com.axelor.apps.base.db.repo.PeriodRepository;
import com.axelor.apps.base.service.AdjustHistoryService;
import com.axelor.apps.base.service.PeriodServiceImpl;
import com.axelor.apps.hr.db.PayrollPreparation;
import com.axelor.apps.hr.db.repo.PayrollPreparationRepository;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.util.List;

public class PeriodTimecardServiceImpl extends PeriodServiceImpl {

  protected PayrollPreparationRepository payrollPreparationRepo;
  protected PayrollPreparationTimecardServiceImpl payrollPreparationService;

  @Inject
  public PeriodTimecardServiceImpl(
      PeriodRepository periodRepo,
      AdjustHistoryService adjustHistoryService,
      PayrollPreparationRepository payrollPreparationRepo,
      PayrollPreparationTimecardServiceImpl payrollPreparationService) {
    super(periodRepo, adjustHistoryService);
    this.payrollPreparationRepo = payrollPreparationRepo;
    this.payrollPreparationService = payrollPreparationService;
  }

  @Override
  @Transactional
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
