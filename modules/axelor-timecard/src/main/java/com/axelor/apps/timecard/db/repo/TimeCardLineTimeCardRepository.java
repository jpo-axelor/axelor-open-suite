package com.axelor.apps.timecard.db.repo;

import com.axelor.apps.timecard.db.TimeCardLine;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class TimeCardLineTimeCardRepository extends TimeCardLineRepository {

    @Override
    public TimeCardLine save(TimeCardLine timeCardLine) {
        timeCardLine.setStartDateTime(LocalDateTime.of(timeCardLine.getDate(), timeCardLine.getStartTime()));
        timeCardLine.setEndDateTime(LocalDateTime.of(timeCardLine.getDate(), timeCardLine.getEndTime()));
        timeCardLine.setDuration(BigDecimal.valueOf(Duration.between(timeCardLine.getStartTime(), timeCardLine.getEndTime()).toMinutes() / 60));

        List<TimeCardLine> tcls = timeCardLine.getSubstitutionTimeCardLineList();
        if (tcls != null) {
            BigDecimal totalSubstitution = BigDecimal.ZERO;
            for (TimeCardLine tcl : tcls) {
                totalSubstitution = totalSubstitution.add(tcl.getDuration());
            }
            timeCardLine.setTotalSubstitutionHours(totalSubstitution);
        }

        return super.save(timeCardLine);
    }
}
