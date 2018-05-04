/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

import static org.openhab.binding.velbus.VelbusBindingConstants.COMMAND_READ_DATA_FROM_MEMORY;

/**
 * The {@link VelbusReadMemoryPacket} represents a Velbus packet that can be used to
 * request a byte from the memory of the given Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusReadMemoryPacket extends VelbusPacket {
    private byte highMemoryAddress;
    private byte lowMemoryAddress;

    public VelbusReadMemoryPacket(byte address, int memoryAddress) {
        super(address, PRIO_LOW);

        highMemoryAddress = (byte) ((memoryAddress >> 8) & 0xFF);
        lowMemoryAddress = (byte) (memoryAddress & 0xFF);
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { COMMAND_READ_DATA_FROM_MEMORY, highMemoryAddress, lowMemoryAddress };
    }
}
