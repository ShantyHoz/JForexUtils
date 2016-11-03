package com.jforex.programming.order.task;

import static com.jforex.programming.order.OrderStaticUtil.isAmountSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isGTTSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isLabelSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isOpenPriceSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isSLSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isTPSetTo;

import java.util.Collection;

import com.dukascopy.api.IOrder;
import com.jforex.programming.math.CalculationUtil;
import com.jforex.programming.order.OrderParams;
import com.jforex.programming.order.OrderStaticUtil;
import com.jforex.programming.order.OrderUtilHandler;
import com.jforex.programming.order.call.OrderCallReason;
import com.jforex.programming.order.event.OrderEvent;
import com.jforex.programming.order.task.params.CloseParams;
import com.jforex.programming.order.task.params.SetSLParams;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class BasicTask {

    private final TaskExecutor taskExecutor;
    private final OrderUtilHandler orderUtilHandler;
    private final CalculationUtil calculationUtil;

    public BasicTask(final TaskExecutor orderTaskExecutor,
                     final OrderUtilHandler orderUtilHandler,
                     final CalculationUtil calculationUtil) {
        this.taskExecutor = orderTaskExecutor;
        this.orderUtilHandler = orderUtilHandler;
        this.calculationUtil = calculationUtil;
    }

    public Observable<OrderEvent> submitOrder(final OrderParams orderParams) {
        final OrderCallReason callReason = orderParams.orderCommand().isConditional()
                ? OrderCallReason.SUBMIT_CONDITIONAL
                : OrderCallReason.SUBMIT;

        return Observable.defer(() -> taskExecutor
            .submitOrder(orderParams)
            .toObservable()
            .flatMap(order -> orderUtilObservable(order, callReason)));
    }

    public Observable<OrderEvent> mergeOrders(final String mergeOrderLabel,
                                              final Collection<IOrder> toMergeOrders) {
        return Observable
            .just(toMergeOrders)
            .filter(orders -> orders.size() >= 2)
            .flatMap(orders -> taskExecutor
                .mergeOrders(mergeOrderLabel, orders)
                .toObservable()
                .flatMap(order -> orderUtilObservable(order, OrderCallReason.MERGE)));
    }

    public Observable<OrderEvent> close(final IOrder orderToClose) {
        return close(CloseParams
            .newBuilder(orderToClose)
            .build());
    }

    public Observable<OrderEvent> close(final CloseParams closeParams) {
        return Observable
            .just(closeParams.order())
            .filter(order -> !OrderStaticUtil.isClosed.test(order))
            .flatMap(order -> evalCloseParmas(order, closeParams)
                .andThen(orderUtilObservable(order, OrderCallReason.CLOSE)));
    }

    private Completable evalCloseParmas(final IOrder orderToClose,
                                        final CloseParams closeParams) {
        return closeParams.maybePrice().isPresent()
                ? taskExecutor
                    .close(orderToClose,
                           closeParams.amount(),
                           closeParams.maybePrice().get(),
                           closeParams.slippage())
                : taskExecutor
                    .close(orderToClose,
                           closeParams.amount());
    }

    public Observable<OrderEvent> setLabel(final IOrder orderToSetLabel,
                                           final String label) {
        return Observable
            .just(orderToSetLabel)
            .filter(order -> !isLabelSetTo(label).test(order))
            .flatMap(order -> taskExecutor
                .setLabel(order, label)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_LABEL)));
    }

    public Observable<OrderEvent> setGoodTillTime(final IOrder orderToSetGTT,
                                                  final long newGTT) {
        return Observable
            .just(orderToSetGTT)
            .filter(order -> !isGTTSetTo(newGTT).test(order))
            .flatMap(order -> taskExecutor
                .setGoodTillTime(order, newGTT)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_GTT)));
    }

    public Observable<OrderEvent> setRequestedAmount(final IOrder orderToSetAmount,
                                                     final double newRequestedAmount) {
        return Observable
            .just(orderToSetAmount)
            .filter(order -> !isAmountSetTo(newRequestedAmount).test(order))
            .flatMap(order -> taskExecutor
                .setRequestedAmount(order, newRequestedAmount)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_AMOUNT)));
    }

    public Observable<OrderEvent> setOpenPrice(final IOrder orderToSetOpenPrice,
                                               final double newOpenPrice) {
        return Observable
            .just(orderToSetOpenPrice)
            .filter(order -> !isOpenPriceSetTo(newOpenPrice).test(order))
            .flatMap(order -> taskExecutor
                .setOpenPrice(order, newOpenPrice)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_PRICE)));
    }

    public Observable<OrderEvent> setStopLossPrice(final IOrder orderToSetSL,
                                                   final double newSL) {
        return setStopLossPrice(SetSLParams
            .newBuilder(orderToSetSL, newSL)
            .build());
    }

    public Observable<OrderEvent> setStopLossForPips(final IOrder order,
                                                     final double pips) {
        return Observable.defer(() -> {
            final double slPrice = calculationUtil.slPriceForPips(order, pips);
            return setStopLossPrice(order, slPrice);
        });
    }

    public Observable<OrderEvent> setStopLossPrice(final SetSLParams setSLParams) {
        return Observable
            .just(setSLParams.order())
            .filter(order -> !isSLSetTo(setSLParams.newSL()).test(order))
            .flatMap(order -> taskExecutor
                .setStopLossPrice(order,
                                  setSLParams.newSL(),
                                  setSLParams.offerSide(),
                                  setSLParams.trailingStep())
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_SL)));
    }

    public Observable<OrderEvent> setTakeProfitPrice(final IOrder orderToSetTP,
                                                     final double newTP) {
        return Observable
            .just(orderToSetTP)
            .filter(order -> !isTPSetTo(newTP).test(order))
            .flatMap(order -> taskExecutor
                .setTakeProfitPrice(order, newTP)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_TP)));
    }

    public Observable<OrderEvent> setTakeProfitForPips(final IOrder order,
                                                       final double pips) {
        return Observable.defer(() -> {
            final double tpPrice = calculationUtil.tpPriceForPips(order, pips);
            return setTakeProfitPrice(order, tpPrice);
        });
    }

    private final Observable<OrderEvent> orderUtilObservable(final IOrder order,
                                                             final OrderCallReason orderCallReason) {
        return orderUtilHandler.callObservable(order, orderCallReason);
    }
}
