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
package com.axelor.apps.timecard.service.batch;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.service.administration.AbstractBatch;
import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.Template;
import com.axelor.apps.message.db.repo.MessageRepository;
import com.axelor.apps.message.service.MessageService;
import com.axelor.apps.message.service.TemplateMessageService;
import com.axelor.apps.timecard.db.Timecard;
import com.axelor.apps.timecard.db.TimecardBatch;
import com.axelor.apps.timecard.db.repo.TimecardRepository;
import com.axelor.apps.timecard.exception.IExceptionMessage;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchTimecardCheckMonthlyHours extends AbstractBatch {

  @Inject private TimecardRepository timecardRepository;
  @Inject private TemplateMessageService templateMessageService;

  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  public BatchTimecardCheckMonthlyHours() {
    super();
  }

  @Override
  @Transactional
  protected void process() {

    TimecardBatch timecardBatch = batch.getTimecardBatch();
    Company company = timecardBatch.getCompany();

    Query<Timecard> query =
        timecardRepository
            .all()
            .filter("self.company = :company AND self.statusSelect = :draftStatus")
            .bind("company", company)
            .bind("draftStatus", TimecardRepository.STATUS_DRAFT);

    List<Timecard> timecardList;
    int offset = 0;
    while (!(timecardList = query.fetch(FETCH_LIMIT, offset)).isEmpty()) {
      int count = 0;
      findBatch();

      for (Timecard timecard : timecardList) {
        try {
          if (timecard.getComplementOfHours() != null
              && timecard.getComplementOfHours().signum() > 0
              && timecard.getNbOfSentMessages() < 2) {
            log.debug(
                "Timecard n°"
                    + timecard.getId()
                    + " has a complement of hours and will generate a warning message.");

            Template template = timecardBatch.getTemplate();
            Message message = templateMessageService.generateMessage(timecard, template);

            Beans.get(MessageRepository.class).save(message);
            Beans.get(MessageService.class).sendMessage(message);

            timecard.setNbOfSentMessages(timecard.getNbOfSentMessages() + 1);
            incrementDone();
          }
        } catch (Exception e) {
          TraceBackService.trace(e);

          incrementAnomaly();
          break;
        } finally {
          count++;
        }
      }

      offset += count;
      JPA.clear();
    }
  }

  /**
   * As {@code batch} entity can be detached from the session, call {@code Batch.find()} get the
   * entity in the persistant context. Warning : {@code batch} entity have to be saved before.
   */
  @Override
  protected void stop() {

    String comment = I18n.get(IExceptionMessage.BATCH_TIMECARD_1);
    comment +=
        String.format(
            "\t* %s " + I18n.get(IExceptionMessage.BATCH_TIMECARD_2) + "\n", batch.getDone());
    comment +=
        String.format(
            I18n.get(com.axelor.apps.base.exceptions.IExceptionMessage.ALARM_ENGINE_BATCH_4),
            batch.getAnomaly());

    super.stop();
    addComment(comment);
  }
}
