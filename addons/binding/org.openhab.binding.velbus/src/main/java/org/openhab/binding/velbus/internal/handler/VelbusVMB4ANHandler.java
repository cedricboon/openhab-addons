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
package org.openhab.binding.velbus.internal.handler;

import static org.openhab.binding.velbus.internal.VelbusBindingConstants.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.MetricPrefix;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.velbus.internal.VelbusChannelIdentifier;
import org.openhab.binding.velbus.internal.packets.VelbusDimmerPacket;
import org.openhab.binding.velbus.internal.packets.VelbusPacket;
import org.openhab.binding.velbus.internal.packets.VelbusSensorReadoutRequestPacket;
import org.openhab.binding.velbus.internal.packets.VelbusStatusRequestPacket;

/**
 * The {@link VelbusVMB4ANHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusVMB4ANHandler extends VelbusSensorWithAlarmClockHandler {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = new HashSet<>(Arrays.asList(THING_TYPE_VMB4AN));

    private static final String alarmChannelPrefix = "alarm#CH";
    private static final String analogInputChannelPrefix = "analogInput#CH";
    private static final String analogOutputChannelPrefix = "analogOutput#CH";

    private static final byte VOLTAGE_SENSOR_TYPE = 0x00;
    private static final byte CURRENT_SENSOR_TYPE = 0x01;
    private static final byte RESISTANCE_SENSOR_TYPE = 0x02;
    private static final byte PERIOD_MEASUREMENT_SENSOR_TYPE = 0x03;

    private ScheduledFuture<?> refreshJob;

    public VelbusVMB4ANHandler(Thing thing) {
        super(thing, 0);
    }

    @Override
    public void initialize() {
        super.initialize();

        initializeAutomaticRefresh();
    }

    private void initializeAutomaticRefresh() {
        Object refreshIntervalObject = getConfig().get(REFRESH_INTERVAL);
        if (refreshIntervalObject != null) {
            int refreshInterval = ((BigDecimal) refreshIntervalObject).intValue();

            if (refreshInterval > 0) {
                startAutomaticRefresh(refreshInterval);
            }
        }
    }

    @Override
    public void dispose() {
        refreshJob.cancel(true);
    }

    private void startAutomaticRefresh(int refreshInterval) {
        VelbusBridgeHandler velbusBridgeHandler = getVelbusBridgeHandler();
        if (velbusBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            sendSensorReadoutRequest(velbusBridgeHandler, ALL_CHANNELS);
        }, 0, refreshInterval, TimeUnit.SECONDS);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);

        VelbusBridgeHandler velbusBridgeHandler = getVelbusBridgeHandler();
        if (velbusBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        byte channelByte = convertChannelUIDToChannelByte(channelUID);

        if (command instanceof RefreshType) {
            VelbusStatusRequestPacket packet = new VelbusStatusRequestPacket(channelByte);

            byte[] packetBytes = packet.getBytes();
            velbusBridgeHandler.sendPacket(packetBytes);
        } else if (command instanceof PercentType && isAnalogOutputChannel(channelUID)) {
            VelbusDimmerPacket packet = new VelbusDimmerPacket(
                    new VelbusChannelIdentifier(this.getModuleAddress().getAddress(), channelByte), COMMAND_SET_VALUE,
                    ((PercentType) command).byteValue(), 0x00, false);

            byte[] packetBytes = packet.getBytes();
            velbusBridgeHandler.sendPacket(packetBytes);
        }
    }

    protected void sendSensorReadoutRequest(VelbusBridgeHandler velbusBridgeHandler, byte channel) {
        VelbusSensorReadoutRequestPacket packet = new VelbusSensorReadoutRequestPacket(getModuleAddress().getAddress(),
                channel);

        byte[] packetBytes = packet.getBytes();
        velbusBridgeHandler.sendPacket(packetBytes);
    }

    @Override
    public void onPacketReceived(byte[] packet) {
        super.onPacketReceived(packet);

        logger.trace("onPacketReceived() was called");

        if (packet[0] == VelbusPacket.STX && packet.length >= 5) {
            byte command = packet[4];

            if (command == COMMAND_SENSOR_RAW_DATA && packet.length >= 10) {
                byte channel = packet[5];
                byte operatingMode = packet[6];
                byte upperByteSensorValue = packet[7];
                byte highByteSensorValue = packet[8];
                byte lowByteCurrentWindValue = packet[9];

                double sensorValue = (upperByteSensorValue * 0x10000 + highByteSensorValue * 0x100
                        + lowByteCurrentWindValue);
                String channelUID = convertAnalogInputChannelByteToChannelUID(channel);

                switch (operatingMode) {
                    case VOLTAGE_SENSOR_TYPE:
                        double voltageResolution = 0.25;
                        double voltageSensorValueState = sensorValue * voltageResolution;
                        updateState(channelUID,
                                new QuantityType<>(voltageSensorValueState, MetricPrefix.MILLI(SmartHomeUnits.VOLT)));
                        break;
                    case CURRENT_SENSOR_TYPE:
                        double currentResolution = 5;
                        double currentSensorValueState = sensorValue * currentResolution;
                        updateState(channelUID,
                                new QuantityType<>(currentSensorValueState, MetricPrefix.MICRO(SmartHomeUnits.AMPERE)));
                        break;
                    case RESISTANCE_SENSOR_TYPE:
                        double resistanceResolution = 0.25;
                        double resistanceSensorValueState = sensorValue * resistanceResolution;
                        updateState(channelUID,
                                new QuantityType<>(resistanceSensorValueState, MetricPrefix.MILLI(SmartHomeUnits.OHM)));
                        break;
                    case PERIOD_MEASUREMENT_SENSOR_TYPE:
                        double periodResolution = 0.5;
                        double periodSensorValueState = sensorValue * periodResolution;
                        updateState(channelUID,
                                new QuantityType<>(periodSensorValueState, MetricPrefix.MICRO(SmartHomeUnits.SECOND)));
                        break;
                }
            }
        }
    }

    protected byte convertChannelUIDToChannelByte(ChannelUID channelUID) {
        if (isAlarmChannel(channelUID)) {
            return convertAlarmChannelUIDToChannelByte(channelUID);
        } else if (isAnalogInputChannel(channelUID)) {
            return convertAnalogInputChannelUIDToChannelByte(channelUID);
        } else if (isAnalogOutputChannel(channelUID)) {
            return convertAnalogOutputChannelUIDToChannelByte(channelUID);
        } else {
            throw new UnsupportedOperationException(
                    "The channel '" + channelUID + "' is not supported on a VMB4AN module.");
        }
    }

    protected boolean isAlarmChannel(ChannelUID channelUID) {
        return channelUID.getId().startsWith(alarmChannelPrefix);
    }

    protected byte convertAlarmChannelUIDToChannelByte(ChannelUID channelUID) {
        return Byte.parseByte(channelUID.getId().replaceAll(alarmChannelPrefix, ""));
    }

    protected String convertAlarmChannelByteToChannelUID(byte channelByte) {
        return alarmChannelPrefix + channelByte;
    }

    protected boolean isAnalogInputChannel(ChannelUID channelUID) {
        return channelUID.getId().startsWith(analogInputChannelPrefix);
    }

    protected byte convertAnalogInputChannelUIDToChannelByte(ChannelUID channelUID) {
        return Byte.parseByte(channelUID.getId().replaceAll(analogInputChannelPrefix, ""));
    }

    protected String convertAnalogInputChannelByteToChannelUID(byte channelByte) {
        return analogInputChannelPrefix + channelByte;
    }

    protected boolean isAnalogOutputChannel(ChannelUID channelUID) {
        return channelUID.getId().startsWith(analogOutputChannelPrefix);
    }

    protected byte convertAnalogOutputChannelUIDToChannelByte(ChannelUID channelUID) {
        return Byte.parseByte(channelUID.getId().replaceAll(analogOutputChannelPrefix, ""));
    }

    protected String convertOutputChannelByteToChannelUID(byte channelByte) {
        return analogOutputChannelPrefix + channelByte;
    }
}
