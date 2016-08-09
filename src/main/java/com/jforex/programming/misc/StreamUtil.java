package com.jforex.programming.misc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jforex.programming.order.call.OrderCallRejectException;
import com.jforex.programming.settings.UserSettings;

import rx.Completable;
import rx.Observable;

public final class StreamUtil {

    private StreamUtil() {
    }

    private static final UserSettings userSettings = ConfigFactory.create(UserSettings.class);
    private static final long delayOnOrderFailRetry = userSettings.delayOnOrderFailRetry();
    private static final int maxRetriesOnOrderFail = userSettings.maxRetriesOnOrderFail();
    private static final long delayOnHistoryFailRetry = userSettings.delayOnHistoryFailRetry();
    private static final int maxRetriesOnHistoryFail = userSettings.maxRetriesOnHistoryFail();
    private static final Logger logger = LogManager.getLogger(StreamUtil.class);

    public static final Observable<Long> retryOnRejectObservable(final Observable<? extends Throwable> errors) {
        return checkNotNull(errors)
            .flatMap(StreamUtil::filterCallErrorType)
            .zipWith(retryCounterObservable(maxRetriesOnOrderFail), Pair::of)
            .flatMap(retryPair -> evaluateRetryPair(retryPair,
                                                    delayOnOrderFailRetry,
                                                    TimeUnit.MILLISECONDS,
                                                    maxRetriesOnOrderFail));
    }

    private static final Observable<? extends Throwable> filterCallErrorType(final Throwable error) {
        if (error instanceof OrderCallRejectException) {
            logPositionTaskRetry((OrderCallRejectException) error);
            return Observable.just(error);
        }
        logger.error("Retry logic received unexpected error " + error.getClass().getName() + "!");
        return Observable.error(error);
    }

    private static final Observable<Long> evaluateRetryPair(final Pair<? extends Throwable, Integer> retryPair,
                                                            final long delay,
                                                            final TimeUnit timeUnit,
                                                            final int maxRetries) {
        return retryPair.getRight() > maxRetries
                ? Observable.error(retryPair.getLeft())
                : waitObservable(delay, timeUnit);
    }

    public static final Observable<Integer> retryCounterObservable(final int maxRetries) {
        return Observable.range(1, maxRetries + 1);
    }

    public static final Observable<Long> waitObservable(final long delay,
                                                        final TimeUnit timeUnit) {
        return Observable
            .interval(delay, timeUnit)
            .take(1);
    }

    private static final void logPositionTaskRetry(final OrderCallRejectException rejectException) {
        logger.warn("Received reject type " + rejectException.orderEvent().type() +
                " for order " + rejectException.orderEvent().order().getLabel() + "!"
                + " Will retry task in " + delayOnOrderFailRetry + " milliseconds...");
    }

    public static final <T> Stream<T> optionalStream(final Optional<T> optional) {
        return checkNotNull(optional).isPresent()
                ? Stream.of(optional.get())
                : Stream.empty();
    }

    public static final Completable completableForJFRunnable(final JFRunnable jfRunnable) {
        checkNotNull(jfRunnable);

        return Completable.fromCallable(() -> {
            jfRunnable.run();
            return null;
        });
    }

    public static final Observable<Long> retryOnHistoryFailObservable(final Observable<? extends Throwable> errors) {
        return checkNotNull(errors)
            .zipWith(retryCounterObservable(5), Pair::of)
            .flatMap(retryPair -> evaluateRetryPair(retryPair,
                                                    delayOnHistoryFailRetry,
                                                    TimeUnit.MILLISECONDS,
                                                    maxRetriesOnHistoryFail));
    }
}
