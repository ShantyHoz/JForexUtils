package com.jforex.programming.position;

import java.util.Set;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.jforex.programming.order.OrderTask;
import com.jforex.programming.order.event.OrderEvent;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class PositionTask {

    private final OrderTask orderTask;
    private final PositionFactory positionFactory;

    public PositionTask(final OrderTask orderTask,
                        final PositionFactory positionFactory) {
        this.orderTask = orderTask;
        this.positionFactory = positionFactory;
    }

    public PositionOrders positionOrders(final Instrument instrument) {
        return positionFactory.forInstrument(instrument);
    }

    public Observable<OrderEvent> merge(final Instrument instrument,
                                        final String mergeOrderLabel) {
        return Observable.defer(() -> {
            final Set<IOrder> toMergeOrders = filledOrders(instrument);
            return toMergeOrders.size() < 2
                    ? Observable.empty()
                    : orderTask.mergeOrders(mergeOrderLabel, toMergeOrders);
        });
    }

    public Observable<OrderEvent> close(final ClosePositionCommand command) {
        final Instrument instrument = command.instrument();
        final String mergeOrderLabel = command.mergeOrderLabel();

        final Observable<OrderEvent> mergeObservable =
                merge(instrument, mergeOrderLabel).compose(command.mergeComposer());
        final Observable<OrderEvent> closeObservable = batch(instrument, order -> orderTask
            .close(order)
            .compose(command.closeComposer(order)));

        return mergeObservable.concatWith(closeObservable);
    }

    private final Observable<OrderEvent> batch(final Instrument instrument,
                                               final Function<IOrder, Observable<OrderEvent>> batchTask) {
        return Observable.defer(() -> Observable
            .fromIterable(filledOrOpenedOrders(instrument))
            .flatMap(batchTask::apply));
    }

    private final Set<IOrder> filledOrders(final Instrument instrument) {
        return positionOrders(instrument).filled();
    }

    private final Set<IOrder> filledOrOpenedOrders(final Instrument instrument) {
        return positionOrders(instrument).filledOrOpened();
    }
}
