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

import com.axelor.apps.hr.db.LeaveRequest;
import com.axelor.apps.hr.db.repo.LeaveRequestRepository;
import com.axelor.apps.timecard.db.TimeCardLine;

import java.math.BigDecimal;

public class LeaveRequestTimeCardRepository extends LeaveRequestRepository {

    @Override
    public LeaveRequest save(LeaveRequest leaveRequest) {
        BigDecimal totalAbsence = BigDecimal.ZERO;
        BigDecimal totalSubstitution = BigDecimal.ZERO;

        if (leaveRequest.getTimeCardLineList() != null) {
            for (TimeCardLine timeCardLine : leaveRequest.getTimeCardLineList()) {
                totalAbsence = totalAbsence.add(timeCardLine.getDuration());
                totalSubstitution = totalSubstitution.add(timeCardLine.getTotalSubstitutionHours());
            }
        }

        leaveRequest.setTotalAbsenceHours(totalAbsence);
        leaveRequest.setTotalSubstitutionHours(totalSubstitution);

        return super.save(leaveRequest);
    }
}
