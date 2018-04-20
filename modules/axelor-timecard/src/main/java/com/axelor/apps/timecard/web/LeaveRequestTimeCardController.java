package com.axelor.apps.timecard.web;

import com.axelor.apps.hr.db.LeaveRequest;
import com.axelor.apps.hr.db.repo.LeaveRequestRepository;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import java.util.ArrayList;
import java.util.List;

public class LeaveRequestTimeCardController {

    public void showCalendar(ActionRequest request, ActionResponse response) {
        Long leaveRequestId = (Long) request.getContext().get("id");
        LeaveRequest leaveRequest = Beans.get(LeaveRequestRepository.class).find(leaveRequestId);

        List<String> substitutions = new ArrayList<>();
        substitutions.add("0");
        for (TimeCardLine timeCardLine : leaveRequest.getTimeCardLineList()) {
            substitutions.add(timeCardLine.getId().toString());
        }

        ActionViewBuilder avb = ActionView.define("Demande de congés - " + leaveRequest.getUser().getEmployee().getName())
                                          .model(TimeCardLine.class.getName())
                                          .add("calendar", "time-card-line-leave-request-calendar")
                                          .domain("self.leaveRequest.id = " + leaveRequestId + " OR self.absenceTimeCardLine.id IN (" + String.join(",", substitutions) + ")")
                                          .context("calendarDate", leaveRequest.getFromDate());

        response.setView(avb.map());
    }

}
