package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.EmploymentContract;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardRepository;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TimeCardServiceImpl implements TimeCardService {

    protected TimeCardRepository timeCardRepo;
    protected TimeCardLineRepository timeCardLineRepo;
    protected PlanningLineRepository planningLineRepo;
    protected FrequencyService frequencyService;
    protected TimeCardLineService timeCardLineService;

    @Inject
    public TimeCardServiceImpl(TimeCardRepository timeCardRepo, TimeCardLineRepository timeCardLineRepo,
                               PlanningLineRepository planningLineRepo, FrequencyService frequencyService,
                               TimeCardLineService timeCardLineService) {
        this.timeCardRepo = timeCardRepo;
        this.timeCardLineRepo = timeCardLineRepo;
        this.planningLineRepo = planningLineRepo;
        this.frequencyService = frequencyService;
        this.timeCardLineService = timeCardLineService;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void generateTimeCardLines(TimeCard timeCard) {
        timeCardLineRepo.all().filter("self.timeCard.id = ? AND self.isDeletable = true", timeCard.getId()).delete();
        timeCardLineRepo.flush();

        LocalDate fromDate = timeCard.getFromDate();
        LocalDate toDate = timeCard.getToDate();

        List<PlanningLine> planningLines = planningLineRepo.findByEmployee(timeCard.getEmployee()).fetch();
        for (PlanningLine planningLine : planningLines) {
            Employee employee = planningLine.getEmployee();
            Project project = planningLine.getProject();

            List<LocalDate> dates = frequencyService.getDates(planningLine.getFrequency(), timeCard.getFromDate().getYear());
            for (LocalDate date : dates) {
                if (date.equals(fromDate) || date.isAfter(fromDate) && date.isBefore(toDate) || date.equals(toDate)) {
                    TimeCardLine timeCardLine = timeCardLineService.generateTimeCardLine(employee, project, date, planningLine.getStartTime(), planningLine.getEndTime(), TimeCardLineRepository.TYPE_CONTRACTUAL, true);
                    timeCardLineRepo.save(timeCardLine);
                    timeCard.addTimeCardLineListItem(timeCardLine);
                }
            }
        }

        timeCardRepo.save(timeCard);
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void attachScheduledTimeCardLines(TimeCard timeCard) {
        List<TimeCardLine> orphanTimeCardLines = timeCardLineRepo.all().filter("self.timeCard IS NULL AND self.isDeletable = false").fetch();

        LocalDate startDate = timeCard.getFromDate();
        LocalDate endDate = timeCard.getToDate();

        for (TimeCardLine orphanTimeCardLine : orphanTimeCardLines) {
            LocalDate date = orphanTimeCardLine.getDate();
            if (date.equals(startDate) || date.equals(endDate) || date.isAfter(startDate) && date.isBefore(endDate)) {
                timeCard.addTimeCardLineListItem(orphanTimeCardLine);
            }
        }

        timeCardRepo.save(timeCard);
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void computeHours(TimeCard timeCard) throws AxelorException {
        /* * *
         * 1) Get weeks of current month
         *
         * Get all the weeks of the month if Sunday is in it.
         *
         * e.g. February 2018
         *   mo tu we th fr sa su   wk
         *             1  2  3  4    5
         *    5  6  7  8  9 10 11    6
         *   12 13 14 15 16 17 18    7
         *   19 20 21 22 23 24 25    8
         *   26 27 28                9
         *
         * Only weeks n° 5, 6, 7 and 8 will be taken into account.
         * * */
        List<Integer> weeks = this.getWeeks(timeCard.getFromDate(), timeCard.getToDate());


        /* * *
         * 2) For each week,
         *      compute supplementary hours
         *      add it to a global variable
         *
         * Formula : suppHours = contractualHoursWeek + extraHoursWeek - notPaidLeavesHoursWeek - 35
         *
         * NB : if 'suppHours' is negative return 0.
         * * */
        BigDecimal sumSupplementaryHours = BigDecimal.ZERO;
        for (Integer week : weeks) {
            sumSupplementaryHours = sumSupplementaryHours.add(this.computeSupplementaryHours(timeCard, week));
        }


        /* * *
         * 3) For the period of the TimeCard,
         *      compute complement of hours
         *
         * Formula : complOfHours = extraHoursPeriod - notPaidLeavesHoursPeriod - suppHours - (10% of monthlyWage)
         *
         * NB : if 'complOfHours' is negative return 0.
         * NB : 'monthlyWage' is found in the employee's main employment contract.
         * * */
        BigDecimal complementOfHours = this.computeComplementOfHours(timeCard, sumSupplementaryHours);


        /* * *
         * 4) For the period of the TimeCard,
         *      compute complementary hours
         *
         * Formula : complHours = extraHoursPeriod - notPaidLeavesHoursPeriod - suppHours - complOfHours
         *
         * NB : complHours CAN be negative.
         * * */
        BigDecimal complementaryHours = this.computeComplementaryHours(timeCard, sumSupplementaryHours, complementOfHours);


        timeCard.setSupplementaryHours(sumSupplementaryHours);
        timeCard.setComplementOfHours(complementOfHours);
        timeCard.setComplementaryHours(complementaryHours);

        timeCardRepo.save(timeCard);
    }

    protected List<Integer> getWeeks(LocalDate startDate, LocalDate endDate) {
        LocalDate currentSunday = startDate;
        if (startDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
            currentSunday = startDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        }

        LocalDate lastSundayOfMonth = endDate;
        if (endDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
            lastSundayOfMonth = endDate.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        }

        List<Integer> weeks = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        while (currentSunday.isBefore(lastSundayOfMonth) || currentSunday.equals(lastSundayOfMonth)) {
            cal.set(currentSunday.getYear(), currentSunday.getMonthValue() - 1, currentSunday.getDayOfMonth());
            weeks.add(cal.get(Calendar.WEEK_OF_YEAR));
            currentSunday = currentSunday.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        }

        return weeks;
    }

    protected BigDecimal computeSupplementaryHours(TimeCard timeCard, int weekOfYear) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, timeCard.getToDate().getYear());
        cal.set(Calendar.WEEK_OF_YEAR, weekOfYear);

        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        LocalDate startDate = new Date(cal.getTime().getTime()).toLocalDate();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        LocalDate endDate = new Date(cal.getTime().getTime()).toLocalDate();

        BigDecimal totalContractual = timeCardLineService.getTotalContractualHours(timeCard.getEmployee(), startDate, endDate);
        BigDecimal totalExtra = timeCardLineService.getTotalExtraHours(timeCard.getEmployee(), startDate, endDate);
        BigDecimal totalNotPaidLeaves = timeCardLineService.getTotalNotPaidLeavesHours(timeCard.getEmployee(), startDate, endDate);

        BigDecimal total = totalContractual.add(totalExtra).subtract(totalNotPaidLeaves).subtract(new BigDecimal(35));

        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }

    protected BigDecimal computeComplementOfHours(TimeCard timeCard, BigDecimal suppHours) throws AxelorException {
        BigDecimal totalExtra = timeCardLineService.getTotalExtraHours(timeCard.getEmployee(), timeCard.getFromDate(), timeCard.getToDate());
        BigDecimal totalNotPaidLeaves = timeCardLineService.getTotalNotPaidLeavesHours(timeCard.getEmployee(), timeCard.getFromDate(), timeCard.getToDate());

        EmploymentContract employmentContract = timeCard.getEmployee().getMainEmploymentContract();
        if (employmentContract == null) {
            throw new AxelorException(timeCard.getEmployee(), IException.MISSING_FIELD, "Veuillez configurer un contrat principal sur l'employé(e) %s", timeCard.getEmployee().getName()); // TODO: translation
        }
        BigDecimal monthlyWage = employmentContract.getMonthlyWage();
        if (monthlyWage.compareTo(BigDecimal.ZERO) == 0) {
            throw new AxelorException(employmentContract, IException.MISSING_FIELD, "Veuillez ajouter une mensualisation sur le contrat principal de l'employé(e) %s", timeCard.getEmployee().getName()); // TODO: translation
        }
        BigDecimal tenPercentMonthlyWage = monthlyWage.multiply(BigDecimal.valueOf(0.1));

        BigDecimal total = totalExtra.subtract(totalNotPaidLeaves).subtract(suppHours).subtract(tenPercentMonthlyWage);

        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }

    protected BigDecimal computeComplementaryHours(TimeCard timeCard, BigDecimal suppHours, BigDecimal complHours) {
        BigDecimal totalExtra = timeCardLineService.getTotalExtraHours(timeCard.getEmployee(), timeCard.getFromDate(), timeCard.getToDate());
        BigDecimal totalNotPaidLeaves = timeCardLineService.getTotalNotPaidLeavesHours(timeCard.getEmployee(), timeCard.getFromDate(), timeCard.getToDate());

        return totalExtra.subtract(totalNotPaidLeaves).subtract(suppHours).subtract(complHours);
    }

}
