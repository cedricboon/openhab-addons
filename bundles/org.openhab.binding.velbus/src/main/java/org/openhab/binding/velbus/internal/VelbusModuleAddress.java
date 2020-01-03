/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.velbus.internal;

import java.util.ArrayList;

import org.eclipse.smarthome.core.thing.ChannelUID;

/**
 * The {@link VelbusModuleAddress} represents the address and possible subaddresses of a Velbus module.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusModuleAddress {
    private byte address;
    private byte[] subAddresses;

    public VelbusModuleAddress(byte address, int numberOfSubAddresses) {
        this(address, getInitialSubAddresses(numberOfSubAddresses));
    }

    public VelbusModuleAddress(byte address, byte[] subAddresses) {
        this.address = address;
        this.subAddresses = subAddresses;
    }

    public byte getAddress() {
        return address;
    }

    public void setSubAddresses(byte[] subAddresses) {
        this.subAddresses = subAddresses;
    }

    public byte[] getSubAddresses() {
        return subAddresses;
    }

    public byte[] getActiveAddresses() {
        ArrayList<Byte> activeAddresses = new ArrayList<Byte>();
        activeAddresses.add(address);

        for (int i = 0; i < subAddresses.length; i++) {
            if (subAddresses[i] != (byte) 0xFF) {
                activeAddresses.add(subAddresses[i]);
            }
        }

        byte[] result = new byte[activeAddresses.size()];

        for (int i = 0; i < activeAddresses.size(); i++) {
            result[i] = activeAddresses.get(i);
        }

        return result;
    }

    public VelbusChannelIdentifier getChannelIdentifier(ChannelUID channelUID) {
        int channelIndex = getChannelIndex(channelUID);

        return getChannelIdentifier(channelIndex);
    }

    public int getChannelNumber(ChannelUID channelUID) {
        String id = channelUID.getId();
        return Integer.parseInt(id.substring(id.indexOf("#") + 1).substring(2));
    }

    public int getChannelIndex(ChannelUID channelUID) {
        return getChannelNumber(channelUID) - 1;
    }

    public String getChannelId(VelbusChannelIdentifier velbusChannelIdentifier) {
        return "CH" + getChannelNumber(velbusChannelIdentifier);
    }

    public int getChannelIndex(VelbusChannelIdentifier velbusChannelIdentifier) {
        return this.getChannelNumber(velbusChannelIdentifier) - 1;
    }

    public int getChannelNumber(VelbusChannelIdentifier velbusChannelIdentifier) {
        byte[] activeAddresses = getActiveAddresses();

        for (int i = 0; i < activeAddresses.length; i++) {
            if (velbusChannelIdentifier.getAddress() == activeAddresses[i]) {
                return (i * 8) + velbusChannelIdentifier.getChannelNumberFromBitNumber();
            }
        }

        throw new IllegalArgumentException("The byte '" + velbusChannelIdentifier.getChannelByte()
                + "' does not represent a valid channel on the address '" + velbusChannelIdentifier.getAddress()
                + "'.");
    }

    public VelbusChannelIdentifier getChannelIdentifier(int channelIndex) {
        int addressIndex = channelIndex / 8;
        int addressChannelIndex = channelIndex % 8;

        byte address = addressIndex == 0 ? this.address : subAddresses[addressIndex - 1];
        byte channel = (byte) Math.pow(2, addressChannelIndex);

        return new VelbusChannelIdentifier(address, channel);
    }

    private static byte[] getInitialSubAddresses(int numberOfSubAddresses) {
        byte[] subAddresses = new byte[numberOfSubAddresses];

        for (int i = 0; i < numberOfSubAddresses; i++) {
            subAddresses[i] = (byte) 0xFF;
        }

        return subAddresses;
    }
}
