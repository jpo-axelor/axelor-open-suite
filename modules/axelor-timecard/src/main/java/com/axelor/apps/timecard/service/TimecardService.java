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
package com.axelor.apps.timecard.service;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.timecard.db.Timecard;
import com.axelor.apps.timecard.db.TimecardLine;
import com.axelor.exception.AxelorException;
import java.math.BigDecimal;

public interface TimecardService {

  /** Closes given {@link Timecard} */
  void close(Timecard timecard);

  /** Generates {@link TimecardLine}s for given {@link Timecard}, after deleting existing ones. */
  void generateTimecardLines(Timecard timecard);

  /** Attaches previously (orphan) generated {@link TimecardLine}s to given {@link Timecard}. */
  void attachScheduledTimecardLines(Timecard timecard);

  /** Computes hours and weekly hours for given {@link Timecard}. */
  void computeAll(Timecard timecard) throws AxelorException;

  /** Computes hours for given {@link Timecard}. */
  void computeHours(Timecard timecard) throws AxelorException;

  /** Computes weekly hours for given {@link Timecard}. */
  void computeWeeklyHours(Timecard timecard);

  /** Validates the given {@link Timecard}. */
  void validate(Timecard timecard) throws AxelorException;

  /** Refuses the given {@link Timecard}. */
  void refuse(Timecard timecard);

  /**
   * Computes {@code employee}'s total worked hours for given {@code weekOfYear} of {@code year}.
   */
  BigDecimal computeWorkedHours(Long timecardId, int year, int weekOfYear, Employee employee);
}
