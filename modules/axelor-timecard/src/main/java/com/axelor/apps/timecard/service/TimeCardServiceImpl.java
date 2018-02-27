package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.LeaveRequest;
import com.axelor.apps.hr.db.repo.LeaveRequestRepository;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.PlanningLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class TimeCardServiceImpl implements TimeCardService {

    protected TimeCardRepository timeCardRepo;
    protected LeaveRequestRepository leaveRequestRepo;
    protected PlanningLineRepository planningLineRepo;
    protected FrequencyService frequencyService;

    @Inject
    public TimeCardServiceImpl(TimeCardRepository timeCardRepo, LeaveRequestRepository leaveRequestRepo,
                               PlanningLineRepository planningLineRepo, FrequencyService frequencyService) {
        this.timeCardRepo = timeCardRepo;
        this.leaveRequestRepo = leaveRequestRepo;
        this.planningLineRepo = planningLineRepo;
        this.frequencyService = frequencyService;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void generateTimeCardLines(TimeCard timeCard) {
        timeCard.clearTimeCardLineList();
        timeCardRepo.flush();

        LocalDate fromDate = timeCard.getFromDate();
        LocalDate toDate = timeCard.getToDate();

        List<LeaveRequest> leaveRequests = leaveRequestRepo.all().filter("self.user.id = ?1 AND ((?2 BETWEEN self.fromDate AND self.toDate) OR (?3 BETWEEN self.fromDate AND self.toDate)) AND self.statusSelect = ?4", timeCard.getEmployee().getUser().getId(), fromDate, toDate, LeaveRequestRepository.STATUS_VALIDATED).fetch();

        List<PlanningLine> planningLines = planningLineRepo.findByEmployee(timeCard.getEmployee()).fetch();
        for (PlanningLine planningLine : planningLines) {
            Employee employee = planningLine.getEmployee();
            Project project = planningLine.getProject();

            List<LocalDate> dates = frequencyService.getDates(planningLine.getFrequency());
            for (LocalDate date : dates) {
                if (date.equals(fromDate) || date.isAfter(fromDate) && date.isBefore(toDate) || date.equals(toDate)) {
                    TimeCardLine timeCardLine = generateTimeCardLine(employee, project, date, planningLine.getStartTime(), planningLine.getEndTime(), TimeCardLineRepository.TYPE_CONTRACTUAL);
                    timeCard.addTimeCardLineListItem(timeCardLine);

                    for (LeaveRequest leaveRequest : leaveRequests) {
                        LocalDateTime fromDateTCL = timeCardLine.getStartDateTime();
                        LocalDateTime toDateTCL = timeCardLine.getEndDateTime();

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


                        // TODO: below, duplicated vars.
                        TimeCardLine tcl = null;
                        if ((fromDateTCL.equals(fromDateLR) || fromDateTCL.isAfter(fromDateLR)) && (fromDateTCL.isBefore(toDateLR) || fromDateTCL.equals(toDateLR))) {
                            LocalTime startTime = fromDateTCL.toLocalTime();
                            LocalTime endTime;
                            if ((toDateTCL.isBefore(toDateLR) || toDateTCL.equals(toDateLR))) {
                                endTime = toDateTCL.toLocalTime();
                            } else {
                                endTime = toDateLR.toLocalTime();
                            }
                            tcl = generateTimeCardLine(employee, project, date, startTime, endTime, TimeCardLineRepository.TYPE_ABSENCE);
                        } else if (fromDateTCL.isBefore(fromDateLR) && toDateTCL.isAfter(fromDateLR) && (toDateTCL.isBefore(toDateLR) || toDateTCL.equals(toDateLR))) {
                            LocalTime startTime = fromDateLR.toLocalTime();
                            LocalTime endTime = toDateTCL.toLocalTime();
                            tcl = generateTimeCardLine(employee, project, date, startTime, endTime, TimeCardLineRepository.TYPE_ABSENCE);
                        } else if (fromDateTCL.isBefore(fromDateLR) && toDateTCL.isAfter(toDateLR)) {
                            LocalTime startTime = fromDateLR.toLocalTime();
                            LocalTime endTime = toDateLR.toLocalTime();
                            tcl = generateTimeCardLine(employee, project, date, startTime, endTime, TimeCardLineRepository.TYPE_ABSENCE);
                        }

                        if (tcl != null) {
                            tcl.setLeaveLine(leaveRequest.getLeaveLine());
                            timeCard.addTimeCardLineListItem(tcl);
                        }
                    }
                }
            }
        }

        timeCardRepo.save(timeCard);
    }

    public TimeCardLine generateTimeCardLine(Employee employee, Project project, LocalDate date, LocalTime startTime, LocalTime endTime, String lineType) {
        TimeCardLine timeCardLine = new TimeCardLine();

        timeCardLine.setEmployee(employee);
        timeCardLine.setProject(project);
        timeCardLine.setWeekDay(date.getDayOfWeek().getValue());


        timeCardLine.setDate(date);
        timeCardLine.setStartTime(startTime);
        timeCardLine.setEndTime(endTime);

        timeCardLine.setStartDateTime(LocalDateTime.of(date, startTime));
        timeCardLine.setEndDateTime(LocalDateTime.of(date, endTime));


        timeCardLine.setDuration(BigDecimal.valueOf(Duration.between(startTime, endTime).toMinutes() / 60.0));
        timeCardLine.setTypeSelect(lineType);

        return timeCardLine;
    }
}
