/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

import static org.openhab.binding.velbus.VelbusBindingConstants.COMMAND_SET_REALTIME_CLOCK;

import java.util.Calendar;

/**
 * The {@link VelbusSetRealtimeClockPacket} represents a Velbus packet that can be used to
 * set the clock of the given Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusSetRealtimeClockPacket extends VelbusPacket {
    private Calendar calendar;

    public VelbusSetRealtimeClockPacket(byte address, Calendar calendar) {
        super(address, PRIO_LOW);

        this.calendar = calendar;
    }

    public byte getHour() {
        return (byte) calendar.get(Calendar.HOUR_OF_DAY);
    }

    public byte getMinute() {
        return (byte) calendar.get(Calendar.MINUTE);
    }

    public byte getWeekDay() {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return 0x00;
            case Calendar.TUESDAY:
                return 0x01;
            case Calendar.WEDNESDAY:
                return 0x02;
            case Calendar.THURSDAY:
                return 0x03;
            case Calendar.FRIDAY:
                return 0x04;
            case Calendar.SATURDAY:
                return 0x05;
            case Calendar.SUNDAY:
                return 0x06;
            default:
                throw new IllegalArgumentException("Day " + dayOfWeek + " is not a valid weekday.");
        }
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { COMMAND_SET_REALTIME_CLOCK, this.getWeekDay(), this.getHour(), this.getMinute() };
    }
}
