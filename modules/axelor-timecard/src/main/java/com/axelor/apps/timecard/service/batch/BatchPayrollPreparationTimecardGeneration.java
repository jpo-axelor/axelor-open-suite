package com.axelor.apps.timecard.service.batch;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Period;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.PayrollPreparation;
import com.axelor.apps.hr.exception.IExceptionMessage;
import com.axelor.apps.hr.service.PayrollPreparationService;
import com.axelor.apps.hr.service.batch.BatchPayrollPreparationGeneration;
import com.axelor.apps.timecard.service.PayrollPreparationTimecardServiceImpl;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import java.util.List;

public class BatchPayrollPreparationTimecardGeneration extends BatchPayrollPreparationGeneration {

  protected PayrollPreparationTimecardServiceImpl payrollPreparationTimecardService;

  @Inject
  public BatchPayrollPreparationTimecardGeneration(
      PayrollPreparationService payrollPreparationService,
      PayrollPreparationTimecardServiceImpl payrollPreparationTimecardService) {
    super(payrollPreparationService);
    this.payrollPreparationTimecardService = payrollPreparationTimecardService;
  }

  @Override
  public void createPayrollPreparation(Employee employee) throws AxelorException {
    List<PayrollPreparation> payrollPreparationList =
        payrollPreparationRepository
            .all()
            .filter(
                "self.period = ?1 AND self.employee = ?2 AND self.company = ?3",
                hrBatch.getPeriod(),
                employee,
                company)
            .fetch();
    log.debug("list : " + payrollPreparationList);
    if (!payrollPreparationList.isEmpty()) {
      throw new AxelorException(
          employee,
          TraceBackRepository.CATEGORY_NO_UNIQUE_KEY,
          I18n.get(IExceptionMessage.PAYROLL_PREPARATION_DUPLICATE),
          employee.getName(),
          hrBatch.getCompany().getName(),
          hrBatch.getPeriod().getName());
    }
    Company currentCompany = companyRepository.find(company.getId());
    Period period = periodRepository.find(hrBatch.getPeriod().getId());

    PayrollPreparation payrollPreparation = new PayrollPreparation();

    payrollPreparation.setCompany(currentCompany);
    payrollPreparation.setEmployee(employee);
    payrollPreparation.setEmploymentContract(employee.getMainEmploymentContract());
    payrollPreparation.setPeriod(period);

    payrollPreparationTimecardService.calculate(payrollPreparation);
    updateEmployee(employee);
  }
}
