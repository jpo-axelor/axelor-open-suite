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

import com.axelor.apps.timecard.db.Timecard;
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import java.time.LocalDateTime;
import javax.persistence.PersistenceException;

public class TimecardTimecardRepository extends TimecardRepository {

  @Override
  public Timecard save(Timecard timecard) {
    if (timecard.getEmployee().getMainEmploymentContract() == null) {
      throw new PersistenceException(
          new AxelorException(
              timecard,
              TraceBackRepository.CATEGORY_MISSING_FIELD,
              I18n.get("Please configure a main employement contract for employee %s."),
              timecard.getEmployee().getName()));
    }

    // Compute full name
    timecard.setFullName(timecard.getEmployee().getName() + " - " + timecard.getPeriod().getName());

    if (timecard.getTimecardLineList() != null) {
      // Compute start/end dateTime to be able to use 'orderBy' in the TimecardLine list
      for (TimecardLine timecardLine : timecard.getTimecardLineList()) {
        timecardLine.setStartDateTime(
            LocalDateTime.of(timecardLine.getDate(), timecardLine.getStartTime()));
        timecardLine.setEndDateTime(
            LocalDateTime.of(timecardLine.getDate(), timecardLine.getEndTime()));
      }
    }

    return super.save(timecard);
  }
}
