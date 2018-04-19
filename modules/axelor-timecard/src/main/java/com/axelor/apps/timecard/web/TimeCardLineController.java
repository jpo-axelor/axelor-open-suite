package com.axelor.apps.timecard.web;

import com.axelor.apps.hr.db.repo.EmployeeRepository;
import com.axelor.apps.project.db.repo.ProjectRepository;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.db.repo.TimeCardLineRepository;
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
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        // From calendar
        startDateTime = (LocalDateTime)request.getContext().get("startDateTime");
        endDateTime = (LocalDateTime)request.getContext().get("endDateTime");

        // From action-view
        Integer timeCardLineId = (Integer)request.getContext().get("_id");
        TimeCardLine timeCardLineParent = Beans.get(TimeCardLineRepository.class).find(Long.valueOf(timeCardLineId));
        if (startDateTime == null || endDateTime == null) {
            startDateTime = timeCardLineParent.getStartDateTime();
            endDateTime = timeCardLineParent.getEndDateTime();
        }

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


        Boolean isSubstitution = (Boolean)request.getContext().get("_isSubstitution");
        if (isSubstitution != null) {
            response.setValue("isSubstitution", isSubstitution);
            response.setValue("absenceTimeCardLine", timeCardLineParent);
            response.setValue("typeSelect", TimeCardLineRepository.TYPE_EXTRA);
            response.setAttr("typeSelect", "readonly", true);
        }
    }

}
