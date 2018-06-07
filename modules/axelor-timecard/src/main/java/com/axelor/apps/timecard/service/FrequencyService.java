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

import com.axelor.apps.timecard.db.Frequency;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;

public interface FrequencyService {

  /**
   * Computes summary of given {@link Frequency}.
   *
   * @return Summary of given {@link Frequency}
   */
  String computeSummary(Frequency frequency);

  /**
   * Retrieves all possible dates for given {@link Frequency} and {@code year} (or current year if
   * null). If fourth and last day of week are checked in given {@link Frequency} and it is the same
   * date, it will only appear once in return list.
   */
  List<LocalDate> getDates(Frequency frequency, @Nullable Integer year);

  /** Retrieves months checked in given {@link Frequency}. */
  List<Integer> getMonths(Frequency frequency);

  /** Retrieves days of week checked in given {@link Frequency}. */
  List<Integer> getDays(Frequency frequency);

  /** Retrieves occurences checked in given {@link Frequency}. */
  List<Integer> getOccurences(Frequency frequency);
}
