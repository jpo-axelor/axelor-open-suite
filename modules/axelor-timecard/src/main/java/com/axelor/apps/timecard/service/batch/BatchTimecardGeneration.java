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
package com.axelor.apps.timecard.service.batch;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Period;
import com.axelor.apps.base.db.repo.CompanyRepository;
import com.axelor.apps.base.db.repo.PeriodRepository;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.HrBatch;
import com.axelor.apps.hr.service.batch.BatchStrategy;
import com.axelor.apps.timecard.db.Timecard;
import com.axelor.apps.timecard.db.repo.TimecardRepository;
import com.axelor.apps.timecard.service.TimecardService;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.util.Set;
import java.util.stream.Collectors;

public class BatchTimecardGeneration extends BatchStrategy {

  protected Set<Long> employeeIds;
  protected Long companyId;
  protected Long periodId;

  protected TimecardRepository timecardRepo;
  protected TimecardService timecardService;
  protected CompanyRepository companyRepo;
  protected PeriodRepository periodRepo;

  @Inject
  public BatchTimecardGeneration(
      TimecardRepository timecardRepo,
      TimecardService timecardService,
      CompanyRepository companyRepo,
      PeriodRepository periodRepo) {
    this.timecardRepo = timecardRepo;
    this.timecardService = timecardService;
    this.companyRepo = companyRepo;
    this.periodRepo = periodRepo;
  }

  @Override
  protected void start() throws IllegalArgumentException, IllegalAccessException, AxelorException {
    super.start();

    HrBatch hrBatch = batch.getHrBatch();

    employeeIds =
        hrBatch.getEmployeeSet().stream().map(Employee::getId).collect(Collectors.toSet());

    Company company = hrBatch.getCompany();
    if (company == null) {
      throw new AxelorException(
          batch, TraceBackRepository.CATEGORY_MISSING_FIELD, I18n.get("Company is not set."));
    }
    companyId = company.getId();

    periodId = hrBatch.getPeriod().getId();
  }

  @Override
  protected void process() {
    for (Long id : employeeIds) {
      Employee employee = employeeRepository.find(id);
      Company company = companyRepo.find(companyId);
      Period period = periodRepo.find(periodId);

      try {
        generateTimecard(employee, company, period);
        updateEmployee(employee);
      } catch (Exception e) {
        TraceBackService.trace(e, "Batch Timecard Generation", batch.getId());
        incrementAnomaly();
      } finally {
        JPA.clear();
      }
    }
  }

  @Override
  protected void stop() {
    String comment =
        String.format(I18n.get("There were %s timecards generated.\n"), batch.getDone());

    if (batch.getAnomaly() > 0) {
      comment =
          String.format(
              I18n.get("There were %s anomalies and %s timecards generated.\n"),
              batch.getAnomaly(),
              batch.getDone());
    }

    addComment(comment);

    super.stop();
  }

  /** Generates (or updates) timecard for given {@code Employee}. */
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  protected void generateTimecard(Employee employee, Company company, Period period) {
    Timecard timecard =
        timecardRepo
            .all()
            .filter(
                "self.employee.id = ? AND self.period.id = ?",
                employee.getId(),
                periodId)
            .fetchOne();

    if (timecard == null) {
      timecard = new Timecard();
      timecard.setCompany(company);
      timecard.setEmployee(employee);
      timecard.setPeriod(period);
      timecard.setFromDate(period.getFromDate());
      timecard.setToDate(period.getToDate());
    }

    timecardService.generateTimecardLines(timecard);
    timecardService.attachScheduledTimecardLines(timecard);
    try {
      Beans.get(TimecardService.class).computeHours(timecard);
    } catch (AxelorException e) {
      TraceBackService.trace(e, "Timecard");
    }
    timecardService.computeWeeklyHours(timecard);
  }
}
