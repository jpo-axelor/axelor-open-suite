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
package com.axelor.apps.timecard.web;

import com.axelor.apps.base.db.Wizard;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.EmploymentContract;
import com.axelor.apps.hr.db.LeaveRequest;
import com.axelor.apps.hr.db.repo.EmployeeRepository;
import com.axelor.apps.hr.db.repo.EmploymentContractRepository;
import com.axelor.apps.hr.db.repo.LeaveRequestRepository;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.db.repo.ProjectRepository;
import com.axelor.apps.timecard.db.EmployeeSuggestion;
import com.axelor.apps.timecard.db.Planning;
import com.axelor.apps.timecard.db.Timecard;
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.apps.timecard.db.repo.PlanningRepository;
import com.axelor.apps.timecard.db.repo.TimecardLineRepository;
import com.axelor.apps.timecard.db.repo.TimecardRepository;
import com.axelor.apps.timecard.service.TimecardLineService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.axelor.team.db.Team;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class TimecardLineController {

  @Inject protected EmployeeRepository employeeRepo;

  /** Set defaults of {@code TimecardLine} in context. */
  public void setDefaults(ActionRequest request, ActionResponse response) {
    LocalDateTime startDateTime;
    LocalDateTime endDateTime;

    // From calendar
    startDateTime = (LocalDateTime) request.getContext().get("startDateTime");
    endDateTime = (LocalDateTime) request.getContext().get("endDateTime");

    // From action-view
    Integer timecardLineId = (Integer) request.getContext().get("_timecardLineId");
    TimecardLine timecardLineParent = null;
    if (timecardLineId != null) {
      timecardLineParent =
          Beans.get(TimecardLineRepository.class).find(Long.valueOf(timecardLineId));
      if (startDateTime == null || endDateTime == null) {
        startDateTime = timecardLineParent.getStartDateTime();
        endDateTime = timecardLineParent.getEndDateTime();
      }
    }

    if (startDateTime != null) {
      response.setValue("weekDay", startDateTime.getDayOfWeek().getValue());
      response.setValue("date", startDateTime.toLocalDate());
      response.setValue("startTime", startDateTime.toLocalTime());

      if (endDateTime != null) {
        if (endDateTime.toLocalDate().compareTo(startDateTime.toLocalDate()) > 0) {
          response.setValue("endTime", LocalTime.MIDNIGHT.minusSeconds(1));
        } else {
          response.setValue("endTime", endDateTime.toLocalTime());
        }
      }
    }

    Integer timecardId = (Integer) request.getContext().get("_timecardId");
    if (timecardId != null) {
      response.setValue(
          "timecard", Beans.get(TimecardRepository.class).find(Long.valueOf(timecardId)));
    }

    Integer projectId = (Integer) request.getContext().get("_projectId");
    if (projectId != null) {
      response.setValue(
          "project", Beans.get(ProjectRepository.class).find(Long.valueOf(projectId)));
    }

    Integer employeeId = (Integer) request.getContext().get("_employeeId");
    if (employeeId == null) {
      // from 'New' in Timecard O2M
      Context parentContext = request.getContext().getParent();
      if (parentContext != null) {
        Timecard timecard = parentContext.asType(Timecard.class);
        if (timecard != null) {
          Employee employee = timecard.getEmployee();
          EmploymentContract employmentContract = timecard.getEmploymentContract();
          if (employee != null) {
            employeeId = employee.getId().intValue();
          }
          if (employmentContract != null) {
            response.setValue(
                "employmentContract",
                Beans.get(EmploymentContractRepository.class).find(employmentContract.getId()));
          }
        }
      }
    }
    if (employeeId != null) {
      Employee employee = Beans.get(EmployeeRepository.class).find(Long.valueOf(employeeId));
      response.setValue("employee", employee);
      if (employee != null) {
        response.setValue("activity", employee.getProduct());
      }
    }

    Boolean isSubstitution = (Boolean) request.getContext().get("_isSubstitution");
    if (isSubstitution != null) {
      response.setValue("isSubstitution", isSubstitution);
      response.setValue("absenceTimecardLine", timecardLineParent);
      response.setValue("typeSelect", TimecardLineRepository.TYPE_EXTRA);
      response.setAttr("typeSelect", "readonly", true);
      response.setAttr(
          "employee",
          "domain",
          "self.id <> "
              + timecardLineParent.getEmployee().getId()
              + " AND self.id IN ("
              + String.join(",", this.extractEmployeeIds(timecardLineParent.getEmployee()))
              + ")");
    }

    Boolean isForecastTCL = (Boolean) request.getContext().get("_isForecastTCL");
    if (isForecastTCL != null) {
      response.setAttr("typeSelect", "required", true);
    }
  }

  /** Set defaults for substitution wizard form. */
  public void setWizardDefaults(ActionRequest request, ActionResponse response) {
    response.setAttr("$projects", "domain", "self.statusSelect = 2 AND self.isSite = true");

    Integer leaveRequestId = (Integer) request.getContext().get("_leaveRequestId");
    if (leaveRequestId != null) {
      LeaveRequest leaveRequest =
          Beans.get(LeaveRequestRepository.class).find(Long.valueOf(leaveRequestId));

      response.setValue("$startDate", leaveRequest.getFromDate());
      response.setValue("$endDate", leaveRequest.getToDate());

      Employee employee = leaveRequest.getUser().getEmployee();
      response.setValue("$employeeToReplace", employee);

      response.setAttr(
          "$employeeReplacing",
          "domain",
          "self.id <> "
              + employee.getId()
              + " AND self.id IN ("
              + String.join(",", this.extractEmployeeIds(employee))
              + ")");

      List<TimecardLine> timecardLines = leaveRequest.getTimecardLineList();
      Set<String> projectsIds = new HashSet<>();
      projectsIds.add("0");
      for (TimecardLine timecardLine : timecardLines) {
        projectsIds.add(timecardLine.getProject().getId().toString());
      }
      response.setAttr(
          "$projects",
          "domain",
          "self.statusSelect = 2 AND self.isSite = true AND self.id IN ("
              + String.join(",", projectsIds)
              + ")");
    }

    Integer planningId = (Integer) request.getContext().get("_planningId");
    if (planningId != null) {
      Planning planning = Beans.get(PlanningRepository.class).find(Long.valueOf(planningId));

      Employee employee = planning.getEmployee();
      if (employee != null) {
        response.setValue("$employeeToReplace", employee);
        response.setAttr(
            "$employeeReplacing",
            "domain",
            "self.id <> "
                + employee.getId()
                + " AND self.id IN ("
                + String.join(",", this.extractEmployeeIds(employee))
                + ")");
      } else {
        response.setAttr("$employeeToReplace", "readonly", false);
      }
    }

    Integer timecardId = (Integer) request.getContext().get("_timecardId");
    if (timecardId != null) {
      Timecard timecard = Beans.get(TimecardRepository.class).find(Long.valueOf(timecardId));

      Employee employee = timecard.getEmployee();
      response.setValue("$employeeToReplace", employee);
      response.setAttr(
          "$employeeReplacing",
          "domain",
          "self.id <> "
              + employee.getId()
              + " AND self.id IN ("
              + String.join(",", this.extractEmployeeIds(employee))
              + ")");
    }

    response.setValue("isContractual", false);
  }

  /** Generates 'extra' TimecardLines. */
  public void generateExtraTCL(ActionRequest request, ActionResponse response) {
    ProjectRepository projectRepo = Beans.get(ProjectRepository.class);
    EmployeeRepository employeeRepo = Beans.get(EmployeeRepository.class);

    Context context = request.getContext();

    List<Project> projects = new ArrayList<>();
    for (Object project : (List) context.get("projects")) {
      Map p = (Map) project;
      projects.add(projectRepo.find(Long.valueOf((Integer) p.get("id"))));
    }

    int total;
    try {
      total =
          Beans.get(TimecardLineService.class)
              .generateExtraTCL(
                  employeeRepo.find(
                      Long.valueOf(((Integer) ((Map) context.get("employeeToReplace")).get("id")))),
                  employeeRepo.find(
                      Long.valueOf(((Integer) ((Map) context.get("employeeReplacing")).get("id")))),
                  projects,
                  LocalDate.parse((String) context.get("startDate")),
                  LocalDate.parse((String) context.get("endDate")),
                  (Boolean) context.get("isContractual"));
    } catch (AxelorException e) {
      TraceBackService.trace(response, e);
      return;
    }

    response.setCanClose(true);
    response.setNotify(
        I18n.get(
            "{0} substitution line has been generated.",
            "{0} substitution line have been generated.", total));
  }

  /** Sets night duration in {@code TimecardLine} in context. */
  public void computeNightHours(ActionRequest request, ActionResponse response) {
    Context context = request.getContext();

    if (context.get("employee") == null) {
      return;
    }

    Employee employee =
        Beans.get(EmployeeRepository.class).find(((Employee) context.get("employee")).getId());

    if (employee.getMainEmploymentContract() == null) {
      TraceBackService.trace(
          response,
          new AxelorException(
              employee,
              TraceBackRepository.CATEGORY_MISSING_FIELD,
              I18n.get("Please configure a main employement contract for employee %s."),
              employee.getName()));
      return;
    }

    try {
      response.setValue(
          "durationNight",
          Beans.get(TimecardLineService.class)
              .getDurationNight(
                  (LocalTime) context.get("startTime"),
                  (LocalTime) context.get("endTime"),
                  employee.getMainEmploymentContract().getPayCompany()));
    } catch (AxelorException e) {
      TraceBackService.trace(response, e);
    }
  }

  /** Sets total substitution hours in {@code TimecardLine} in context. */
  public void computeSubstitutionsDuration(ActionRequest request, ActionResponse response) {
    TimecardLine timecardLine =
        Beans.get(TimecardLineRepository.class)
            .find(request.getContext().asType(TimecardLine.class).getId());
    BigDecimal total = Beans.get(TimecardLineService.class).getSubstitutionsDuration(timecardLine);
    response.setValue("totalSubstitutionHours", total);
  }

  /** Suggest a list of {@link Employee} based on projects of the context. */
  public void suggestEmployee(ActionRequest request, ActionResponse response) {
    TimecardLineService timecardLineService = Beans.get(TimecardLineService.class);
    Context context = request.getContext();

    Set<EmployeeSuggestion> employeeSuggestions = new HashSet<>();

    if (context.getContextClass().equals(Wizard.class)) {
      /*
       * From substitution wizard
       */
      List projects = (List) context.get("projects");
      if (projects == null || projects.isEmpty()) {
        response.setAlert(I18n.get("Please select at least one project."));
        return;
      }

      Long employeeToReplaceId =
          ((Integer) ((Map) context.get("employeeToReplace")).get("id")).longValue();

      for (Object project : projects) {
        Long projectId = ((Integer) ((Map) project).get("id")).longValue();

        Set<EmployeeSuggestion> newEmployeeSuggestions =
            timecardLineService.suggestEmployee(projectId, employeeToReplaceId, null);

        for (EmployeeSuggestion newES : newEmployeeSuggestions) {
          employeeSuggestions.removeIf(
              es ->
                  es.getEmployee().equals(newES.getEmployee())
                      && newES.getHasWorkedOnOneOfTheProjects());
          employeeSuggestions.add(newES);
        }
      }

    } else if (context.getContextClass().equals(TimecardLine.class)) {
      /*
       * From timecard line form
       */
      Project project = (Project) context.get("project");
      if (project == null) {
        response.setAlert(I18n.get("Please select a project."));
        return;
      }

      Long employeeToReplaceId =
          ((TimecardLine) context.get("absenceTimecardLine")).getEmployee().getId();
      Long projectId = project.getId();
      LocalDate date = (LocalDate) context.get("date");

      employeeSuggestions =
          timecardLineService.suggestEmployee(projectId, employeeToReplaceId, date);
    }

    // Set elements of the view
    response.setValue("$employeeSuggestionList", employeeSuggestions);

    Set<Long> employeeSuggestionsIds =
        employeeSuggestions
            .stream()
            .map(EmployeeSuggestion::getEmployee)
            .map(Employee::getId)
            .collect(Collectors.toSet());
    employeeSuggestionsIds.add(0L);

    response.setAttr("$employeeSuggestion", "value", "null");
    response.setAttr(
        "$employeeSuggestion",
        "domain",
        "self.id IN (" + StringUtils.join(employeeSuggestionsIds, ",") + ")");

    // Show suggestions
    response.setAttr("suggestionOffPanel", "hidden", true);
    response.setAttr("suggestionOnPanel", "hidden", false);
    response.setAttr("$employeeSuggestionList", "hidden", false);
  }

  /** Creates an absence for contractual {@link TimecardLine} in context. */
  public void createAbsence(ActionRequest request, ActionResponse response) {
    TimecardLine contractualTL =
        Beans.get(TimecardLineRepository.class)
            .find(request.getContext().asType(TimecardLine.class).getId());

    TimecardLine absenceTL =
        Beans.get(TimecardLineService.class).createAbsenceFromContractual(contractualTL);

    response.setReload(true);
    response.setView(
        ActionView.define(I18n.get("Timecard line"))
            .model(TimecardLine.class.getName())
            .add("form", "timecard-line-form")
            .param("popup", "true")
            .param("popup-save", "true")
            .param("show-toolbar", "false")
            .param("forceEdit", "true")
            .context("_showRecord", absenceTL.getId())
            .map());
  }

  private Set<String> extractEmployeeIds(Employee employee) {
    Set<Team> teamSet = employee.getTeamSet();
    Set<String> employeeIds = new HashSet<>();
    for (Team team : teamSet) {
      employeeIds.addAll(
          employeeRepo
              .all()
              .filter("?1 MEMBER OF self.teamSet", team.getId())
              .fetch()
              .stream()
              .map(Employee::getId)
              .map(it -> it.toString())
              .collect(Collectors.toList()));
    }
    return employeeIds;
  }
}
