package com.jforex.programming.position.test;

import java.util.Set;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.dukascopy.api.IOrder;
import com.google.common.collect.Sets;
import com.jforex.programming.order.command.CloseCommand;
import com.jforex.programming.order.command.CommandUtil;
import com.jforex.programming.order.command.MergeCommand;
import com.jforex.programming.position.Position;
import com.jforex.programming.position.PositionClose;
import com.jforex.programming.position.PositionFactory;
import com.jforex.programming.position.PositionMerge;
import com.jforex.programming.test.common.InstrumentUtilForTest;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import io.reactivex.Completable;
import io.reactivex.functions.Action;

@RunWith(HierarchicalContextRunner.class)
public class PositionCloseTest extends InstrumentUtilForTest {

    private PositionClose positionClose;

    @Mock
    private PositionMerge positionMergeMock;
    @Mock
    private PositionFactory positionFactoryMock;
    @Mock
    private CommandUtil commandUtilMock;
    @Mock
    private Function<Set<IOrder>, MergeCommand> mergeCommandFactory;
    @Mock
    private Function<IOrder, CloseCommand> closeCommandFactory;
    @Mock
    private MergeCommand mergeCommandMock;
    @Mock
    private CloseCommand closeCommandMock;
    @Mock
    private Action completedActionMock;

    @Before
    public void setUp() {
        setUpMocks();

        positionClose = new PositionClose(positionMergeMock,
                                          positionFactoryMock,
                                          commandUtilMock);
    }

    private void setUpMocks() {
        when(closeCommandFactory.apply(any())).thenReturn(closeCommandMock);
        when(mergeCommandFactory.apply(any())).thenReturn(mergeCommandMock);
    }

    private void setUpMergeCompletables(final Completable firstCompletable,
                                        final Completable... completables) {
        when(positionMergeMock.merge(any(), eq(mergeCommandFactory)))
            .thenReturn(firstCompletable, completables);
    }

    private void setUpCommandUtilCompletables(final Completable firstCompletable,
                                              final Completable... completables) {
        when(commandUtilMock.runCommandsOfFactory(any(), eq(closeCommandFactory)))
            .thenReturn(firstCompletable, completables);
    }

    private void verifyDeferredCompletable() {
        verifyZeroInteractions(positionMergeMock);
        verifyZeroInteractions(positionFactoryMock);
        verifyZeroInteractions(commandUtilMock);
    }

    private void expectPositions(final Set<Position> positions) {
        when(positionFactoryMock.all()).thenReturn(positions);
    }

    public class ClosePositionTests {

        private Completable closePositionCompletable;

        @Before
        public void setUp() {
            closePositionCompletable = positionClose.close(instrumentEURUSD,
                                                           mergeCommandFactory,
                                                           closeCommandFactory);
        }

        private void expectFilledOrOpenedOrders(final Set<IOrder> filledOrders) {
            orderUtilForTest.setUpPositionFactory(positionFactoryMock, instrumentEURUSD, filledOrders);
        }

        @Test
        public void completableIsDeferredWithNoInteractionsToMocks() {
            verifyDeferredCompletable();
        }

        @Test
        public void onSubscribeWithNoOrdersToCloseCompletesImmediately() throws Exception {
            final Set<IOrder> filledOrOpenedOrders = Sets.newHashSet();
            expectFilledOrOpenedOrders(filledOrOpenedOrders);
            setUpMergeCompletables(emptyCompletable());
            setUpCommandUtilCompletables(emptyCompletable());

            closePositionCompletable.subscribe(completedActionMock);

            verify(completedActionMock).run();
        }

        @Test
        public void onSubscribePositionMergeCompletableIsCalled() throws Exception {
            final Set<IOrder> filledOrOpenedOrders = Sets.newHashSet(buyOrderEURUSD, sellOrderEURUSD);
            expectFilledOrOpenedOrders(filledOrOpenedOrders);
            setUpMergeCompletables(emptyCompletable());
            setUpCommandUtilCompletables(emptyCompletable());

            closePositionCompletable.subscribe(completedActionMock);

            verify(positionMergeMock).merge(instrumentEURUSD, mergeCommandFactory);
            verify(commandUtilMock).runCommandsOfFactory(filledOrOpenedOrders, closeCommandFactory);
            verify(completedActionMock).run();
        }
    }

    public class CloseAllPositionsTests {

        private Completable closeAllPositionsCompletable;

        @Before
        public void setUp() {
            closeAllPositionsCompletable = positionClose.closeAll(mergeCommandFactory, closeCommandFactory);
        }

        @Test
        public void completableIsDeferredWithNoInteractionsToMocks() {
            verifyDeferredCompletable();
        }

        @Test
        public void onSubscribeWithNoPositionsCompletesImmediately() throws Exception {
            expectPositions(Sets.newHashSet());

            closeAllPositionsCompletable.subscribe(completedActionMock);

            verifyZeroInteractions(positionMergeMock);
            verifyZeroInteractions(commandUtilMock);
            verify(completedActionMock).run();
        }

        public class TwoPositionsPresent {

            @Before
            public void setUp() {
                final Position positionOneMock = orderUtilForTest
                    .createPositionMock(instrumentEURUSD, Sets.newHashSet(buyOrderEURUSD,
                                                                          sellOrderEURUSD));
                final Position positionTwoMock = orderUtilForTest
                    .createPositionMock(instrumentAUDUSD, Sets.newHashSet(buyOrderAUDUSD,
                                                                          sellOrderAUDUSD));

                expectPositions(Sets.newHashSet(positionOneMock, positionTwoMock));

                orderUtilForTest.setUpPositionFactory(positionFactoryMock,
                                                      instrumentEURUSD,
                                                      Sets.newHashSet(buyOrderEURUSD, sellOrderEURUSD));
                orderUtilForTest.setUpPositionFactory(positionFactoryMock,
                                                      instrumentAUDUSD,
                                                      Sets.newHashSet(buyOrderEURUSD, sellOrderEURUSD));
            }

            @Test
            public void testThatCloseCompletablesAreNotConcatenated() {
                setUpMergeCompletables(neverCompletable(), neverCompletable());
                setUpCommandUtilCompletables(neverCompletable(), neverCompletable());

                closeAllPositionsCompletable.subscribe(completedActionMock);

                verifyZeroInteractions(completedActionMock);
                verify(positionMergeMock, times(2)).merge(any(), eq(mergeCommandFactory));
                verify(commandUtilMock, times(2)).runCommandsOfFactory(any(), eq(closeCommandFactory));
            }

            @Test
            public void whenBothPositionsAreClosedTheCallIsCompleted() throws Exception {
                setUpMergeCompletables(emptyCompletable(), emptyCompletable());
                setUpCommandUtilCompletables(emptyCompletable(), emptyCompletable());

                closeAllPositionsCompletable.subscribe(completedActionMock);

                verify(completedActionMock).run();
            }

            @Test
            public void whenFirstButNotSecondCompletesTheCallIsNotCompleted() {
                setUpMergeCompletables(emptyCompletable(), neverCompletable());

                closeAllPositionsCompletable.subscribe(completedActionMock);

                verifyZeroInteractions(completedActionMock);
            }

            @Test
            public void whenSecondButNotFirstCompletesTheCallIsNotCompleted() {
                setUpMergeCompletables(neverCompletable(), emptyCompletable());

                closeAllPositionsCompletable.subscribe(completedActionMock);

                verifyZeroInteractions(completedActionMock);
            }
        }
    }
}
