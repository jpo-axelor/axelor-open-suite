package com.axelor.apps.timecard.service.app;

import com.axelor.apps.base.db.AppTimecard;
import com.axelor.apps.base.service.app.AppBaseService;

public interface AppTimecardService extends AppBaseService {

  AppTimecard getAppTimecard();
}
