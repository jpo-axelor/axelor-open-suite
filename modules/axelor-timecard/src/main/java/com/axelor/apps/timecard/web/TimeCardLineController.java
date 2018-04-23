package com.axelor.apps.timecard.web;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.LeaveRequest;
import com.axelor.apps.hr.db.repo.EmployeeRepository;
import com.axelor.apps.hr.db.repo.LeaveRequestRepository;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.db.repo.ProjectRepository;
import com.axelor.apps.timecard.db.Planning;
import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.PlanningRepository;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
import com.axelor.apps.timecard.db.repo.TimeCardRepository;
import com.axelor.apps.timecard.service.TimeCardLineService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimeCardLineController {

    /**
     * Set defaults of {@code TimeCardLine} in context.
     *
     * @param request
     * @param response
     */
    public void setDefaults(ActionRequest request, ActionResponse response) {
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        // From calendar
        startDateTime = (LocalDateTime) request.getContext().get("startDateTime");
        endDateTime = (LocalDateTime) request.getContext().get("endDateTime");

        // From action-view
        Integer timeCardLineId = (Integer) request.getContext().get("_id");
        TimeCardLine timeCardLineParent = null;
        if (timeCardLineId != null) {
            timeCardLineParent = Beans.get(TimeCardLineRepository.class).find(Long.valueOf(timeCardLineId));
            if (startDateTime == null || endDateTime == null) {
                startDateTime = timeCardLineParent.getStartDateTime();
                endDateTime = timeCardLineParent.getEndDateTime();
            }
        }

        response.setValue("weekDay", startDateTime.getDayOfWeek().getValue());
        response.setValue("date", startDateTime.toLocalDate());
        response.setValue("startTime", startDateTime.toLocalTime());

        if (endDateTime.toLocalDate().compareTo(startDateTime.toLocalDate()) > 0) {
            response.setValue("endTime", LocalTime.MIDNIGHT.minusSeconds(1));
        } else {
            response.setValue("endTime", endDateTime.toLocalTime());
        }


        Integer projectId = (Integer) request.getContext().get("_projectId");
        if (projectId != null) {
            response.setValue("project", Beans.get(ProjectRepository.class).find(Long.valueOf(projectId)));
        }


        Integer employeeId = (Integer) request.getContext().get("_employeeId");
        if (employeeId != null) {
            response.setValue("employee", Beans.get(EmployeeRepository.class).find(Long.valueOf(employeeId)));
        }


        Boolean isSubstitution = (Boolean) request.getContext().get("_isSubstitution");
        if (isSubstitution != null) {
            response.setValue("isSubstitution", isSubstitution);
            response.setValue("absenceTimeCardLine", timeCardLineParent);
            response.setValue("typeSelect", TimeCardLineRepository.TYPE_EXTRA);
            response.setAttr("typeSelect", "readonly", true);
        }
    }

    /**
     * Set defaults for substitution wizard form.
     *
     * @param request
     * @param response
     */
    public void setWizardDefaults(ActionRequest request, ActionResponse response) {
        response.setAttr("$projects", "domain", "self.statusSelect = 2");

        Integer leaveRequestId = (Integer) request.getContext().get("_leaveRequestId");
        if (leaveRequestId != null) {
            LeaveRequest leaveRequest = Beans.get(LeaveRequestRepository.class).find(Long.valueOf(leaveRequestId));

            response.setValue("$startDate", leaveRequest.getFromDate());
            response.setValue("$endDate", leaveRequest.getToDate());

            Employee employee = leaveRequest.getUser().getEmployee();
            response.setValue("$employeeToReplace", employee);
            response.setAttr("$employeeReplacing", "domain", "self.id <> " + employee.getId());

            List<TimeCardLine> timeCardLines = leaveRequest.getTimeCardLineList();
            Set<String> projectsIds = new HashSet<>();
            projectsIds.add("0");
            for (TimeCardLine timeCardLine : timeCardLines) {
                projectsIds.add(timeCardLine.getProject().getId().toString());
            }
            response.setAttr("$projects", "domain", "self.statusSelect = 2 AND self.id IN (" + String.join(",", projectsIds) + ")");
        }

        Integer planningId = (Integer) request.getContext().get("_planningId");
        if (planningId != null) {
            Planning planning = Beans.get(PlanningRepository.class).find(Long.valueOf(planningId));

            Employee employee = planning.getEmployee();
            if (employee != null) {
                response.setValue("$employeeToReplace", employee);
                response.setAttr("$employeeReplacing", "domain", "self.id <> " + employee.getId());
            } else {
                response.setAttr("$employeeToReplace", "readonly", false);
            }
        }

        Integer timeCardId = (Integer) request.getContext().get("_timeCardId");
        if (timeCardId != null) {
            TimeCard timeCard = Beans.get(TimeCardRepository.class).find(Long.valueOf(timeCardId));

            Employee employee = timeCard.getEmployee();
            response.setValue("$employeeToReplace", employee);
            response.setAttr("$employeeReplacing", "domain", "self.id <> " + employee.getId());
        }

        response.setValue("isContractual", false);
    }

    /**
     * Generates 'extra' TimeCardLines.
     *
     * @param request
     * @param response
     */
    public void generateExtraTCL(ActionRequest request, ActionResponse response) {
        ProjectRepository projectRepo = Beans.get(ProjectRepository.class);
        EmployeeRepository employeeRepo = Beans.get(EmployeeRepository.class);

        Context context = request.getContext();

        List<Project> projects = new ArrayList<>();
        for (Object project : (List) context.get("projects")) {
            Map p = (Map) project;
            projects.add(projectRepo.find(Long.valueOf((Integer) p.get("id"))));
        }

        Beans.get(TimeCardLineService.class).generateExtraTCL(employeeRepo.find(Long.valueOf(((Integer) ((Map) context.get("employeeToReplace")).get("id")))),
                                                              employeeRepo.find(Long.valueOf(((Integer) ((Map) context.get("employeeReplacing")).get("id")))),
                                                              projects,
                                                              LocalDate.parse((String) context.get("startDate")),
                                                              LocalDate.parse((String) context.get("endDate")),
                                                              (Boolean) context.get("isContractual"));

        response.setCanClose(true);
    }

    /**
     * Sets night duration in {@code TimeCardLine} in context.
     *
     * @param request
     * @param response
     */
    public void computeNightHours(ActionRequest request, ActionResponse response) {
        Context context = request.getContext();

        Employee employee = Beans.get(EmployeeRepository.class).find(((Employee) context.get("employee")).getId());

        response.setValue("durationNight", Beans.get(TimeCardLineService.class).getDurationNight((LocalTime) context.get("startTime"), (LocalTime) context.get("endTime"), employee.getMainEmploymentContract().getPayCompany()));
    }

}
