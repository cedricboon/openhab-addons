/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.packets;

import static org.openhab.binding.velbus.internal.VelbusBindingConstants.COMMAND_SET_TEMP;

/**
 * The {@link VelbusSetTemperaturePacket} represents a Velbus packet that can be used to
 * set the value of a temperature variable of the given Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusSetTemperaturePacket extends VelbusPacket {

    private byte temperatureVariable;
    private byte temperature;

    public VelbusSetTemperaturePacket(byte address, byte temperatureVariable, byte temperature) {
        super(address, PRIO_LOW);

        this.temperatureVariable = temperatureVariable;
        this.temperature = temperature;
    }

    @Override
    protected byte[] getDataBytes() {
        return new byte[] { COMMAND_SET_TEMP, temperatureVariable, temperature };
    }
}
