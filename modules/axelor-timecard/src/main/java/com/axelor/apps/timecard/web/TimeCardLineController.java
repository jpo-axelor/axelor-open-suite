package com.axelor.apps.timecard.web;

import com.axelor.apps.hr.db.repo.EmployeeRepository;
import com.axelor.apps.project.db.repo.ProjectRepository;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeCardLineController {

    /**
     * Set defaults of {@code TimeCardLine} in context.
     *
     * @param request
     * @param response
     */
    public void setDefaults(ActionRequest request, ActionResponse response) {
        LocalDateTime startDateTime = (LocalDateTime)request.getContext().get("startDateTime");
        LocalDateTime endDateTime = (LocalDateTime)request.getContext().get("endDateTime");

        response.setValue("weekDay", startDateTime.getDayOfWeek().getValue());
        response.setValue("date", startDateTime.toLocalDate());
        response.setValue("startTime", startDateTime.toLocalTime());

        if (endDateTime.toLocalDate().compareTo(startDateTime.toLocalDate()) > 0) {
            response.setValue("endTime", LocalTime.MIDNIGHT.minusSeconds(1));
        } else {
            response.setValue("endTime", endDateTime.toLocalTime());
        }

        Integer projectId = (Integer)request.getContext().get("_projectId");
        if (projectId != null) {
            response.setValue("project", Beans.get(ProjectRepository.class).find(Long.valueOf(projectId)));
        }

        Integer employeeId = (Integer)request.getContext().get("_employeeId");
        if (employeeId != null) {
            response.setValue("employee", Beans.get(EmployeeRepository.class).find(Long.valueOf(employeeId)));
        }
    }

}
