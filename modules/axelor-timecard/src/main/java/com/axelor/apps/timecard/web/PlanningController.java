package com.axelor.apps.timecard.web;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.Planning;
import com.axelor.apps.timecard.db.TempTimeCardLine;
import com.axelor.apps.timecard.service.TempTimeCardLineService;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import java.util.List;

public class PlanningController {

    public void preview(ActionRequest request, ActionResponse response) {
        Planning planning = request.getContext().asType(Planning.class);
        Project project = planning.getProject();
        Employee employee = planning.getEmployee();

        List<TempTimeCardLine> tempTimeCardLines = Beans.get(TempTimeCardLineService.class).generateTempTimeCardLines(project, employee, planning.getStartDate(), planning.getEndDate());
        if (tempTimeCardLines.size() == 0) {
            response.setNotify("Pas d'évènements à afficher.");
            return;
        }

        ActionViewBuilder actionView = null;
        if (employee != null && project == null) {
            actionView = ActionView.define("Prévisualisation - " + employee.getName())
                                   .add("calendar", "temp-time-card-line-calendar-by-project")
                                   .domain("self.employee.id = :_employeeId")
                                   .context("_employeeId", employee.getId());
        } else if (employee == null && project != null) {
            actionView = ActionView.define("Prévisualisation - " + project.getName())
                                   .add("calendar", "temp-time-card-line-calendar-by-employee")
                                   .domain("self.project.id = :_projectId")
                                   .context("_projectId", project.getId());
        } else if (employee != null && project != null) {
            actionView = ActionView.define("Prévisualisation - " + employee.getName())
                                   .add("calendar", "temp-time-card-line-calendar-by-project")
                                   .domain("self.employee.id = :_employeeId AND self.project.id = :_projectId")
                                   .context("_employeeId", employee.getId())
                                   .context("_projectId", project.getId());
        }

        if (actionView != null) {
            response.setView(actionView.model(TempTimeCardLine.class.getName()).map());
        }
    }

}
