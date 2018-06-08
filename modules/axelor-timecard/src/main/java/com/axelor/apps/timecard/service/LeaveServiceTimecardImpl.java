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

import com.axelor.apps.base.db.ICalendarEvent;
import com.axelor.apps.base.db.repo.ICalendarEventRepository;
import com.axelor.apps.base.ical.ICalendarService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.weeklyplanning.WeeklyPlanningService;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.LeaveRequest;
import com.axelor.apps.hr.db.repo.LeaveLineRepository;
import com.axelor.apps.hr.db.repo.LeaveRequestRepository;
import com.axelor.apps.hr.service.config.HRConfigService;
import com.axelor.apps.hr.service.leave.LeaveServiceImpl;
import com.axelor.apps.hr.service.publicHoliday.PublicHolidayHrService;
import com.axelor.apps.message.service.TemplateMessageService;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.db.repo.TimecardLineRepository;
import com.axelor.apps.timecard.service.app.AppTimecardService;
import com.axelor.auth.AuthUtils;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LeaveServiceTimecardImpl extends LeaveServiceImpl {

  protected TimecardLineService timecardLineService;
  protected PlanningLineRepository planningLineRepo;
  protected FrequencyService frequencyService;
  protected TimecardLineRepository timecardLineRepo;

  @Inject
  public LeaveServiceTimecardImpl(
      LeaveLineRepository leaveLineRepo,
      WeeklyPlanningService weeklyPlanningService,
      PublicHolidayHrService publicHolidayHrService,
      LeaveRequestRepository leaveRequestRepo,
      AppBaseService appBaseService,
      HRConfigService hrConfigService,
      TemplateMessageService templateMessageService,
      ICalendarEventRepository icalEventRepo,
      ICalendarService icalendarService,
      TimecardLineService timecardLineService,
      PlanningLineRepository planningLineRepo,
      FrequencyService frequencyService,
      TimecardLineRepository timecardLineRepo) {
    super(
        leaveLineRepo,
        weeklyPlanningService,
        publicHolidayHrService,
        leaveRequestRepo,
        appBaseService,
        hrConfigService,
        templateMessageService,
        icalEventRepo,
        icalendarService);

    this.timecardLineService = timecardLineService;
    this.planningLineRepo = planningLineRepo;
    this.frequencyService = frequencyService;
    this.timecardLineRepo = timecardLineRepo;
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void validate(LeaveRequest leaveRequest) throws AxelorException {
    if (!Beans.get(AppTimecardService.class).getAppTimecard().getDeductLeavesFromTimecard()
        && leaveRequest.getLeaveLine().getLeaveReason().getManageAccumulation()) {
      manageValidateLeaves(leaveRequest);
    }

    leaveRequest.setStatusSelect(LeaveRequestRepository.STATUS_VALIDATED);
    leaveRequest.setValidatedBy(AuthUtils.getUser());
    leaveRequest.setValidationDate(appBaseService.getTodayDate());

    leaveRequestRepo.save(leaveRequest);

    createEvents(leaveRequest);

    // Generates scheduled time card lines

    Employee employee = leaveRequest.getUser().getEmployee();
    Set<Project> projects = leaveRequest.getProjectSet();

    LocalDate fromDate = leaveRequest.getFromDate();
    LocalDate toDate = leaveRequest.getToDate();

    List<Integer> years = new ArrayList<>();
    for (int i = fromDate.getYear(); i <= toDate.getYear(); i++) {
      years.add(i);
    }

    List<PlanningLine> planningLines = planningLineRepo.findByEmployee(employee).fetch();
    for (PlanningLine planningLine : planningLines) {
      Project project = planningLine.getProject();

      if (!projects.isEmpty() && !projects.contains(project)) {
        continue;
      }

      List<LocalDate> dates = new ArrayList<>();
      for (Integer year : years) {
        dates.addAll(frequencyService.getDates(planningLine.getFrequency(), year));
      }

      for (LocalDate date : dates) {
        if (date.equals(fromDate)
            || date.isAfter(fromDate) && date.isBefore(toDate)
            || date.equals(toDate)) {
          LocalDateTime fromDateTCL = LocalDateTime.of(date, planningLine.getStartTime());
          LocalDateTime toDateTCL = LocalDateTime.of(date, planningLine.getEndTime());

          LocalDateTime fromDateLR;
          if (leaveRequest.getStartOnSelect().equals(LeaveRequestRepository.SELECT_MORNING)) {
            fromDateLR = LocalDateTime.of(leaveRequest.getFromDate(), LocalTime.MIDNIGHT);
          } else {
            fromDateLR = LocalDateTime.of(leaveRequest.getFromDate(), LocalTime.NOON);
          }

          LocalDateTime toDateLR;
          if (leaveRequest.getEndOnSelect().equals(LeaveRequestRepository.SELECT_MORNING)) {
            toDateLR = LocalDateTime.of(leaveRequest.getToDate(), LocalTime.NOON);
          } else {
            toDateLR = LocalDateTime.of(leaveRequest.getToDate().plusDays(1), LocalTime.MIDNIGHT);
          }

          LocalTime startTime = null;
          LocalTime endTime = null;
          if ((fromDateTCL.equals(fromDateLR) || fromDateTCL.isAfter(fromDateLR))
              && (fromDateTCL.isBefore(toDateLR) || fromDateTCL.equals(toDateLR))) {
            startTime = fromDateTCL.toLocalTime();
            if ((toDateTCL.isBefore(toDateLR) || toDateTCL.equals(toDateLR))) {
              endTime = toDateTCL.toLocalTime();
            } else {
              endTime = toDateLR.toLocalTime();
            }
          } else if (fromDateTCL.isBefore(fromDateLR)
              && toDateTCL.isAfter(fromDateLR)
              && (toDateTCL.isBefore(toDateLR) || toDateTCL.equals(toDateLR))) {
            startTime = fromDateLR.toLocalTime();
            endTime = toDateTCL.toLocalTime();
          } else if (fromDateTCL.isBefore(fromDateLR) && toDateTCL.isAfter(toDateLR)) {
            startTime = fromDateLR.toLocalTime();
            endTime = toDateLR.toLocalTime();
          }

          TimecardLine tcl =
              timecardLineService.generateTimecardLine(
                  employee,
                  project,
                  date,
                  startTime,
                  endTime,
                  TimecardLineRepository.TYPE_ABSENCE,
                  false);
          if (tcl != null) {
            tcl.setLeaveLine(leaveRequest.getLeaveLine());
            timecardLineRepo.save(tcl);
            leaveRequest.addTimecardLineListItem(tcl);
          }
        }
      }
    }
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void cancel(LeaveRequest leaveRequest) throws AxelorException {
    List<TimecardLine> timecardLines = leaveRequest.getTimecardLineList();

    if (timecardLines != null && !timecardLines.isEmpty()) {
      for (TimecardLine timecardLine : timecardLines) {
        if (!timecardLine.getSubstitutionTimecardLineList().isEmpty()) {
          throw new AxelorException(timecardLine, TraceBackRepository.TYPE_FUNCTIONNAL,
              I18n.get("Please cancel all substitution lines before canceling leave request."));
        }
      }

      List<Long> timecardLineIds =
          timecardLines.stream().map(TimecardLine::getId).collect(Collectors.toList());
      timecardLineRepo.all().filter("self.id IN (?)", timecardLineIds).delete();
    }

    if (!Beans.get(AppTimecardService.class).getAppTimecard().getDeductLeavesFromTimecard()
        && leaveRequest.getLeaveLine().getLeaveReason().getManageAccumulation()) {
      manageCancelLeaves(leaveRequest);
    }

    if (leaveRequest.getIcalendarEvent() != null) {
      ICalendarEvent event = leaveRequest.getIcalendarEvent();
      leaveRequest.setIcalendarEvent(null);
      icalEventRepo.remove(icalEventRepo.find(event.getId()));
    }
    leaveRequest.setStatusSelect(LeaveRequestRepository.STATUS_CANCELED);
  }

  @Transactional(rollbackOn = {AxelorException.class, Exception.class})
  public void computeDurationTotals(LeaveRequest leaveRequest) {
    BigDecimal totalAbsence = BigDecimal.ZERO;
    BigDecimal totalSubstitution = BigDecimal.ZERO;

    if (leaveRequest.getTimecardLineList() != null) {
      for (TimecardLine timecardLine : leaveRequest.getTimecardLineList()) {
        totalAbsence = totalAbsence.add(timecardLine.getDuration());
        totalSubstitution = totalSubstitution.add(timecardLine.getTotalSubstitutionHours());
      }
    }

    leaveRequest.setTotalAbsenceHours(totalAbsence);
    leaveRequest.setTotalSubstitutionHours(totalSubstitution);

    leaveRequestRepo.save(leaveRequest);
  }
}
