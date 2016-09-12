package com.jforex.programming.quote;

import java.util.Set;

import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;

import io.reactivex.Observable;

public interface TickQuoteProvider {

    public ITick tick(Instrument instrument);

    public double ask(Instrument instrument);

    public double bid(Instrument instrument);

    public double forOfferSide(Instrument instrument,
                               OfferSide offerSide);

    public Observable<TickQuote> observableForInstruments(Set<Instrument> instruments);

    public Observable<TickQuote> observable();
}
