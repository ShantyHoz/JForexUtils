package com.jforex.programming.order.command;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.jforex.programming.order.event.OrderEvent;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

public class ClosePositionCommand {

    private final Instrument instrument;
    private final CloseExecutionMode executionMode;
    private final Function<Observable<OrderEvent>, Observable<OrderEvent>> closeFilledComposer;
    private final Function<Observable<OrderEvent>, Observable<OrderEvent>> closeOpenedComposer;
    private final Function<Observable<OrderEvent>, Observable<OrderEvent>> closeAllComposer;
    private final BiFunction<Observable<OrderEvent>, IOrder, Observable<OrderEvent>> singleCloseComposer;
    private final Optional<MergeCommand> maybeMergeCommand;

    public enum CloseExecutionMode {
        CloseFilled,
        CloseOpened,
        CloseAll
    }

    public interface SingleCloseOption {

        CloseOption singleCloseComposer(BiFunction<Observable<OrderEvent>,
                                                   IOrder,
                                                   Observable<OrderEvent>> singleCloseComposer);
    }

    public interface CloseOption {

        public MergeForCloseOption closeFilled(Function<Observable<OrderEvent>,
                                                        Observable<OrderEvent>> closeFilledComposer);

        public BuildOption closeOpened(Function<Observable<OrderEvent>,
                                                Observable<OrderEvent>> closeOpenedComposer);

        public MergeForCloseOption closeAll(Function<Observable<OrderEvent>,
                                                     Observable<OrderEvent>> closeAllComposer);
    }

    public interface MergeForCloseOption {

        BuildOption withMergeCommand(MergeCommand maybeMergeCommand);
    }

    public interface BuildOption {

        public ClosePositionCommand build();
    }

    private ClosePositionCommand(final Builder builder) {
        instrument = builder.instrument;
        executionMode = builder.executionMode;
        closeFilledComposer = builder.closeFilledComposer;
        closeOpenedComposer = builder.closeOpenedComposer;
        closeAllComposer = builder.closeAllComposer;
        singleCloseComposer = builder.singleCloseComposer;
        maybeMergeCommand = builder.maybeMergeCommand;
    }

    public final Instrument instrument() {
        return instrument;
    }

    public final Optional<MergeCommand> maybeMergeCommand() {
        return maybeMergeCommand;
    }

    public final CloseExecutionMode executionMode() {
        return executionMode;
    }

    public final Function<Observable<OrderEvent>, Observable<OrderEvent>> closeFilledComposer() {
        return closeFilledComposer;
    }

    public final Function<Observable<OrderEvent>, Observable<OrderEvent>> closeOpenedComposer() {
        return closeOpenedComposer;
    }

    public final Function<Observable<OrderEvent>, Observable<OrderEvent>> closeAllComposer() {
        return closeAllComposer;
    }

    public final Function<Observable<OrderEvent>,
                          Observable<OrderEvent>>
           singleCloseComposer(final IOrder orderToClose) {
        return obs -> singleCloseComposer.apply(obs, orderToClose);
    }

    public static SingleCloseOption newBuilder(final Instrument instrument) {
        return new Builder(checkNotNull(instrument));
    }

    public static class Builder implements
                                CloseOption,
                                MergeForCloseOption,
                                SingleCloseOption,
                                BuildOption {

        private final Instrument instrument;
        private CloseExecutionMode executionMode;
        private Function<Observable<OrderEvent>, Observable<OrderEvent>> closeFilledComposer =
                observable -> observable;
        private Function<Observable<OrderEvent>, Observable<OrderEvent>> closeOpenedComposer =
                observable -> observable;
        private Function<Observable<OrderEvent>, Observable<OrderEvent>> closeAllComposer =
                observable -> observable;
        private BiFunction<Observable<OrderEvent>, IOrder, Observable<OrderEvent>> singleCloseComposer =
                (observable, order) -> observable;
        private Optional<MergeCommand> maybeMergeCommand = Optional.empty();

        private Builder(final Instrument instrument) {
            this.instrument = instrument;
        }

        @Override
        public CloseOption singleCloseComposer(final BiFunction<Observable<OrderEvent>,
                                                                IOrder,
                                                                Observable<OrderEvent>> singleCloseComposer) {
            this.singleCloseComposer = checkNotNull(singleCloseComposer);
            return this;
        }

        @Override
        public MergeForCloseOption closeFilled(final Function<Observable<OrderEvent>,
                                                              Observable<OrderEvent>> closeFilledComposer) {
            this.executionMode = CloseExecutionMode.CloseFilled;
            this.closeFilledComposer = checkNotNull(closeFilledComposer);
            return this;
        }

        @Override
        public BuildOption closeOpened(final Function<Observable<OrderEvent>,
                                                      Observable<OrderEvent>> closeOpenedComposer) {
            this.executionMode = CloseExecutionMode.CloseOpened;
            this.closeOpenedComposer = checkNotNull(closeOpenedComposer);
            return this;
        }

        @Override
        public MergeForCloseOption closeAll(final Function<Observable<OrderEvent>,
                                                           Observable<OrderEvent>> closeAllComposer) {
            this.executionMode = CloseExecutionMode.CloseAll;
            this.closeAllComposer = checkNotNull(closeAllComposer);
            return this;
        }

        @Override
        public BuildOption withMergeCommand(final MergeCommand maybeMergeCommand) {
            this.maybeMergeCommand = Optional.ofNullable(maybeMergeCommand);
            return this;
        }

        @Override
        public ClosePositionCommand build() {
            return new ClosePositionCommand(this);
        }
    }
}