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
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.axelor.exception.AxelorException;
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

public class LeaveServiceTimeCardImpl extends LeaveServiceImpl {

    protected TimeCardLineService timeCardLineService;
    protected PlanningLineRepository planningLineRepo;
    protected FrequencyService frequencyService;
    protected TimeCardLineRepository timeCardLineRepo;

    @Inject
    public LeaveServiceTimeCardImpl(LeaveLineRepository leaveLineRepo, WeeklyPlanningService weeklyPlanningService,
                                    PublicHolidayHrService publicHolidayHrService, LeaveRequestRepository leaveRequestRepo,
                                    AppBaseService appBaseService, HRConfigService hrConfigService, TemplateMessageService templateMessageService,
                                    ICalendarEventRepository icalEventRepo, ICalendarService icalendarService,
                                    TimeCardLineService timeCardLineService, PlanningLineRepository planningLineRepo, FrequencyService frequencyService, TimeCardLineRepository timeCardLineRepo) {
        super(leaveLineRepo, weeklyPlanningService, publicHolidayHrService, leaveRequestRepo, appBaseService, hrConfigService, templateMessageService, icalEventRepo, icalendarService);

        this.timeCardLineService = timeCardLineService;
        this.planningLineRepo = planningLineRepo;
        this.frequencyService = frequencyService;
        this.timeCardLineRepo = timeCardLineRepo;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void validate(LeaveRequest leaveRequest) throws AxelorException {
        super.validate(leaveRequest);


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
                if (date.equals(fromDate) || date.isAfter(fromDate) && date.isBefore(toDate) || date.equals(toDate)) {
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
                    if ((fromDateTCL.equals(fromDateLR) || fromDateTCL.isAfter(fromDateLR)) && (fromDateTCL.isBefore(toDateLR) || fromDateTCL.equals(toDateLR))) {
                        startTime = fromDateTCL.toLocalTime();
                        if ((toDateTCL.isBefore(toDateLR) || toDateTCL.equals(toDateLR))) {
                            endTime = toDateTCL.toLocalTime();
                        } else {
                            endTime = toDateLR.toLocalTime();
                        }
                    } else if (fromDateTCL.isBefore(fromDateLR) && toDateTCL.isAfter(fromDateLR) && (toDateTCL.isBefore(toDateLR) || toDateTCL.equals(toDateLR))) {
                        startTime = fromDateLR.toLocalTime();
                        endTime = toDateTCL.toLocalTime();
                    } else if (fromDateTCL.isBefore(fromDateLR) && toDateTCL.isAfter(toDateLR)) {
                        startTime = fromDateLR.toLocalTime();
                        endTime = toDateLR.toLocalTime();
                    }

                    TimeCardLine tcl = timeCardLineService.generateTimeCardLine(employee, project, date, startTime, endTime, TimeCardLineRepository.TYPE_ABSENCE, false);
                    if (tcl != null) {
                        tcl.setLeaveLine(leaveRequest.getLeaveLine());
                        timeCardLineRepo.save(tcl);
                        leaveRequest.addTimeCardLineListItem(tcl);
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void cancel(LeaveRequest leaveRequest) throws AxelorException {
        List<TimeCardLine> timeCardLines = leaveRequest.getTimeCardLineList();

        if (timeCardLines != null && !timeCardLines.isEmpty()) {
            for (TimeCardLine timeCardLine : timeCardLines) {
                // TODO: test the if condition
                if (!timeCardLine.getSubstitutionTimeCardLineList().isEmpty()) {
                    return;
                }
            }

            List<Long> timeCardLineIds = timeCardLines.stream().map(TimeCardLine::getId).collect(Collectors.toList());
            timeCardLineRepo.all().filter("self.id IN (?)", timeCardLineIds).delete();
        }

        super.cancel(leaveRequest);
    }

    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void computeDurationTotals(LeaveRequest leaveRequest) {
        BigDecimal totalAbsence = BigDecimal.ZERO;
        BigDecimal totalSubstitution = BigDecimal.ZERO;

        if (leaveRequest.getTimeCardLineList() != null) {
            for (TimeCardLine timeCardLine : leaveRequest.getTimeCardLineList()) {
                totalAbsence = totalAbsence.add(timeCardLine.getDuration());
                totalSubstitution = totalSubstitution.add(timeCardLine.getTotalSubstitutionHours());
            }
        }

        leaveRequest.setTotalAbsenceHours(totalAbsence);
        leaveRequest.setTotalSubstitutionHours(totalSubstitution);

        leaveRequestRepo.save(leaveRequest);
    }
}
