/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

import static org.openhab.binding.velbus.VelbusBindingConstants.COMMAND_SET_REALTIME_DATE;

import java.util.Calendar;

/**
 * The {@link VelbusSetDatePacket} represents a Velbus packet that can be used to
 * set the date of the given Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusSetDatePacket extends VelbusPacket {
    private Calendar calendar;

    public VelbusSetDatePacket(byte address, Calendar calendar) {
        super(address, PRIO_LOW);

        this.calendar = calendar;
    }

    public byte getDay() {
        return (byte) calendar.get(Calendar.DAY_OF_MONTH);
    }

    public byte getMonth() {
        return (byte) calendar.get(Calendar.MONTH);
    }

    public byte getYearHighByte() {
        return (byte) ((calendar.get(Calendar.YEAR) & 0xff00) / 0x100);
    }

    public byte getYearLowByte() {
        return (byte) (calendar.get(Calendar.YEAR) & 0xff);
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { COMMAND_SET_REALTIME_DATE, this.getDay(), this.getMonth(), getYearHighByte(),
                getYearLowByte() };
    }
}
