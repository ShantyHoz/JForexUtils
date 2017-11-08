package com.jforex.programming.instrument;

import static java.util.stream.Collectors.toSet;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.dukascopy.api.ICurrency;
import com.dukascopy.api.Instrument;
import com.google.common.collect.Sets;
import com.jforex.programming.currency.CurrencyFactory;
import com.jforex.programming.currency.CurrencyUtil;

import io.reactivex.Maybe;

public class CrossInstrument {

    private final Instrument firstInstrument;
    private final Instrument secondInstrument;
    private final Instrument instrument;
    private final ICurrency crossCurrency;
    private final boolean isEmpty;
    private final boolean shouldDivide;
    private final int pipScale;

    public CrossInstrument(final Instrument firstInstrument,
                           final Instrument secondInstrument) {
        this.firstInstrument = firstInstrument;
        this.secondInstrument = secondInstrument;

        final Maybe<Instrument> maybeCross = InstrumentFactory.maybeCross(firstInstrument, secondInstrument);

        instrument = maybeCross.blockingGet();
        isEmpty = maybeCross
            .isEmpty()
            .blockingGet();
        crossCurrency = calcCrossCurrency();
        shouldDivide = shouldDivide();
        pipScale = instrument.getPipScale() + 1;
    }

    public boolean isValid() {
        return !isEmpty;
    }

    public Instrument get() {
        return instrument;
    }

    public ICurrency crossCurrency() {
        return crossCurrency;
    }

    public FxRate rate(final FxRate rateA,
                       final FxRate rateB) {
        final BigDecimal bdcFirst = rateA.instrument().equals(firstInstrument)
                ? BigDecimal.valueOf(rateA.value())
                : BigDecimal.valueOf(rateB.value());
        final BigDecimal bdcSecond = rateB.instrument().equals(secondInstrument)
                ? BigDecimal.valueOf(rateB.value())
                : BigDecimal.valueOf(rateA.value());

        final double crossValue = shouldDivide
                ? bdcFirst.divide(bdcSecond,
                                  pipScale,
                                  RoundingMode.HALF_UP)
                    .doubleValue()
                : bdcFirst
                    .multiply(bdcSecond)
                    .setScale(pipScale, RoundingMode.HALF_UP)
                    .doubleValue();

        return new FxRate(crossValue, instrument);
    }

    private ICurrency calcCrossCurrency() {
        return CurrencyFactory
            .fromInstruments(firstInstrument, secondInstrument)
            .stream()
            .filter(currency -> CurrencyUtil.isInAllInstruments(currency,
                                                                Sets.newHashSet(firstInstrument, secondInstrument)))
            .collect(toSet())
            .iterator()
            .next();
    }

    private boolean shouldDivide() {
        final ICurrency baseFirstCurrency = firstInstrument.getPrimaryJFCurrency();
        final ICurrency baseSecondCurrency = secondInstrument.getPrimaryJFCurrency();
        final ICurrency quoteFirstCurrency = firstInstrument.getSecondaryJFCurrency();
        final ICurrency quoteSecondCurrency = secondInstrument.getSecondaryJFCurrency();

        return baseFirstCurrency.equals(crossCurrency) && baseSecondCurrency.equals(crossCurrency)
                || quoteFirstCurrency.equals(crossCurrency) && quoteSecondCurrency.equals(crossCurrency);
    }
}