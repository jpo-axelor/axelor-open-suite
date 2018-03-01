package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.LeaveRequest;
import com.axelor.apps.hr.db.repo.LeaveRequestRepository;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.PlanningLine;
import com.axelor.apps.timecard.db.TempTimeCardLine;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.TempTimeCardLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TempTimeCardLineServiceImpl implements TempTimeCardLineService {

    protected TempTimeCardLineRepository tempTimeCardLineRepo;
    protected PlanningLineService planningLineService;
    protected FrequencyService frequencyService;
    protected TimeCardLineService timeCardLineService;
    protected LeaveRequestRepository leaveRequestRepo;

    @Inject
    public TempTimeCardLineServiceImpl(TempTimeCardLineRepository tempTimeCardLineRepo, PlanningLineService planningLineService,
                                       FrequencyService frequencyService, TimeCardLineService timeCardLineService, LeaveRequestRepository leaveRequestRepo) {
        this.tempTimeCardLineRepo = tempTimeCardLineRepo;
        this.planningLineService = planningLineService;
        this.frequencyService = frequencyService;
        this.timeCardLineService = timeCardLineService;
        this.leaveRequestRepo = leaveRequestRepo;
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public void invalidateTempTimeCardLines() {
        tempTimeCardLineRepo.all().delete();
        tempTimeCardLineRepo.flush();
    }

    @Override
    @Transactional(rollbackOn = {AxelorException.class, Exception.class})
    public List<TempTimeCardLine> generateTempTimeCardLines(Project project, Employee employee, LocalDate startDate, LocalDate endDate) {
        invalidateTempTimeCardLines();

        List<TempTimeCardLine> tempTimeCardLines = new ArrayList<>();

        // Get leave requests
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        if ((project == null && employee != null) || (project != null && employee != null)) {
            leaveRequests = leaveRequestRepo.all().filter("self.user.employee.id = ?1 AND self.statusSelect = ?2 AND (((?3 BETWEEN self.fromDate AND self.toDate) OR (?4 BETWEEN self.fromDate AND self.toDate)) OR ((self.fromDate BETWEEN ?3 AND ?4) OR (self.toDate BETWEEN ?3 AND ?4)))", employee.getId(), LeaveRequestRepository.STATUS_VALIDATED, startDate, endDate).fetch();
        } else if (project != null && employee == null) {
            List<String> employeeIdsList = new ArrayList<>();
            for (User user : project.getMembersUserSet()) {
                if (user.getEmployee() != null) {
                    employeeIdsList.add(user.getEmployee().getId().toString());
                }
            }

            String employeeIds = "0";
            if (employeeIdsList.size() > 0) {
                employeeIds = String.join(",", employeeIdsList);
            }

            leaveRequests = leaveRequestRepo.all().filter("self.user.employee.id IN (?1) AND self.statusSelect = ?2 AND (((?3 BETWEEN self.fromDate AND self.toDate) OR (?4 BETWEEN self.fromDate AND self.toDate)) OR ((self.fromDate BETWEEN ?3 AND ?4) OR (self.toDate BETWEEN ?3 AND ?4)))", employeeIds, LeaveRequestRepository.STATUS_VALIDATED, startDate, endDate).fetch();
        }


        // Get planning lines
        List<PlanningLine> planningLines = planningLineService.getPlanningLines(project, employee);
        // Generate temp time card lines
        for (PlanningLine planningLine : planningLines) {
            List<LocalDate> dates = frequencyService.getDates(planningLine.getFrequency(), null);
            for (LocalDate date : dates) {
                if (date.equals(startDate) || date.equals(endDate) || date.isAfter(startDate) && date.isBefore(endDate)) {
                    TempTimeCardLine tempTimeCardLine = generateTempTimeCardLine(planningLine.getEmployee(), planningLine.getProject(), date, planningLine.getStartTime(), planningLine.getEndTime(), TempTimeCardLineRepository.TYPE_CONTRACTUAL);

                    tempTimeCardLines.add(tempTimeCardLine);
                    tempTimeCardLineRepo.save(tempTimeCardLine);

                    List<TempTimeCardLine> tempTimeCardLinesAbsence = generateTempTimeCardLineAbsence(planningLine.getEmployee(), planningLine.getProject(), date, leaveRequests, tempTimeCardLine);
                    tempTimeCardLines.addAll(tempTimeCardLinesAbsence);
                }
            }
        }


        // Get scheduled time card lines
        List<TimeCardLine> timeCardLines = timeCardLineService.getScheduledTimeCardLine(project, employee);
        // Generate temp time card lines
        for (TimeCardLine timeCardLine : timeCardLines) {
            LocalDate date = timeCardLine.getDate();
            if (date.equals(startDate) || date.equals(endDate) || date.isAfter(startDate) && date.isBefore(endDate)) {
                TempTimeCardLine tempTimeCardLine = generateTempTimeCardLine(timeCardLine.getEmployee(), timeCardLine.getProject(), timeCardLine.getDate(), timeCardLine.getStartTime(), timeCardLine.getEndTime(), timeCardLine.getTypeSelect());

                tempTimeCardLines.add(tempTimeCardLine);
                tempTimeCardLineRepo.save(tempTimeCardLine);

                List<TempTimeCardLine> tempTimeCardLinesAbsence = generateTempTimeCardLineAbsence(timeCardLine.getEmployee(), timeCardLine.getProject(), date, leaveRequests, tempTimeCardLine);
                tempTimeCardLines.addAll(tempTimeCardLinesAbsence);
            }
        }

        return tempTimeCardLines;
    }

    public TempTimeCardLine generateTempTimeCardLine(Employee employee, Project project, LocalDate date, LocalTime startTime, LocalTime endTime, String lineType) {
        TempTimeCardLine tempTimeCardLine = new TempTimeCardLine();

        tempTimeCardLine.setEmployee(employee);
        tempTimeCardLine.setProject(project);
        tempTimeCardLine.setTypeSelect(lineType);

        tempTimeCardLine.setStartDateTime(LocalDateTime.of(date, startTime));
        tempTimeCardLine.setEndDateTime(LocalDateTime.of(date, endTime));

        return tempTimeCardLine;
    }

    public List<TempTimeCardLine> generateTempTimeCardLineAbsence(Employee employee, Project project, LocalDate date, List<LeaveRequest> leaveRequests, TempTimeCardLine tempTimeCardLine) {
        List<TempTimeCardLine> tempTimeCardLinesAbsence = new ArrayList<>();

        for (LeaveRequest leaveRequest : leaveRequests) {
            LocalDateTime fromDateTTCL = tempTimeCardLine.getStartDateTime();
            LocalDateTime toDateTTCL = tempTimeCardLine.getEndDateTime();

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
            TempTimeCardLine ttcl = null;
            if ((fromDateTTCL.equals(fromDateLR) || fromDateTTCL.isAfter(fromDateLR)) && (fromDateTTCL.isBefore(toDateLR) || fromDateTTCL.equals(toDateLR))) {
                LocalTime startTime = fromDateTTCL.toLocalTime();
                LocalTime endTime;
                if ((toDateTTCL.isBefore(toDateLR) || toDateTTCL.equals(toDateLR))) {
                    endTime = toDateTTCL.toLocalTime();
                } else {
                    endTime = toDateLR.toLocalTime();
                }
                ttcl = generateTempTimeCardLine(employee, project, date, startTime, endTime, TimeCardLineRepository.TYPE_ABSENCE);
            } else if (fromDateTTCL.isBefore(fromDateLR) && toDateTTCL.isAfter(fromDateLR) && (toDateTTCL.isBefore(toDateLR) || toDateTTCL.equals(toDateLR))) {
                LocalTime startTime = fromDateLR.toLocalTime();
                LocalTime endTime = toDateTTCL.toLocalTime();
                ttcl = generateTempTimeCardLine(employee, project, date, startTime, endTime, TimeCardLineRepository.TYPE_ABSENCE);
            } else if (fromDateTTCL.isBefore(fromDateLR) && toDateTTCL.isAfter(toDateLR)) {
                LocalTime startTime = fromDateLR.toLocalTime();
                LocalTime endTime = toDateLR.toLocalTime();
                ttcl = generateTempTimeCardLine(employee, project, date, startTime, endTime, TimeCardLineRepository.TYPE_ABSENCE);
            }

            if (ttcl != null) {
                tempTimeCardLinesAbsence.add(ttcl);
                tempTimeCardLineRepo.save(ttcl);
            }
        }

        return tempTimeCardLinesAbsence;
    }

}
