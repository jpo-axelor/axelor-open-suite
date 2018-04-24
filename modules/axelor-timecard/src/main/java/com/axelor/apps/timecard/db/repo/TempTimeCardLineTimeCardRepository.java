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
