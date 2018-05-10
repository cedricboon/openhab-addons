/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal;

import static org.openhab.binding.velbus.VelbusBindingConstants.*;

import java.util.HashMap;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link VelbusMemoryMap} represents a class that contains the addresses of objects in memory.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusMemoryMap {
    private static final HashMap<ThingTypeUID, Integer> alarmConfigurationMemoryAddresses = new HashMap<ThingTypeUID, Integer>();

    {
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMB2PBN, 0x0093);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMB6PBN, 0x0093);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMB7IN, 0x0093);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMB8PBU, 0x0093);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMBPIRC, 0x0031);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMBPIRM, 0x0031);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMBPIRO, 0x0031);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMBMETEO, 0x0083);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMBGP1, 0x00A4);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMBGP2, 0x00A4);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMBGP4, 0x00A4);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMBGP4PIR, 0x00A4);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMBGPO, 0x0284);
        alarmConfigurationMemoryAddresses.put(THING_TYPE_VMBGPOD, 0x0284);
    }

    public static int getAlarmConfigurationMemoryAddress(ThingTypeUID thingTypeUID) {
        if (alarmConfigurationMemoryAddresses.containsKey(thingTypeUID)) {
            return alarmConfigurationMemoryAddresses.get(thingTypeUID);
        }

        throw new IllegalArgumentException("No alarm configuration memory address could be found for " + thingTypeUID);
    }
}
