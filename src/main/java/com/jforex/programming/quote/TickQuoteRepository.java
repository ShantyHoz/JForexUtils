package com.jforex.programming.quote;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.dukascopy.api.Instrument;
import com.jforex.programming.misc.HistoryUtil;

import io.reactivex.Flowable;

public class TickQuoteRepository {

    private final Map<Instrument, TickQuote> quotesByInstrument = new ConcurrentHashMap<>();

    public TickQuoteRepository(final Flowable<TickQuote> tickQuoteObservable,
                               final HistoryUtil historyUtil,
                               final Set<Instrument> subscribedInstruments) {
        historyUtil
            .tickQuotesObservable(subscribedInstruments)
            .subscribe(quote -> quotesByInstrument.put(quote.instrument(), quote));

        tickQuoteObservable.subscribe(this::onTickQuote);
    }

    private final void onTickQuote(final TickQuote tickQuote) {
        quotesByInstrument.put(tickQuote.instrument(), tickQuote);
    }

    public TickQuote get(final Instrument instrument) {
        return quotesByInstrument.get(instrument);
    }
}
