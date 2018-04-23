package com.axelor.apps.timecard.db.repo;

import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.apps.timecard.service.TimeCardLineService;
import com.axelor.inject.Beans;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class TimeCardLineTimeCardRepository extends TimeCardLineRepository {

    @Override
    public TimeCardLine save(TimeCardLine timeCardLine) {
        LocalTime startTime = timeCardLine.getStartTime();
        LocalTime endTime = timeCardLine.getEndTime();

        timeCardLine.setStartDateTime(LocalDateTime.of(timeCardLine.getDate(), startTime));
        timeCardLine.setEndDateTime(LocalDateTime.of(timeCardLine.getDate(), endTime));

        timeCardLine.setDuration(BigDecimal.valueOf(Duration.between(startTime, endTime).toMinutes() / 60.0));

        timeCardLine.setDurationNight(Beans.get(TimeCardLineService.class).getDurationNight(startTime, endTime, timeCardLine.getEmployee().getMainEmploymentContract().getPayCompany()));

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
