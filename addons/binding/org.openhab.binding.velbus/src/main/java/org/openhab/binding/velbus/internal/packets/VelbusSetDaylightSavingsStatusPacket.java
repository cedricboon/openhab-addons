/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

import static org.openhab.binding.velbus.internal.VelbusBindingConstants.COMMAND_DAYLIGHT_SAVING_STATUS;

import java.time.ZonedDateTime;

/**
 * The {@link VelbusSetDaylightSavingsStatusPacket} represents a Velbus packet that can be used to
 * set the daylight saving status of the given Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusSetDaylightSavingsStatusPacket extends VelbusPacket {
    private ZonedDateTime zonedDateTime;

    public VelbusSetDaylightSavingsStatusPacket(byte address, ZonedDateTime zonedDateTime) {
        super(address, PRIO_LOW);

        this.zonedDateTime = zonedDateTime;
    }

    public boolean isDaylightSavings() {
        return zonedDateTime.getZone().getRules().isDaylightSavings(zonedDateTime.toInstant());
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { COMMAND_DAYLIGHT_SAVING_STATUS, (byte) (isDaylightSavings() ? 0x01 : 0x00) };
    }
}
