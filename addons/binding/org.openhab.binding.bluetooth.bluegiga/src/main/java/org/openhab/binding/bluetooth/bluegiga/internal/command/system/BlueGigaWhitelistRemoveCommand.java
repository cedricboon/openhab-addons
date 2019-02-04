/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.bluetooth.bluegiga.internal.command.system;

import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaCommand;

/**
 * Class to implement the BlueGiga command <b>whitelistRemove</b>.
 * <p>
 * Remove an entry from the running white list. Do not use this command while advertising or
 * while being connected.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
public class BlueGigaWhitelistRemoveCommand extends BlueGigaCommand {
    public static int COMMAND_CLASS = 0x00;
    public static int COMMAND_METHOD = 0x0B;

    /**
     * Bluetooth device address to remove from the running white list.
     * <p>
     * BlueGiga API type is <i>bd_addr</i> - Java type is {@link String}
     */
    private String address;

    /**
     * Bluetooth device address to remove from the running white list.
     *
     * @param address the address to set as {@link String}
     */
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeAddress(address);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaWhitelistRemoveCommand [address=");
        builder.append(address);
        builder.append(']');
        return builder.toString();
    }
}
