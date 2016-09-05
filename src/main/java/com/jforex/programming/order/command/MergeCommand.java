package com.jforex.programming.order.command;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.dukascopy.api.IOrder;
import com.jforex.programming.misc.IEngineUtil;
import com.jforex.programming.order.call.OrderCallReason;
import com.jforex.programming.order.event.OrderEventType;
import com.jforex.programming.order.process.option.MergeOption;

import rx.Completable;

public class MergeCommand extends CommonCommand {

    private final String mergeOrderLabel;
    private final Set<IOrder> toMergeOrders;

    private MergeCommand(final Builder builder) {
        super(builder);
        mergeOrderLabel = builder.mergeOrderLabel;
        toMergeOrders = builder.toMergeOrders;
    }

    public String mergeOrderLabel() {
        return mergeOrderLabel;
    }

    public Set<IOrder> toMergeOrders() {
        return toMergeOrders;
    }

    public static final MergeOption create(final String mergeOrderLabel,
                                           final Set<IOrder> toMergeOrders,
                                           final IEngineUtil engineUtil,
                                           final Function<MergeCommand, Completable> startFunction) {
        return new Builder(checkNotNull(mergeOrderLabel),
                           checkNotNull(toMergeOrders),
                           engineUtil,
                           startFunction);
    }

    private static class Builder extends CommonBuilder<MergeOption>
                                 implements MergeOption {

        private final String mergeOrderLabel;
        private final Set<IOrder> toMergeOrders;

        private Builder(final String mergeOrderLabel,
                        final Set<IOrder> toMergeOrders,
                        final IEngineUtil engineUtil,
                        final Function<MergeCommand, Completable> startFunction) {
            this.mergeOrderLabel = mergeOrderLabel;
            this.toMergeOrders = toMergeOrders;
            this.callable = engineUtil.mergeCallable(mergeOrderLabel, toMergeOrders);
            this.callReason = OrderCallReason.MERGE;
            this.startFunction = startFunction;
        }

        @Override
        public MergeOption doOnMergeReject(final Consumer<IOrder> rejectAction) {
            eventHandlerForType.put(OrderEventType.MERGE_REJECTED, checkNotNull(rejectAction));
            return this;
        }

        @Override
        public MergeOption doOnMergeClose(final Consumer<IOrder> mergeCloseAction) {
            eventHandlerForType.put(OrderEventType.MERGE_CLOSE_OK, checkNotNull(mergeCloseAction));
            return this;
        }

        @Override
        public MergeOption doOnMerge(final Consumer<IOrder> doneAction) {
            eventHandlerForType.put(OrderEventType.MERGE_OK, checkNotNull(doneAction));
            return this;
        }

        @Override
        public MergeCommand build() {
            return new MergeCommand(this);
        }
    }
}