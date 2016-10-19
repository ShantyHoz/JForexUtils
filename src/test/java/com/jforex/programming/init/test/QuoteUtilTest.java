package com.jforex.programming.init.test;

import org.junit.Before;
import org.junit.Test;

import com.dukascopy.api.OfferSide;
import com.jforex.programming.init.ContextUtil;
import com.jforex.programming.init.QuoteUtil;
import com.jforex.programming.test.common.QuoteProviderForTest;

public class QuoteUtilTest extends QuoteProviderForTest {

    private QuoteUtil quoteUtil;

    @Before
    public void setUp() {
        final ContextUtil contextUtil = new ContextUtil(contextMock);

        quoteUtil = new QuoteUtil(contextUtil, true);
    }

    @Test
    public void initBarsFeedCallsContext() {
        quoteUtil.initBarsFeed(askBarEURUSDCustomPeriodParams);

        verify(contextMock).subscribeToBarsFeed(eq(instrumentEURUSD),
                                                eq(custom3MinutePeriod),
                                                eq(OfferSide.ASK),
                                                any());
    }
}
