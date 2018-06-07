package com.axelor.apps.timecard.service.app;

import com.axelor.apps.base.db.AppTimecard;
import com.axelor.apps.base.db.repo.AppTimecardRepository;
import com.axelor.apps.base.service.app.AppBaseServiceImpl;
import com.google.inject.Inject;

public class AppTimecardServiceImpl extends AppBaseServiceImpl implements AppTimecardService {

  private AppTimecardRepository appTimecardRepo;

  @Inject
  public AppTimecardServiceImpl(AppTimecardRepository appTimecardRepo) {
    this.appTimecardRepo = appTimecardRepo;
  }

  @Override
  public AppTimecard getAppTimecard() {
    return appTimecardRepo.all().fetchOne();
  }
}
