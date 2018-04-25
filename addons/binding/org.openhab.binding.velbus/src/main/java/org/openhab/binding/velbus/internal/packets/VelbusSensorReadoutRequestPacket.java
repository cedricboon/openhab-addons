/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

import static org.openhab.binding.velbus.VelbusBindingConstants.COMMAND_SENSOR_READOUT_REQUEST;

/**
 * The {@link VelbusSensorReadoutRequestPacket} represents a Velbus packet that can be used to
 * request the value of a sensor channel of the given Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusSensorReadoutRequestPacket extends VelbusPacket {

    private final byte autosendTimeInterval = 0x00;

    private byte channel;

    public VelbusSensorReadoutRequestPacket(byte address, byte channel) {
        super(address, PRIO_LOW);

        this.channel = channel;
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { COMMAND_SENSOR_READOUT_REQUEST, channel, autosendTimeInterval };
    }
}
