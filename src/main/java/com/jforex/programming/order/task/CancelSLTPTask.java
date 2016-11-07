package com.jforex.programming.order.task;

import java.util.Collection;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.jforex.programming.order.event.OrderEvent;
import com.jforex.programming.order.task.params.TaskParamsUtil;
import com.jforex.programming.order.task.params.position.BatchCancelSLAndTPParams;
import com.jforex.programming.order.task.params.position.ComplexMergePositionParams;

import io.reactivex.Observable;

public class CancelSLTPTask {

    private final BatchChangeTask batchChangeTask;

    public CancelSLTPTask(final BatchChangeTask batchChangeTask) {
        this.batchChangeTask = batchChangeTask;
    }

    public Observable<OrderEvent> observe(final Collection<IOrder> toCancelSLTPOrders,
                                          final ComplexMergePositionParams mergeParams) {
        return Observable.defer(() -> toCancelSLTPOrders.size() < 2
                ? Observable.empty()
                : createTask(toCancelSLTPOrders, mergeParams));
    }

    private Observable<OrderEvent> createTask(final Collection<IOrder> toCancelSLTPOrders,
                                              final ComplexMergePositionParams complexMergeParams) {
        final BatchCancelSLAndTPParams batchCancelSLAndTPParams = complexMergeParams.batchCancelSLAndTPParams();
        final Instrument instrument = toCancelSLTPOrders.iterator().next().getInstrument();

        final Observable<OrderEvent> cancelSL =
                TaskParamsUtil.composePositionTask(instrument,
                                                   Observable.defer(() -> batchChangeTask.cancelSL(toCancelSLTPOrders,
                                                                                                   batchCancelSLAndTPParams
                                                                                                       .batchCancelSLParams()
                                                                                                       .cancelSLParams(),
                                                                                                   batchCancelSLAndTPParams
                                                                                                       .batchCancelSLParams()
                                                                                                       .batchMode())),
                                                   batchCancelSLAndTPParams.batchCancelSLParams());
        final Observable<OrderEvent> cancelTP =
                TaskParamsUtil.composePositionTask(instrument,
                                                   Observable.defer(() -> batchChangeTask.cancelTP(toCancelSLTPOrders,
                                                                                                   batchCancelSLAndTPParams
                                                                                                       .batchCancelTPParams()
                                                                                                       .cancelTPParams(),
                                                                                                   batchCancelSLAndTPParams
                                                                                                       .batchCancelTPParams()
                                                                                                       .batchMode())),
                                                   batchCancelSLAndTPParams.batchCancelTPParams());

        final Observable<OrderEvent> observable = arrangeObservables(cancelSL,
                                                                     cancelTP,
                                                                     batchCancelSLAndTPParams
                                                                         .mergeExecutionMode());

        return TaskParamsUtil.composePositionTask(instrument,
                                                  observable,
                                                  batchCancelSLAndTPParams);
    }

    private Observable<OrderEvent> arrangeObservables(final Observable<OrderEvent> cancelSL,
                                                      final Observable<OrderEvent> cancelTP,
                                                      final MergeExecutionMode executionMode) {
        if (executionMode == MergeExecutionMode.ConcatCancelSLAndTP)
            return cancelSL.concatWith(cancelTP);
        else if (executionMode == MergeExecutionMode.ConcatCancelTPAndSL)
            return cancelTP.concatWith(cancelSL);
        return cancelSL.mergeWith(cancelTP);
    }
}
