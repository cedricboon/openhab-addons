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

import java.time.ZonedDateTime;

/**
 * The {@link VelbusSetDatePacket} represents a Velbus packet that can be used to
 * set the date of the given Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusSetDatePacket extends VelbusPacket {
    private ZonedDateTime zonedDateTime;

    public VelbusSetDatePacket(byte address, ZonedDateTime zonedDateTime) {
        super(address, PRIO_LOW);

        this.zonedDateTime = zonedDateTime;
    }

    public byte getDay() {
        return (byte) this.zonedDateTime.getDayOfMonth();
    }

    public byte getMonth() {
        return (byte) this.zonedDateTime.getMonthValue();
    }

    public byte getYearHighByte() {
        return (byte) ((zonedDateTime.getYear() & 0xff00) / 0x100);
    }

    public byte getYearLowByte() {
        return (byte) (zonedDateTime.getYear() & 0xff);
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { COMMAND_SET_REALTIME_DATE, getDay(), getMonth(), getYearHighByte(), getYearLowByte() };
    }
}
