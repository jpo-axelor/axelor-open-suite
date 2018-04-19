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

        for (TimeCardLine timeCardLine : leaveRequest.getTimeCardLineList()) {
            totalAbsence = totalAbsence.add(timeCardLine.getDuration());
            totalSubstitution = totalSubstitution.add(timeCardLine.getTotalSubstitutionHours());
        }

        leaveRequest.setTotalAbsenceHours(totalAbsence);
        leaveRequest.setTotalSubstitutionHours(totalSubstitution);

        return super.save(leaveRequest);
    }
}
