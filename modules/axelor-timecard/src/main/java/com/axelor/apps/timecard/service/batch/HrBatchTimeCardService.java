package com.axelor.apps.timecard.service.batch;

import com.axelor.apps.base.db.Batch;
import com.axelor.apps.base.exceptions.IExceptionMessage;
import com.axelor.apps.hr.db.HrBatch;
import com.axelor.apps.hr.db.repo.HrBatchRepository;
import com.axelor.apps.hr.service.batch.HrBatchService;
import com.axelor.db.Model;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;

public class HrBatchTimeCardService extends HrBatchService {

    @Override
    public Batch run(Model batchModel) throws AxelorException {
        HrBatch hrBatch = (HrBatch) batchModel;
        Batch batch = null;

        switch (hrBatch.getActionSelect()) {
            case HrBatchRepository.ACTION_LEAVE_MANAGEMENT:
                batch = leaveManagement(hrBatch);
                break;
            case HrBatchRepository.ACTION_SENIORITY_LEAVE_MANAGEMENT:
                batch = seniorityLeaveManagement(hrBatch);
                break;
            case HrBatchRepository.ACTION_PAYROLL_PREPARATION_GENERATION:
                batch = payrollPreparationGeneration(hrBatch);
                break;
            case HrBatchRepository.ACTION_PAYROLL_PREPARATION_EXPORT:
                batch = payrollPreparationExport(hrBatch);
                break;
            case HrBatchRepository.ACTION_LEAVE_MANAGEMENT_RESET:
                batch = leaveManagementReset(hrBatch);
                break;
            case HrBatchRepository.ACTION_TIMECARD_GENERATION:
                batch = timecardGeneration(hrBatch);
                break;
            default:
                throw new AxelorException(IException.INCONSISTENCY, I18n.get(IExceptionMessage.BASE_BATCH_1), hrBatch.getActionSelect(), hrBatch.getCode());
        }

        return batch;
    }

    public Batch timecardGeneration(HrBatch hrBatch) {
        return Beans.get(BatchTimecardGeneration.class).run(hrBatch);
    }

}
