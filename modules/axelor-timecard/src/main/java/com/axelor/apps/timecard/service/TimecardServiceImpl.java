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

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.EmploymentContract;
import com.axelor.apps.hr.db.LeaveLine;
import com.axelor.apps.hr.db.LeaveRequest;
import com.axelor.apps.hr.db.repo.LeaveLineRepository;
import com.axelor.apps.hr.db.repo.LeaveRequestRepository;
import com.axelor.apps.hr.exception.IExceptionMessage;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.Timecard;
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.apps.timecard.db.WeeklyHours;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.db.repo.TimecardLineRepository;
import com.axelor.apps.timecard.db.repo.TimecardRepository;
import com.axelor.auth.AuthUtils;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class TimecardServiceImpl implements TimecardService {

  protected TimecardRepository timecardRepo;
  protected TimecardLineRepository timecardLineRepo;
  protected PlanningLineRepository planningLineRepo;
  protected FrequencyService frequencyService;
  protected TimecardLineService timecardLineService;

  @Inject
  public TimecardServiceImpl(
      TimecardRepository timecardRepo,
      TimecardLineRepository timecardLineRepo,
      PlanningLineRepository planningLineRepo,
      FrequencyService frequencyService,
      TimecardLineService timecardLineService) {
    this.timecardRepo = timecardRepo;
    this.timecardLineRepo = timecardLineRepo;
    this.planningLineRepo = planningLineRepo;
    this.frequencyService = frequencyService;
    this.timecardLineService = timecardLineService;
  }

  @Override
  @Transactional
  public void close(Timecard timecard) {
    timecard.setStatusSelect(TimecardRepository.STATUS_CLOSED);
    timecardRepo.save(timecard);
  }

  @Override
  @Transactional
  public void detachAbsenceTimecardLines(Timecard timecard) {
    List<TimecardLine> absenceTLs =
        timecard
            .getTimecardLineList()
            .stream()
            .filter(tl -> tl.getTypeSelect().equals(TimecardLineRepository.TYPE_ABSENCE))
            .collect(Collectors.toList());

    for (TimecardLine absenceTL : absenceTLs) {
      absenceTL.setContractualTimecardLine(null);
      absenceTL.setTimecard(
          null); // detach absence from Timecard in order to be able to relink it later
    }

    timecardRepo.save(timecard);
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void generateTimecardLines(Timecard timecard) {
    timecardLineRepo
        .all()
        .filter("self.timecard.id = ? AND self.isDeletable = true", timecard.getId())
        .delete();
    timecardLineRepo.flush();

    LocalDate fromDate = timecard.getFromDate();
    LocalDate toDate = timecard.getToDate();

    List<PlanningLine> planningLines =
        planningLineRepo.findByEmployee(timecard.getEmployee()).fetch();
    for (PlanningLine planningLine : planningLines) {
      Employee employee = planningLine.getEmployee();
      Project project = planningLine.getProject();

      List<LocalDate> dates =
          frequencyService.getDates(planningLine.getFrequency(), timecard.getFromDate().getYear());
      for (LocalDate date : dates) {
        if (date.equals(fromDate)
            || date.isAfter(fromDate) && date.isBefore(toDate)
            || date.equals(toDate)) {
          TimecardLine timecardLine =
              timecardLineService.generateTimecardLine(
                  employee,
                  project,
                  date,
                  planningLine.getStartTime(),
                  planningLine.getEndTime(),
                  TimecardLineRepository.TYPE_CONTRACTUAL,
                  true);
          timecardLineRepo.save(timecardLine);
          timecard.addTimecardLineListItem(timecardLine);
        }
      }
    }

    timecardRepo.save(timecard);
  }

  @Override
  @Transactional
  public void attachScheduledTimecardLines(Timecard timecard) {
    List<TimecardLine> orphanTimecardLines =
        timecardLineRepo
            .all()
            .filter(
                "self.timecard IS NULL AND self.isDeletable = false AND self.employee.id = ?",
                timecard.getEmployee().getId())
            .fetch();

    LocalDate startDate = timecard.getFromDate();
    LocalDate endDate = timecard.getToDate();

    for (TimecardLine orphanTimecardLine : orphanTimecardLines) {
      LocalDate date = orphanTimecardLine.getDate();
      if (date.equals(startDate)
          || date.equals(endDate)
          || date.isAfter(startDate) && date.isBefore(endDate)) {
        if (orphanTimecardLine.getTypeSelect().equals(TimecardLineRepository.TYPE_ABSENCE)
            && orphanTimecardLine.getContractualTimecardLine() == null) {
          // Bind contractual line with absence line for payroll prep
          List<TimecardLine> contractualTimecardLineList =
              timecardLineRepo
                  .all()
                  .fetch()
                  .stream()
                  .filter(
                      timecardLine ->
                          timecardLine
                                  .getTypeSelect()
                                  .equals(TimecardLineRepository.TYPE_CONTRACTUAL)
                              && timecardLine.getTimecard().equals(timecard)
                              && timecardLine.getProject().equals(orphanTimecardLine.getProject())
                              && timecardLine.getDate().equals(orphanTimecardLine.getDate()))
                  .collect(Collectors.toList());

          if (!contractualTimecardLineList.isEmpty()) {
            orphanTimecardLine.setContractualTimecardLine(contractualTimecardLineList.get(0));
            timecardLineRepo.save(orphanTimecardLine);
          }
        }

        timecard.addTimecardLineListItem(orphanTimecardLine);
      }
    }

    timecardRepo.save(timecard);
  }

  @Override
  public void computeAll(Timecard timecard) throws AxelorException {
    this.computeHours(timecard);
    this.computeWeeklyHours(timecard);
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void computeHours(Timecard timecard) throws AxelorException {
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
    List<Integer> weeks = this.getWeeks(timecard.getFromDate(), timecard.getToDate());

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
      sumSupplementaryHours =
          sumSupplementaryHours.add(this.computeSupplementaryHours(timecard, week));
    }

    /* * *
     * 3) For the period of the Timecard,
     *      compute complement of hours
     *
     * Formula : complOfHours = extraHoursPeriod - notPaidLeavesHoursPeriod - suppHours - (10% of monthlyHours)
     *
     * NB : if 'complOfHours' is negative return 0.
     * NB : 'monthlyHours' is found in the employee's main employment contract.
     * * */
    BigDecimal complementOfHours = this.computeComplementOfHours(timecard, sumSupplementaryHours);

    /* * *
     * 4) For the period of the Timecard,
     *      compute complementary hours
     *
     * Formula : complHours = extraHoursPeriod - notPaidLeavesHoursPeriod - suppHours - complOfHours
     *
     * NB : complHours CAN be negative.
     * * */
    BigDecimal complementaryHours =
        this.computeComplementaryHours(timecard, sumSupplementaryHours, complementOfHours);

    /* * *
     * 5) For the period of the Timecard,
     *      compute night hours
     *
     * NB : night hours range is found in the HR config of the company of the employee's main employment contract.
     * * */
    BigDecimal nightHours = this.computeNightHours(timecard);

    timecard.setSupplementaryHours(sumSupplementaryHours);
    timecard.setComplementOfHours(complementOfHours);
    timecard.setComplementaryHours(complementaryHours);
    timecard.setNightHours(nightHours);

    timecardRepo.save(timecard);
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void computeWeeklyHours(Timecard timecard) {
    timecard.clearWeeklyHoursList();
    timecardRepo.flush();

    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, timecard.getToDate().getYear());

    List<Integer> weeks = this.getWeeks(timecard.getFromDate(), timecard.getToDate());

    for (Integer week : weeks) {
      WeeklyHours wh = new WeeklyHours();
      cal.setFirstDayOfWeek(Calendar.MONDAY);
      cal.set(Calendar.WEEK_OF_YEAR, week);

      cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
      wh.setStartDate(new Date(cal.getTime().getTime()).toLocalDate());
      cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
      wh.setEndDate(new Date(cal.getTime().getTime()).toLocalDate());

      wh.setContractualHours(
          this.computeContractualHours(timecard, wh.getStartDate(), wh.getEndDate()));
      wh.setAbsenceHours(this.computeAbsenceHours(timecard, wh.getStartDate(), wh.getEndDate()));
      wh.setSupplementaryHours(this.computeSupplementaryHours(timecard, week));

      timecard.addWeeklyHoursListItem(wh);
    }

    timecardRepo.save(timecard);
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
    cal.setFirstDayOfWeek(Calendar.MONDAY);
    while (currentSunday.isBefore(lastSundayOfMonth) || currentSunday.equals(lastSundayOfMonth)) {
      cal.set(
          currentSunday.getYear(),
          currentSunday.getMonthValue() - 1,
          currentSunday.getDayOfMonth());
      weeks.add(cal.get(Calendar.WEEK_OF_YEAR));
      currentSunday = currentSunday.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
    }

    return weeks;
  }

  @Override
  public BigDecimal computeWorkedHours(
      Long timecardId, int year, int weekOfYear, Employee employee) {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.WEEK_OF_YEAR, weekOfYear);
    cal.setFirstDayOfWeek(Calendar.MONDAY);

    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    LocalDate startDate = new Date(cal.getTime().getTime()).toLocalDate();
    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
    LocalDate endDate = new Date(cal.getTime().getTime()).toLocalDate();

    BigDecimal totalContractual =
        timecardLineService.getTotalContractualHours(timecardId, employee, startDate, endDate);
    BigDecimal totalExtra =
        timecardLineService.getTotalExtraHours(timecardId, employee, startDate, endDate);
    BigDecimal totalNotPaidLeaves =
        timecardLineService.getTotalNotPaidLeavesHours(timecardId, employee, startDate, endDate);

    return totalContractual.add(totalExtra).subtract(totalNotPaidLeaves);
  }

  protected BigDecimal computeSupplementaryHours(Timecard timecard, int weekOfYear) {
    BigDecimal total =
        this.computeWorkedHours(
            timecard.getId(), timecard.getToDate().getYear(), weekOfYear, timecard.getEmployee());
    total = total.subtract(new BigDecimal(35));

    return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
  }

  protected BigDecimal computeComplementOfHours(Timecard timecard, BigDecimal suppHours)
      throws AxelorException {
    BigDecimal totalExtra =
        timecardLineService.getTotalExtraHours(
            timecard.getId(), timecard.getEmployee(), timecard.getFromDate(), timecard.getToDate());
    BigDecimal totalNotPaidLeaves =
        timecardLineService.getTotalNotPaidLeavesHours(
            timecard.getId(), timecard.getEmployee(), timecard.getFromDate(), timecard.getToDate());

    EmploymentContract employmentContract = timecard.getEmployee().getMainEmploymentContract();
    if (employmentContract == null) {
      throw new AxelorException(
          timecard.getEmployee(),
          TraceBackRepository.CATEGORY_MISSING_FIELD,
          I18n.get("Please configure a main employement contract for employee %s."),
          timecard.getEmployee().getName());
    }
    BigDecimal monthlyHours = employmentContract.getMonthlyHours();
    if (monthlyHours.compareTo(BigDecimal.ZERO) == 0) {
      throw new AxelorException(
          employmentContract,
          TraceBackRepository.CATEGORY_MISSING_FIELD,
          I18n.get(
              "Please configure monthly hours on the main employement contract for employee %s"),
          timecard.getEmployee().getName());
    }
    BigDecimal tenPercentMonthlyHours = monthlyHours.multiply(BigDecimal.valueOf(0.1));

    BigDecimal total =
        totalExtra
            .subtract(totalNotPaidLeaves)
            .subtract(suppHours)
            .subtract(tenPercentMonthlyHours);

    return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
  }

  protected BigDecimal computeComplementaryHours(
      Timecard timecard, BigDecimal suppHours, BigDecimal complHours) {
    BigDecimal totalExtra =
        timecardLineService.getTotalExtraHours(
            timecard.getId(), timecard.getEmployee(), timecard.getFromDate(), timecard.getToDate());
    BigDecimal totalNotPaidLeaves =
        timecardLineService.getTotalNotPaidLeavesHours(
            timecard.getId(), timecard.getEmployee(), timecard.getFromDate(), timecard.getToDate());

    return totalExtra.subtract(totalNotPaidLeaves).subtract(suppHours).subtract(complHours);
  }

  protected BigDecimal computeNightHours(Timecard timecard) {
    BigDecimal total = BigDecimal.ZERO;

    if (timecard.getTimecardLineList() != null) {
      for (TimecardLine timecardLine : timecard.getTimecardLineList()) {
        total = total.add(timecardLine.getDurationNight());
      }
    }

    return total;
  }

  protected BigDecimal computeContractualHours(
      Timecard timecard, LocalDate startDate, LocalDate endDate) {
    return timecardLineService.getTotalContractualHours(
        timecard.getId(), timecard.getEmployee(), startDate, endDate);
  }

  protected BigDecimal computeAbsenceHours(
      Timecard timecard, LocalDate startDate, LocalDate endDate) {
    return timecardLineService.getTotalAbsenceHours(
        timecard.getId(), timecard.getEmployee(), startDate, endDate);
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void send(Timecard timecard) throws AxelorException {
    if (timecard.getTimecardLineList() == null || timecard.getTimecardLineList().isEmpty()) {
      throw new AxelorException(
          timecard,
          TraceBackRepository.CATEGORY_MISSING_FIELD,
          I18n.get("There is no lines in this timecard."));
    }

    for (TimecardLine timecardLine : timecard.getTimecardLineList()) {
      if (timecardLine.getTypeSelect().equals(TimecardLineRepository.TYPE_ABSENCE)) {
        if (timecardLine.getContractualTimecardLine() == null) {
          throw new AxelorException(
              timecardLine,
              TraceBackRepository.CATEGORY_MISSING_FIELD,
              I18n.get("There are still absence lines with no bound contractual line."));
        }

        if (timecardLine.getLeaveLine() == null) {
          throw new AxelorException(
              timecardLine,
              TraceBackRepository.CATEGORY_MISSING_FIELD,
              I18n.get("The following absence line does not have a leave reason:")
                  + "<br>"
                  + timecardLine.getProject().getFullName()
                  + " "
                  + I18n.get("on")
                  + " "
                  + timecardLine.getDate()
                  + " "
                  + I18n.get("FROM_TIME_lowercase")
                  + " "
                  + timecardLine.getStartTime()
                  + " "
                  + I18n.get("TO_TIME_lowercase")
                  + " "
                  + timecardLine.getEndTime());
        }
      }
    }

    timecard.setStatusSelect(TimecardRepository.STATUS_AWAITING_VALIDATION);
    timecardRepo.save(timecard);
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void validate(Timecard timecard) throws AxelorException {
    if (timecard.getEmployee().getDailyWorkHours().compareTo(BigDecimal.ZERO) == 0) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_MISSING_FIELD,
          I18n.get("Please configure how many hours employee %s works a day."),
          timecard.getEmployee().getName());
    }

    List<TimecardLine> abscenceTimecardLines =
        timecard
            .getTimecardLineList()
            .stream()
            .filter(e -> e.getTypeSelect().equals(TimecardLineRepository.TYPE_ABSENCE))
            .collect(Collectors.toList());

    for (TimecardLine absence : abscenceTimecardLines) {
      LeaveRequest leaveRequest = absence.getLeaveRequest();
      if (leaveRequest == null) {
        continue;
      }

      BigDecimal absenceHours = leaveRequest.getTotalAbsenceHours();
      BigDecimal leaveRequestDuration =
          absenceHours.divide(
              leaveRequest.getUser().getEmployee().getDailyWorkHours(),
              absenceHours.scale(),
              RoundingMode.HALF_UP);

      Employee employee = leaveRequest.getUser().getEmployee();
      if (employee == null) {
        throw new AxelorException(
            leaveRequest,
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.LEAVE_USER_EMPLOYEE),
            leaveRequest.getUser().getName());
      }

      LeaveLine leaveLine =
          Beans.get(LeaveLineRepository.class)
              .all()
              .filter(
                  "self.employee = ?1 AND self.leaveReason = ?2",
                  employee,
                  leaveRequest.getLeaveLine().getLeaveReason())
              .fetchOne();
      if (leaveLine == null) {
        throw new AxelorException(
            leaveRequest,
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.LEAVE_LINE),
            employee.getName(),
            leaveRequest.getLeaveLine().getLeaveReason().getLeaveReason());
      }

      if (leaveRequest.getInjectConsumeSelect().equals(LeaveRequestRepository.SELECT_CONSUME)) {
        leaveLine.setQuantity(leaveLine.getQuantity().subtract(leaveRequestDuration));
        if (leaveLine.getQuantity().compareTo(BigDecimal.ZERO) < 0
            && !employee.getNegativeValueLeave()) {
          throw new AxelorException(
              leaveRequest,
              TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
              I18n.get(IExceptionMessage.LEAVE_ALLOW_NEGATIVE_VALUE_EMPLOYEE),
              employee.getName());
        }
        if (leaveLine.getQuantity().compareTo(BigDecimal.ZERO) < 0
            && !leaveRequest.getLeaveLine().getLeaveReason().getAllowNegativeValue()) {
          throw new AxelorException(
              leaveRequest,
              TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
              I18n.get(IExceptionMessage.LEAVE_ALLOW_NEGATIVE_VALUE_REASON),
              leaveRequest.getLeaveLine().getLeaveReason().getLeaveReason());
        }
        leaveLine.setDaysToValidate(leaveLine.getDaysToValidate().add(leaveRequestDuration));
        leaveLine.setDaysValidated(leaveLine.getDaysValidated().add(leaveRequestDuration));
      } else {
        leaveLine.setQuantity(leaveLine.getQuantity().add(leaveRequestDuration));
        leaveLine.setDaysToValidate(leaveLine.getDaysToValidate().subtract(leaveRequestDuration));
      }

      Beans.get(LeaveLineRepository.class).save(leaveLine);
    }

    timecard.setValidatedBy(AuthUtils.getUser());
    timecard.setValidationDate(Beans.get(AppBaseService.class).getTodayDate());
    timecard.setStatusSelect(TimecardRepository.STATUS_VALIDATED);
    timecard.setGroundForRefusal(null);
    timecardRepo.save(timecard);
  }

  @Override
  @Transactional
  public void refuse(Timecard timecard) {
    timecard.setRefusedBy(AuthUtils.getUser());
    timecard.setRefusalDate(Beans.get(AppBaseService.class).getTodayDate());
    timecard.setStatusSelect(TimecardRepository.STATUS_REFUSED);
    timecardRepo.save(timecard);
  }
}
