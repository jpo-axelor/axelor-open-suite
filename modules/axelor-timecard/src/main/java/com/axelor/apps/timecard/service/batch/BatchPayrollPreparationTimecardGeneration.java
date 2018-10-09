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
import com.axelor.apps.base.db.WeeklyPlanning;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.HrBatch;
import com.axelor.apps.hr.db.PayrollPreparation;
import com.axelor.apps.hr.exception.IExceptionMessage;
import com.axelor.apps.hr.service.PayrollPreparationService;
import com.axelor.apps.hr.service.batch.BatchPayrollPreparationGeneration;
import com.axelor.apps.timecard.service.PayrollPreparationTimecardServiceImpl;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
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

  // Fast bugfix of ABS issue. Delete this code once the ABS version used by the project includes
  // PR#2323 (RM#13588)
  @Override
  public List<Employee> getEmployees(HrBatch hrBatch) {

    List<String> query = Lists.newArrayList();

    if (!hrBatch.getEmployeeSet().isEmpty()) {
      String employeeIds =
          Joiner.on(',')
              .join(
                  Iterables.transform(
                      hrBatch.getEmployeeSet(),
                      new Function<Employee, String>() {
                        public String apply(Employee obj) {
                          return obj.getId().toString();
                        }
                      }));
      query.add("self.id IN (" + employeeIds + ")");
    }
    if (!hrBatch.getPlanningSet().isEmpty()) {
      String planningIds =
          Joiner.on(',')
              .join(
                  Iterables.transform(
                      hrBatch.getPlanningSet(),
                      new Function<WeeklyPlanning, String>() {
                        public String apply(WeeklyPlanning obj) {
                          return obj.getId().toString();
                        }
                      }));

      query.add("self.weeklyPlanning.id IN (" + planningIds + ")");
    }

    List<Employee> employeeList = Lists.newArrayList();
    String liaison = query.isEmpty() ? "" : " AND";
    if (hrBatch.getCompany() != null) {
      employeeList =
          JPA.all(Employee.class)
              .filter(
                  Joiner.on(" AND ").join(query)
                      + liaison
                      + " self.mainEmploymentContract.payCompany = :company")
              .bind("company", hrBatch.getCompany())
              .fetch();
    } else {
      employeeList = JPA.all(Employee.class).filter(Joiner.on(" AND ").join(query)).fetch();
    }

    return employeeList;
  }
}
