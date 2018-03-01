package com.axelor.apps.timecard.db.repo;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.timecard.db.TempTimeCardLine;

public class TempTimeCardLineTimeCardRepository extends TempTimeCardLineRepository {

    @Override
    public TempTimeCardLine save(TempTimeCardLine tempTimeCardLine) {
        tempTimeCardLine.setFullName(computeFullName(tempTimeCardLine));

        return super.save(tempTimeCardLine);
    }

    protected String computeFullName(TempTimeCardLine tempTimeCardLine) {
        StringBuilder fullName = new StringBuilder();

        String typeSelect = tempTimeCardLine.getTypeSelect();
        if (typeSelect != null) {
            switch (typeSelect) {
                case TYPE_CONTRACTUAL:
                    fullName.append("[C]");
                    break;
                case TYPE_EXTRA:
                    fullName.append("[+]");
                    break;
                case TYPE_ABSENCE:
                    fullName.append("[A]");
                    break;
            }
        }

        Employee employee = tempTimeCardLine.getEmployee();
        if (employee != null) {
            fullName.append(" - ");
            fullName.append(employee.getContactPartner().getName());
        }

        Project project = tempTimeCardLine.getProject();
        if (project != null) {
            fullName.append(" - ");
            fullName.append(project.getName());
        }

        return fullName.toString();
    }
}
