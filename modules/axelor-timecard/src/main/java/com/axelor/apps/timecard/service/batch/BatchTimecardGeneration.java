package com.axelor.apps.timecard.service.batch;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.HrBatch;
import com.axelor.apps.hr.service.batch.BatchStrategy;
import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.apps.timecard.db.repo.TimeCardRepository;
import com.axelor.apps.timecard.service.TimeCardService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.google.inject.persist.Transactional;

import java.time.LocalDate;
import java.util.Set;

public class BatchTimecardGeneration extends BatchStrategy {

    protected Set<Employee> employees;
    protected Company company;
    protected LocalDate startPeriod;
    protected LocalDate endPeriod;

    protected TimeCardRepository timeCardRepo;
    protected TimeCardService timeCardService;

    @Override
    protected void start() throws IllegalArgumentException, IllegalAccessException, AxelorException {
        super.start();

        timeCardRepo = Beans.get(TimeCardRepository.class);
        timeCardService = Beans.get(TimeCardService.class);

        HrBatch hrBatch = batch.getHrBatch();

        employees = hrBatch.getEmployeeSet();

        company = hrBatch.getCompany();
        if (company == null) {
            throw new AxelorException(batch, IException.MISSING_FIELD, "Le champ 'Société' est manquant.");
        }

        startPeriod = hrBatch.getPeriod().getFromDate();
        endPeriod = hrBatch.getPeriod().getToDate();
    }

    @Override
    protected void process() {
        for (Employee employee : employees) {
            try {
                generateTimeCard(employee);
                updateEmployee(employee);
            } catch (Exception e) {
                TraceBackService.trace(e, "Batch Timecard Generation", batch.getId()); // TODO
                incrementAnomaly();
            }
        }
    }

    @Override
    protected void stop() {
        String comment = String.format("Il y a eu %s pointages générés.\n", batch.getDone());

        if (batch.getAnomaly() > 0) {
            comment = String.format("Il y a eu %s anomalies et %s pointages générés.\n", batch.getAnomaly(), batch.getDone());
        }

        addComment(comment);

        super.stop();
    }

    /**
     * Generates (or updates) timecard for given {@code Employee}.
     *
     * @param employee
     * @return true if there is an anomaly, false otherwise
     */
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    protected void generateTimeCard(Employee employee) {
        TimeCard timeCard = timeCardRepo.all().filter("self.employee.id = ? AND self.fromDate = ? AND self.toDate = ?", employee.getId(), startPeriod, endPeriod).fetchOne();
        if (timeCard == null) {
            timeCard = new TimeCard();
            timeCard.setCompany(company);
            timeCard.setEmployee(employee);
            timeCard.setFromDate(startPeriod);
            timeCard.setToDate(endPeriod);
        }

        timeCardService.generateTimeCardLines(timeCard);
        timeCardService.attachScheduledTimeCardLines(timeCard);
    }
}
