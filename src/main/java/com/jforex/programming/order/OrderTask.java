package com.jforex.programming.order;

import static com.jforex.programming.order.OrderStaticUtil.isAmountSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isClosed;
import static com.jforex.programming.order.OrderStaticUtil.isGTTSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isLabelSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isOpenPriceSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isSLSetTo;
import static com.jforex.programming.order.OrderStaticUtil.isTPSetTo;

import java.util.Collection;

import com.dukascopy.api.IOrder;
import com.jforex.programming.misc.JForexUtil;
import com.jforex.programming.order.call.OrderCallReason;
import com.jforex.programming.order.event.OrderEvent;
import com.jforex.programming.settings.PlatformSettings;

import io.reactivex.Observable;

public class OrderTask {

    private final OrderTaskExecutor orderTaskExecutor;
    private final OrderUtilHandler orderUtilHandler;

    private static final PlatformSettings platformSettings = JForexUtil.platformSettings;

    public OrderTask(final OrderTaskExecutor orderTaskExecutor,
                     final OrderUtilHandler orderUtilHandler) {
        this.orderTaskExecutor = orderTaskExecutor;
        this.orderUtilHandler = orderUtilHandler;
    }

    public Observable<OrderEvent> submitOrder(final OrderParams orderParams) {
        return orderTaskExecutor
            .submitOrder(orderParams)
            .toObservable()
            .flatMap(order -> orderUtilObservable(order, OrderCallReason.SUBMIT));
    }

    public Observable<OrderEvent> mergeOrders(final String mergeOrderLabel,
                                              final Collection<IOrder> toMergeOrders) {
        return orderTaskExecutor
            .mergeOrders(mergeOrderLabel, toMergeOrders)
            .toObservable()
            .flatMap(order -> orderUtilObservable(order, OrderCallReason.MERGE));
    }

    public Observable<OrderEvent> close(final IOrder orderToClose) {
        return Observable
            .just(orderToClose)
            .filter(order -> !isClosed.test(order))
            .flatMap(order -> orderTaskExecutor
                .close(order)
                .andThen(orderUtilObservable(order, OrderCallReason.CLOSE)));
    }

    public Observable<OrderEvent> setLabel(final IOrder orderToSetLabel,
                                           final String label) {
        return Observable
            .just(orderToSetLabel)
            .filter(order -> !isLabelSetTo(label).test(order))
            .flatMap(order -> orderTaskExecutor
                .setLabel(order, label)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_LABEL)));
    }

    public Observable<OrderEvent> setGoodTillTime(final IOrder orderToSetGTT,
                                                  final long newGTT) {
        return Observable
            .just(orderToSetGTT)
            .filter(order -> !isGTTSetTo(newGTT).test(order))
            .flatMap(order -> orderTaskExecutor
                .setGoodTillTime(order, newGTT)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_GTT)));
    }

    public Observable<OrderEvent> setRequestedAmount(final IOrder orderToSetAmount,
                                                     final double newRequestedAmount) {
        return Observable
            .just(orderToSetAmount)
            .filter(order -> !isAmountSetTo(newRequestedAmount).test(order))
            .flatMap(order -> orderTaskExecutor
                .setRequestedAmount(order, newRequestedAmount)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_AMOUNT)));
    }

    public Observable<OrderEvent> setOpenPrice(final IOrder orderToSetOpenPrice,
                                               final double newOpenPrice) {
        return Observable
            .just(orderToSetOpenPrice)
            .filter(order -> !isOpenPriceSetTo(newOpenPrice).test(order))
            .flatMap(order -> orderTaskExecutor
                .setOpenPrice(order, newOpenPrice)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_PRICE)));
    }

    public Observable<OrderEvent> setStopLossPrice(final IOrder orderToSetSL,
                                                   final double newSL) {
        return Observable
            .just(orderToSetSL)
            .filter(order -> !isSLSetTo(newSL).test(order))
            .flatMap(order -> orderTaskExecutor
                .setStopLossPrice(order, newSL)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_SL)));
    }

    public Observable<OrderEvent> setTakeProfitPrice(final IOrder orderToSetTP,
                                                     final double newTP) {
        return Observable
            .just(orderToSetTP)
            .filter(order -> !isTPSetTo(newTP).test(order))
            .flatMap(order -> orderTaskExecutor
                .setTakeProfitPrice(order, newTP)
                .andThen(orderUtilObservable(order, OrderCallReason.CHANGE_TP)));
    }

    public Observable<OrderEvent> cancelStopLossPrice(final IOrder order) {
        return setStopLossPrice(order, platformSettings.noSLPrice());
    }

    public Observable<OrderEvent> cancelTakeProfitPrice(final IOrder order) {
        return setTakeProfitPrice(order, platformSettings.noTPPrice());
    }

    private Observable<OrderEvent> orderUtilObservable(final IOrder order,
                                                       final OrderCallReason orderCallReason) {
        return Observable.defer(() -> orderUtilHandler.callObservable(order, orderCallReason));
    }
}