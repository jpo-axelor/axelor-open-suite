package com.axelor.apps.base.web;

import com.axelor.apps.base.db.PrintTemplateLineTest;
import com.axelor.apps.base.db.repo.PrintTemplateLineTestRepository;
import com.axelor.apps.base.service.PrintTemplateLineService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import java.io.IOException;
import java.util.LinkedHashMap;

public class PrintTemplateLineController {

  public void checkTemplateLineExpression(ActionRequest request, ActionResponse response)
      throws ClassNotFoundException, AxelorException, IOException {

    Context context = request.getContext();
    PrintTemplateLineTest printTemplateLineTest = context.asType(PrintTemplateLineTest.class);
    printTemplateLineTest =
        Beans.get(PrintTemplateLineTestRepository.class).find(printTemplateLineTest.getId());
    MetaModel metaModel =
        Beans.get(MetaModelRepository.class)
            .all()
            .filter("self.fullName = ?", printTemplateLineTest.getReference())
            .fetchOne();
    Beans.get(PrintTemplateLineService.class)
        .checkExpression(
            Long.parseLong(printTemplateLineTest.getReferenceId().toString()),
            metaModel,
            printTemplateLineTest.getPrintTemplateLine());
    response.setReload(true);
  }

  @SuppressWarnings("unchecked")
  public void addItemToReferenceSelection(ActionRequest request, ActionResponse response) {
    Context context = request.getContext();
    LinkedHashMap<String, Object> metaModelMap =
        (LinkedHashMap<String, Object>) context.get("metaModel");
    Long metaModelId = Long.parseLong(metaModelMap.get("id").toString());
    MetaModel metaModel = Beans.get(MetaModelRepository.class).find(metaModelId);
    Beans.get(PrintTemplateLineService.class).addItemToReferenceSelection(metaModel);
    response.setReload(true);
  }
}
