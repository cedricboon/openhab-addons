/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

import static org.openhab.binding.velbus.VelbusBindingConstants.COMMAND_COUNTER_STATUS_REQUEST;

import org.openhab.binding.velbus.internal.VelbusChannelIdentifier;

/**
 * The {@link VelbusCounterStatusRequestPacket} represents a Velbus packet that can be used to
 * request the counter status of the given Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusCounterStatusRequestPacket extends VelbusPacket {
    private byte channel;

    private final byte autosendTimeInterval = 0x00;

    public VelbusCounterStatusRequestPacket(VelbusChannelIdentifier velbusChannelIdentifier) {
        super(velbusChannelIdentifier.getAddress(), PRIO_LOW);

        this.channel = velbusChannelIdentifier.getChannelByte();
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { COMMAND_COUNTER_STATUS_REQUEST, channel, autosendTimeInterval };
    }
}
