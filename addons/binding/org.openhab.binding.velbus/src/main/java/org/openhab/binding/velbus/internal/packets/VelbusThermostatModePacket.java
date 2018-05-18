/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

/**
 * The {@link VelbusThermostatModePacket} represents a Velbus packet that can be used to
 * set the mode (comfort/day/night/safe) of the given Velbus thermostat module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusThermostatModePacket extends VelbusPacket {

    private byte commandByte;

    public VelbusThermostatModePacket(byte address, byte commandByte) {
        super(address, PRIO_LOW);

        this.commandByte = commandByte;
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { commandByte, 0x00, 0x00 };
    }
}
