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
import com.axelor.inject.Beans;
import javax.persistence.PersistenceException;

public class TimecardTimecardRepository extends TimecardRepository {

  @Override
  public Timecard save(Timecard timecard) {
    if (timecard.getTimecardLineList() != null) {
      for (TimecardLine timecardLine : timecard.getTimecardLineList()) {
        Beans.get(TimecardLineRepository.class).save(timecardLine);
      }
    }

    if (timecard.getEmployee().getMainEmploymentContract() == null) {
      throw new PersistenceException(
          new AxelorException(
              timecard,
              TraceBackRepository.CATEGORY_MISSING_FIELD,
              I18n.get(
                  "Please configure a main employement contract for employee "
                      + timecard.getEmployee().getName())));
    }

    // Compute full name
    timecard.setFullName(
        I18n.get("Timecard")
            + " - "
            + timecard.getEmployee().getName()
            + " - "
            + timecard.getPeriod().getName());

    return super.save(timecard);
  }
}
