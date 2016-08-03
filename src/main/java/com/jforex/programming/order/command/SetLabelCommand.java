package com.jforex.programming.order.command;

import static com.jforex.programming.order.OrderStaticUtil.isLabelSetTo;

import com.dukascopy.api.IOrder;
import com.jforex.programming.order.event.OrderEventTypeData;

public final class SetLabelCommand extends OrderChangeCommand<String> {

    public SetLabelCommand(final IOrder orderToChangeLabel,
                           final String newLabel) {
        super(orderToChangeLabel,
              () -> orderToChangeLabel.setLabel(newLabel),
              OrderEventTypeData.changeLabelData,
              orderToChangeLabel.getLabel(),
              newLabel,
              "label");
    }

    @Override
    public final boolean filter() {
        return !isLabelSetTo(newValue).test(orderToChange);
    }
}
