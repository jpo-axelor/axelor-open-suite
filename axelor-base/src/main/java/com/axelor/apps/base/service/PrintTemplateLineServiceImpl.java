/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2019 Axelor (<http://axelor.com>).
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
package com.axelor.apps.base.service;

import com.axelor.apps.base.db.PrintTemplateLine;
import com.axelor.apps.base.db.repo.PrintTemplateLineRepository;
import com.axelor.apps.message.service.TemplateContextService;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.repo.MetaSelectItemRepository;
import com.axelor.meta.db.repo.MetaSelectRepository;
import com.axelor.rpc.Context;
import com.axelor.tool.template.TemplateMaker;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PrintTemplateLineServiceImpl implements PrintTemplateLineService {

  protected static final char TEMPLATE_DELIMITER = '$';

  protected TemplateContextService templateContextService;
  protected PrintTemplateLineRepository printTemplateLineRepo;

  @Inject
  public PrintTemplateLineServiceImpl(
      TemplateContextService templateContextService,
      PrintTemplateLineRepository printTemplateLineRepo) {
    this.templateContextService = templateContextService;
    this.printTemplateLineRepo = printTemplateLineRepo;
  }

  @SuppressWarnings("unchecked")
  @Transactional
  @Override
  public void checkExpression(
      Long objectId, MetaModel metaModel, PrintTemplateLine printTemplateLine)
      throws AxelorException, IOException, ClassNotFoundException {
    if (metaModel == null) {
      return;
    }
    String model = metaModel.getFullName();
    String simpleModel = metaModel.getName();

    Context scriptContext = null;
    if (StringUtils.notEmpty(model)) {
      Class<? extends Model> modelClass = (Class<? extends Model>) Class.forName(model);
      Model modelObject = JPA.find(modelClass, objectId);
      if (ObjectUtils.notEmpty(modelObject)) {
        scriptContext = new Context(Mapper.toMap(modelObject), modelClass);
      }
    }
    Boolean present = true;
    if (StringUtils.notEmpty(printTemplateLine.getConditions())) {
      Object evaluation =
          templateContextService.computeTemplateContext(
              printTemplateLine.getConditions(), scriptContext);
      if (evaluation instanceof Boolean) {
        present = (Boolean) evaluation;
      } else {
        present = (Boolean) null;
      }
    }

    printTemplateLine.getPrintTemplateLineTest().setConditionsResult(present);

    if (present) {
      String resultOfTitle = null;
      String resultOfContent = null;
      TemplateMaker maker = initMaker(objectId, model, simpleModel);

      if (StringUtils.notEmpty(printTemplateLine.getTitle())) {
        maker.setTemplate(printTemplateLine.getTitle());
        resultOfTitle = maker.make() + " ";
      }
      if (StringUtils.notEmpty(printTemplateLine.getContent())) {
        maker.setTemplate(printTemplateLine.getContent());
        resultOfContent = maker.make();
        if (StringUtils.notEmpty(resultOfContent)) {
          resultOfContent = resultOfTitle + resultOfContent;
        }
      }
      printTemplateLine.getPrintTemplateLineTest().setContentResult(resultOfContent);
    }
    printTemplateLineRepo.save(printTemplateLine);
  }

  @SuppressWarnings("unchecked")
  private TemplateMaker initMaker(Long objectId, String model, String simpleModel)
      throws ClassNotFoundException {
    TemplateMaker maker = new TemplateMaker(Locale.FRENCH, TEMPLATE_DELIMITER, TEMPLATE_DELIMITER);

    Class<? extends Model> modelClass = (Class<? extends Model>) Class.forName(model);
    maker.setContext(JPA.find(modelClass, objectId), simpleModel);

    return maker;
  }

  @Override
  public void addItemToReferenceSelection(MetaModel model) {
    MetaSelect metaSelect =
        Beans.get(MetaSelectRepository.class)
            .findByName("print.template.line.test.reference.select");
    List<MetaSelectItem> items = metaSelect.getItems();
    if (items != null && !items.stream().anyMatch(x -> x.getValue().equals(model.getFullName()))) {
      MetaSelectItem metaSelectItem = new MetaSelectItem();
      metaSelectItem.setTitle(model.getName());
      metaSelectItem.setValue(model.getFullName());
      metaSelectItem.setSelect(metaSelect);
      saveMetaSelectItem(metaSelectItem);
    }
  }

  @Transactional
  public void saveMetaSelectItem(MetaSelectItem metaSelectItem) {
    Beans.get(MetaSelectItemRepository.class).save(metaSelectItem);
  }
}
