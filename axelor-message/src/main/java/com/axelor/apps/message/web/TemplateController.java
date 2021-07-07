package com.axelor.apps.message.web;

import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.Template;
import com.axelor.apps.message.db.repo.TemplateRepository;
import com.axelor.apps.message.exception.IExceptionMessage;
import com.axelor.apps.message.service.TemplateService;
import com.axelor.apps.message.translation.ITranslation;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;

public class TemplateController {

  public void generateDraftMessage(ActionRequest request, ActionResponse response) {

    Context context = request.getContext();
    Template template = context.asType(Template.class);
    template = Beans.get(TemplateRepository.class).find(template.getId());
    MetaModel metaModel =
        Beans.get(MetaModelRepository.class)
            .all()
            .filter("self.fullName = ?", template.getReference())
            .fetchOne();
    try {
      Message message = Beans.get(TemplateService.class).generateDraftMessage(template, metaModel);
      response.setView(
          ActionView.define(I18n.get(ITranslation.MESSAGE_TEST_TEMPLATE))
              .model(Message.class.getName())
              .add("form", "message-form")
              .add("grid", "message-grid")
              .param("forceTitle", "true")
              .context("_message", message)
              .map());
    } catch (NumberFormatException | ClassNotFoundException | AxelorException e) {
      TraceBackService.trace(response, e);
    }
  }

  public void addItemToReferenceSelection(ActionRequest request, ActionResponse response) {
    MetaModel metaModel = (MetaModel) request.getContext().get("metaModel");
    metaModel = Beans.get(MetaModelRepository.class).find(metaModel.getId());
    Beans.get(TemplateService.class).addItemToReferenceSelection(metaModel);
    response.setNotify(I18n.get(IExceptionMessage.TEMPLATE_TEST_REFRESH));
  }
}
