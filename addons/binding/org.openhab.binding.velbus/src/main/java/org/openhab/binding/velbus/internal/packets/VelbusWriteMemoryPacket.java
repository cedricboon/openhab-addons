/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

import static org.openhab.binding.velbus.VelbusBindingConstants.COMMAND_WRITE_DATA_TO_MEMORY;

/**
 * The {@link VelbusWriteMemoryPacket} represents a Velbus packet that can be used to
 * request a byte from the memory of the given Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusWriteMemoryPacket extends VelbusPacket {
    private byte highMemoryAddress;
    private byte lowMemoryAddress;
    private byte data;

    public VelbusWriteMemoryPacket(byte address, int memoryAddress, byte data) {
        super(address, PRIO_LOW);

        this.highMemoryAddress = (byte) ((memoryAddress >> 8) & 0xFF);
        this.lowMemoryAddress = (byte) (memoryAddress & 0xFF);
        this.data = data;
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { COMMAND_WRITE_DATA_TO_MEMORY, highMemoryAddress, lowMemoryAddress, data };
    }
}
