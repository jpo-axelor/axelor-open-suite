package com.axelor.apps.timecard.service.batch;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.repo.CompanyRepository;
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
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

public class BatchTimecardGeneration extends BatchStrategy {

  protected Set<Long> employeeIds;
  protected Long companyId;
  protected LocalDate startPeriod;
  protected LocalDate endPeriod;

  protected TimecardRepository timecardRepo;
  protected TimecardService timecardService;
  protected CompanyRepository companyRepo;

  @Inject
  public BatchTimecardGeneration(
      TimecardRepository timecardRepo,
      TimecardService timecardService,
      CompanyRepository companyRepo) {
    this.timecardRepo = timecardRepo;
    this.timecardService = timecardService;
    this.companyRepo = companyRepo;
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
          batch, TraceBackRepository.CATEGORY_MISSING_FIELD, "Le champ 'Société' est manquant.");
    }
    companyId = company.getId();

    startPeriod = hrBatch.getPeriod().getFromDate();
    endPeriod = hrBatch.getPeriod().getToDate();
  }

  @Override
  protected void process() {
    for (Long id : employeeIds) {
      Employee employee = employeeRepository.find(id);
      Company company = companyRepo.find(companyId);
      try {
        generateTimecard(employee, company);
        updateEmployee(employee);
      } catch (Exception e) {
        TraceBackService.trace(e, "Batch Timecard Generation", batch.getId()); // TODO
        incrementAnomaly();
      } finally {
        JPA.clear();
      }
    }
  }

  @Override
  protected void stop() {
    String comment = String.format("Il y a eu %s pointages générés.\n", batch.getDone());

    if (batch.getAnomaly() > 0) {
      comment =
          String.format(
              "Il y a eu %s anomalies et %s pointages générés.\n",
              batch.getAnomaly(), batch.getDone());
    }

    addComment(comment);

    super.stop();
  }

  /**
   * Generates (or updates) timecard for given {@code Employee}.
   *
   * @param employee
   * @param company
   */
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  protected void generateTimecard(Employee employee, Company company) {
    Timecard timecard =
        timecardRepo
            .all()
            .filter(
                "self.employee.id = ? AND self.fromDate = ? AND self.toDate = ?",
                employee.getId(),
                startPeriod,
                endPeriod)
            .fetchOne();
    if (timecard == null) {
      timecard = new Timecard();
      timecard.setCompany(company);
      timecard.setEmployee(employee);
      timecard.setFromDate(startPeriod);
      timecard.setToDate(endPeriod);
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
