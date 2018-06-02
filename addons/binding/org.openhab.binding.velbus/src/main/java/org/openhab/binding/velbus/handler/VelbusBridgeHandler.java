/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.handler;

import static org.openhab.binding.velbus.VelbusBindingConstants.TIME_UPDATE_INTERVAL;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.velbus.internal.VelbusPacketListener;
import org.openhab.binding.velbus.internal.packets.VelbusSetDatePacket;
import org.openhab.binding.velbus.internal.packets.VelbusSetDaylightSavingsStatusPacket;
import org.openhab.binding.velbus.internal.packets.VelbusSetRealtimeClockPacket;

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

    private ScheduledFuture<?> timeUpdateJob;

    public VelbusBridgeHandler(Bridge velbusBridge) {
        super(velbusBridge);
    }

    @Override
    public void initialize() {
        initializeTimeUpdate();
    }

    private void initializeTimeUpdate() {
        Object timeUpdateIntervalObject = getConfig().get(TIME_UPDATE_INTERVAL);
        if (timeUpdateIntervalObject != null) {
            int timeUpdateInterval = ((BigDecimal) timeUpdateIntervalObject).intValue();

            if (timeUpdateInterval > 0) {
                startTimeUpdates(timeUpdateInterval);
            }
        }
    }

    private void startTimeUpdates(int timeUpdatesInterval) {
        timeUpdateJob = scheduler.scheduleWithFixedDelay(() -> {
            updateDateTime();
        }, 0, timeUpdatesInterval, TimeUnit.MINUTES);
    }

    protected void updateDateTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.now(), TimeZone.getDefault().toZoneId());

        updateDate(zonedDateTime);
        updateTime(zonedDateTime);
        updateDaylightSavingsStatus(zonedDateTime);
    }

    protected void updateTime(ZonedDateTime zonedDateTime) {
        VelbusSetRealtimeClockPacket packet = new VelbusSetRealtimeClockPacket((byte) 0x00, zonedDateTime);

        byte[] packetBytes = packet.getBytes();
        this.sendPacket(packetBytes);
    }

    protected void updateDate(ZonedDateTime zonedDateTime) {
        VelbusSetDatePacket packet = new VelbusSetDatePacket((byte) 0x00, zonedDateTime);

        byte[] packetBytes = packet.getBytes();
        this.sendPacket(packetBytes);
    }

    protected void updateDaylightSavingsStatus(ZonedDateTime zonedDateTime) {
        VelbusSetDaylightSavingsStatusPacket packet = new VelbusSetDaylightSavingsStatusPacket((byte) 0x00,
                zonedDateTime);

        byte[] packetBytes = packet.getBytes();
        this.sendPacket(packetBytes);
    }

    @Override
    public void dispose() {
        timeUpdateJob.cancel(true);
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
