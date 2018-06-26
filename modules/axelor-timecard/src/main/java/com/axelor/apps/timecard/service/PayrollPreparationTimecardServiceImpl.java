package com.axelor.apps.timecard.service;

import com.axelor.apps.base.db.EventsPlanningLine;
import com.axelor.apps.base.service.weeklyplanning.WeeklyPlanningService;
import com.axelor.apps.hr.db.EmploymentContract;
import com.axelor.apps.hr.db.LeaveLine;
import com.axelor.apps.hr.db.LeaveRequest;
import com.axelor.apps.hr.db.OtherCostsEmployee;
import com.axelor.apps.hr.db.PayrollPreparation;
import com.axelor.apps.hr.db.repo.LeaveRequestRepository;
import com.axelor.apps.hr.db.repo.PayrollPreparationRepository;
import com.axelor.apps.hr.service.PayrollPreparationService;
import com.axelor.apps.hr.service.leave.LeaveService;
import com.axelor.apps.timecard.db.LeaveSummaryPayrollPrep;
import com.axelor.apps.timecard.db.Timecard;
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.apps.timecard.db.repo.TimecardLineRepository;
import com.axelor.apps.timecard.db.repo.TimecardRepository;
import com.axelor.apps.timecard.service.app.AppTimecardService;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PayrollPreparationTimecardServiceImpl extends PayrollPreparationService {

  protected TimecardLineRepository timecardLineRepo;
  protected TimecardService timecardService;

  @Inject
  public PayrollPreparationTimecardServiceImpl(
      LeaveService leaveService,
      LeaveRequestRepository leaveRequestRepo,
      WeeklyPlanningService weeklyPlanningService,
      TimecardLineRepository timecardLineRepo,
      TimecardService timecardService) {
    super(leaveService, leaveRequestRepo, weeklyPlanningService);
    this.timecardLineRepo = timecardLineRepo;
    this.timecardService = timecardService;
  }

  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void calculate(PayrollPreparation payrollPrep) throws AxelorException {
    if (payrollPrep.getEmployee().getDailyWorkHours().compareTo(BigDecimal.ZERO) == 0) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_MISSING_FIELD,
          I18n.get("Please configure how many hours employee %s works a day."),
          payrollPrep.getEmployee().getName());
    }

    List<LocalDate> employeePublicHolidays =
        payrollPrep
            .getEmployee()
            .getPublicHolidayEventsPlanning()
            .getEventsPlanningLineList()
            .stream()
            .map(EventsPlanningLine::getDate)
            .collect(Collectors.toList());

    List<Timecard> timecards =
        Beans.get(TimecardRepository.class)
            .all()
            .filter(
                "self.employee.id = ? AND self.period.id = ?",
                payrollPrep.getEmployee().getId(),
                payrollPrep.getPeriod().getId())
            .fetch();

    BigDecimal dailyWorkHours = payrollPrep.getEmployee().getDailyWorkHours();
    EmploymentContract mainEmploymentContract =
        payrollPrep.getEmployee().getMainEmploymentContract();

    BigDecimal complementOfHours = BigDecimal.ZERO;
    BigDecimal complementaryHours = BigDecimal.ZERO;

    BigDecimal publicHolidayScheduled = BigDecimal.ZERO;
    BigDecimal publicHolidayExceptional = BigDecimal.ZERO;
    BigDecimal nightHoursScheduled = BigDecimal.ZERO;
    BigDecimal nightHoursExceptional = BigDecimal.ZERO;
    BigDecimal sundayScheduled = BigDecimal.ZERO;
    BigDecimal sundayExceptional = BigDecimal.ZERO;

    BigDecimal supplementaryHours = BigDecimal.ZERO;

    BigDecimal leaveDays = BigDecimal.ZERO;
    Map<LeaveRequest, LeaveSummaryPayrollPrep> leaveRequestMap = new HashMap<>();
    List<LeaveSummaryPayrollPrep> leaveSummaryPayrollPreps = new ArrayList<>();

    StringBuilder comments = new StringBuilder();

    for (Timecard timecard : timecards) {
      // Update timecard
      timecardService.computeAll(timecard);

      // Contractual
      complementOfHours = complementOfHours.add(timecard.getComplementOfHours());
      complementaryHours = complementaryHours.add(timecard.getComplementaryHours());

      for (TimecardLine timecardLine : timecard.getTimecardLineList()) {
        // Supplement: Public holiday & Night hours & Sunday hours
        if (timecardLine.getTypeSelect().equals(TimecardLineRepository.TYPE_CONTRACTUAL)) {
          try {
            this.doOperations(
                "add",
                timecardLine,
                employeePublicHolidays,
                publicHolidayScheduled,
                nightHoursScheduled,
                sundayScheduled);
          } catch (Exception e) {
            TraceBackService.trace(e);
          }

          List<TimecardLine> absenceTimecardLines =
              timecardLineRepo
                  .all()
                  .filter("self.contractualTimecardLine.id = ?", timecardLine.getId())
                  .fetch();

          for (TimecardLine absenceTimecardLine : absenceTimecardLines) {
            try {
              this.doOperations(
                  "subtract",
                  absenceTimecardLine,
                  employeePublicHolidays,
                  publicHolidayScheduled,
                  nightHoursScheduled,
                  sundayScheduled);
            } catch (Exception e) {
              TraceBackService.trace(e);
            }
          }

        } else if (timecardLine.getTypeSelect().equals(TimecardLineRepository.TYPE_EXTRA)) {
          try {
            this.doOperations(
                "add",
                timecardLine,
                employeePublicHolidays,
                publicHolidayExceptional,
                nightHoursExceptional,
                sundayExceptional);
          } catch (Exception e) {
            TraceBackService.trace(e);
          }
        }

        // Leaves
        if (timecardLine.getTypeSelect().equals(TimecardLineRepository.TYPE_ABSENCE)) {
          LeaveRequest leaveRequest = timecardLine.getLeaveRequest();
          if (leaveRequest != null) {
            LeaveSummaryPayrollPrep leaveSummary =
                leaveRequestMap.containsKey(leaveRequest)
                    ? leaveRequestMap.get(leaveRequest)
                    : new LeaveSummaryPayrollPrep();

            fillLeaveSummary(
                leaveSummary,
                leaveRequest.getLeaveLine(),
                leaveRequest.getFromDate(),
                leaveRequest.getToDate(),
                timecardLine.getDuration(),
                dailyWorkHours);

            leaveDays = leaveDays.add(leaveSummary.getNumberOfDays());

            leaveRequestMap.put(timecardLine.getLeaveRequest(), leaveSummary);
          }
          if (leaveRequest == null) {
            LeaveSummaryPayrollPrep leaveSummary = new LeaveSummaryPayrollPrep();

            fillLeaveSummary(
                leaveSummary,
                timecardLine.getLeaveLine(),
                timecardLine.getDate(),
                timecardLine.getDate(),
                timecardLine.getDuration(),
                dailyWorkHours);

            leaveDays = leaveDays.add(leaveSummary.getNumberOfDays());

            leaveSummaryPayrollPreps.add(leaveSummary);
          }
        }
      }

      // Bonuses
      supplementaryHours = supplementaryHours.add(timecard.getSupplementaryHours());

      // Comments
      if (timecard.getPayrollPreparationComment() != null) {
        comments.append(timecard.getPayrollPreparationComment());
        comments.append("\n");
      }
    }

    // Contractual
    payrollPrep.setMonthlyWage(mainEmploymentContract.getMonthlyWage());
    payrollPrep.setComplementOfHours(complementOfHours);
    payrollPrep.setComplementaryHours(complementaryHours);

    // Supplement
    payrollPrep.setPublicHolidayScheduled(publicHolidayScheduled);
    payrollPrep.setPublicHolidayExceptional(publicHolidayExceptional);
    payrollPrep.setNightHoursScheduled(nightHoursScheduled);
    payrollPrep.setNightHoursExceptional(nightHoursExceptional);
    payrollPrep.setSundayScheduled(sundayScheduled);
    payrollPrep.setSundayExceptional(sundayExceptional);

    // Bonuses
    BigDecimal thresholdSupplementaryHours =
        Beans.get(AppTimecardService.class).getAppTimecard().getThresholdForSupplementaryHours50();
    if (supplementaryHours.compareTo(thresholdSupplementaryHours) > 0) {
      payrollPrep.setSupplementaryHours25(thresholdSupplementaryHours);
      payrollPrep.setSupplementaryHours50(supplementaryHours.subtract(thresholdSupplementaryHours));
    } else {
      payrollPrep.setSupplementaryHours25(supplementaryHours);
      payrollPrep.setSupplementaryHours50(BigDecimal.ZERO);
    }

    payrollPrep.setEmployeeBonusAmount(this.computeEmployeeBonusAmount(payrollPrep));

    // Leaves
    payrollPrep.setLeaveDuration(leaveDays.setScale(1, RoundingMode.HALF_UP));

    payrollPrep.clearLeaveSummaryPayrollPrepList();
    JPA.flush();

    leaveSummaryPayrollPreps.addAll(leaveRequestMap.values());
    for (LeaveSummaryPayrollPrep leaveSummary : leaveSummaryPayrollPreps) {
      payrollPrep.addLeaveSummaryPayrollPrepListItem(leaveSummary);
    }

    // Expenses amount + list & other costs
    payrollPrep.setExpenseAmount(this.computeExpenseAmount(payrollPrep));
    for (OtherCostsEmployee otherCostsEmployee :
        mainEmploymentContract.getOtherCostsEmployeeSet()) {
      payrollPrep.addOtherCostsEmployeeSetItem(otherCostsEmployee);
    }

    // Misc: Lunch vouchers amount + list
    payrollPrep.setLunchVoucherNumber(this.computeLunchVoucherNumber(payrollPrep));

    // Misc: Comments
    payrollPrep.setComments(comments.toString());

    Beans.get(PayrollPreparationRepository.class).save(payrollPrep);
  }

  protected void doOperations(
      String methodName,
      TimecardLine timecardLine,
      List<LocalDate> employeePublicHolidays,
      BigDecimal publicHoliday,
      BigDecimal nightHours,
      BigDecimal sunday)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = BigDecimal.class.getMethod(methodName, BigDecimal.class);

    if (employeePublicHolidays.contains(timecardLine.getDate())) {
      publicHoliday = (BigDecimal) method.invoke(publicHoliday, timecardLine.getDuration());
    }

    nightHours = (BigDecimal) method.invoke(nightHours, timecardLine.getDurationNight());

    if (timecardLine.getWeekDay().equals(DayOfWeek.SUNDAY.getValue())) {
      sunday = (BigDecimal) method.invoke(sunday, timecardLine.getDuration());
    }
  }

  protected void fillLeaveSummary(
      LeaveSummaryPayrollPrep leaveSummary,
      LeaveLine leaveLine,
      LocalDate fromDate,
      LocalDate toDate,
      BigDecimal duration,
      BigDecimal dailyWorkHours) {
    leaveSummary.setLeaveLine(leaveLine);
    leaveSummary.setFromDate(fromDate);
    leaveSummary.setToDate(toDate);
    leaveSummary.setNumberOfHours(leaveSummary.getNumberOfHours().add(duration));
    leaveSummary.setNumberOfDays(
        leaveSummary.getNumberOfHours().divide(dailyWorkHours, RoundingMode.HALF_UP));
  }

  public void close(PayrollPreparation payrollPreparation) {
    List<Timecard> timecards =
        Beans.get(TimecardRepository.class)
            .all()
            .filter(
                "self.employee.id = ? AND self.period.id = ?",
                payrollPreparation.getEmployee().getId(),
                payrollPreparation.getPeriod().getId())
            .fetch();

    for (Timecard timecard : timecards) {
      timecardService.close(timecard);
    }

    payrollPreparation.setStatusSelect(PayrollPreparationRepository.STATUS_CLOSED);
    payrollPreparationRepo.save(payrollPreparation);
  }
}
