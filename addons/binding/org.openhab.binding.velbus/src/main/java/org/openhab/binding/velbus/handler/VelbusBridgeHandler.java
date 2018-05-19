/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.velbus.internal.VelbusPacketListener;

/**
 * {@link VelbusBridgeHandler} is an abstract handler for a Velbus interface and connects it to
 * the framework.
 *
 * @author Cedric Boon - Initial contribution
 */
public abstract class VelbusBridgeHandler extends BaseBridgeHandler {
    private long lastPacketTimeMillis;

    protected VelbusPacketListener defaultPacketListener;
    protected Map<Byte, VelbusPacketListener> packetListeners = new HashMap<Byte, VelbusPacketListener>();

    public VelbusBridgeHandler(Bridge velbusBridge) {
        super(velbusBridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // There is nothing to handle in the bridge handler
    }

    public synchronized void sendPacket(byte[] packet) {
        long currentTimeMillis = System.currentTimeMillis();
        long timeSinceLastPacket = currentTimeMillis - lastPacketTimeMillis;

        if (timeSinceLastPacket < 60) {
            // When sending you need a delay of 60ms between each packet (to prevent flooding the VMB1USB).
            long timeToDelay = 60 - timeSinceLastPacket;

            scheduler.schedule(() -> {
                sendPacket(packet);
            }, timeToDelay, TimeUnit.MILLISECONDS);

            return;
        }

        writePacket(packet);

        lastPacketTimeMillis = System.currentTimeMillis();
    }

    protected abstract void writePacket(byte[] packet);

    protected void readPacket(byte[] packet) {
        byte address = packet[2];

        VelbusPacketListener packetListener = packetListeners.get(address);
        if (packetListener != null) {
            packetListener.onPacketReceived(packet);
        } else if (defaultPacketListener != null) {
            defaultPacketListener.onPacketReceived(packet);
        }
    }

    public void setDefaultPacketListener(VelbusPacketListener velbusPacketListener) {
        defaultPacketListener = velbusPacketListener;
    }

    public void registerPacketListener(byte address, VelbusPacketListener packetListener) {
        if (packetListener == null) {
            throw new IllegalArgumentException("It's not allowed to pass a null VelbusPacketListener.");
        }

        packetListeners.put(Byte.valueOf(address), packetListener);
    }

    public void unregisterRelayStatusListener(byte address) {
        packetListeners.remove(Byte.valueOf(address));
    }
}
