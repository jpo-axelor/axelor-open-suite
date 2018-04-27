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

import com.axelor.apps.timecard.db.TimeCard;
import com.axelor.apps.timecard.db.TimeCardLine;
import com.axelor.inject.Beans;

public class TimeCardTimeCardRepository extends TimeCardRepository {

    @Override
    public TimeCard save(TimeCard timeCard) {
        if (timeCard.getTimeCardLineList() != null) {
            for (TimeCardLine timeCardLine : timeCard.getTimeCardLineList()) {
                Beans.get(TimeCardLineRepository.class).save(timeCardLine);
            }
        }

        return super.save(timeCard);
    }
}
