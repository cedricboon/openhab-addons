/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.handler;

import static org.openhab.binding.velbus.VelbusBindingConstants.*;

import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.velbus.internal.packets.VelbusPacket;
import org.openhab.binding.velbus.internal.packets.VelbusSensorSettingsRequestPacket;
import org.openhab.binding.velbus.internal.packets.VelbusSetTemperaturePacket;

/**
 * The {@link VelbusThermostatHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Cedric Boon - Initial contribution
 */
public abstract class VelbusThermostatHandler extends VelbusTemperatureSensorHandler {
    private final double THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION = 0.5;

    private final ChannelUID CURRENT_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "CURRENTTEMPERATURESETPOINT");
    private final ChannelUID HEATING_MODE_COMFORT_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "HEATINGMODECOMFORTTEMPERATURESETPOINT");
    private final ChannelUID HEATING_MODE_DAY_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "HEATINGMODEDAYTEMPERATURESETPOINT");
    private final ChannelUID HEATING_MODE_NIGHT_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "HEATINGMODENIGHTTEMPERATURESETPOINT");
    private final ChannelUID HEATING_MODE_ANTIFROST_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "HEATINGMODEANTIFROSTTEMPERATURESETPOINT");
    private final ChannelUID COOLING_MODE_COMFORT_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "COOLINGMODECOMFORTTEMPERATURESETPOINT");
    private final ChannelUID COOLING_MODE_DAY_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "COOLINGMODEDAYTEMPERATURESETPOINT");
    private final ChannelUID COOLING_MODE_NIGHT_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "COOLINGMODENIGHTTEMPERATURESETPOINT");
    private final ChannelUID COOLING_MODE_SAFE_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "COOLINGMODESAFETEMPERATURESETPOINT");

    public VelbusThermostatHandler(Thing thing, int numberOfSubAddresses, ChannelUID temperatureChannel) {
        super(thing, numberOfSubAddresses, temperatureChannel);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);

        VelbusBridgeHandler velbusBridgeHandler = getVelbusBridgeHandler();
        if (velbusBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        if (isThermostatChannel(channelUID) && command instanceof RefreshType) {
            VelbusSensorSettingsRequestPacket packet = new VelbusSensorSettingsRequestPacket(
                    getModuleAddress().getAddress());

            byte[] packetBytes = packet.getBytes();
            velbusBridgeHandler.sendPacket(packetBytes);
        } else if (isThermostatChannel(channelUID) && command instanceof QuantityType<?>) {
            byte temperatureVariable = determineTemperatureVariable(channelUID);
            QuantityType<?> temperatureInDegreesCelcius = ((QuantityType<?>) command).toUnit(SIUnits.CELSIUS);

            if (temperatureInDegreesCelcius != null) {
                byte temperature = convertToTwoComplementByte(temperatureInDegreesCelcius.doubleValue(),
                        THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);

                VelbusSetTemperaturePacket packet = new VelbusSetTemperaturePacket(getModuleAddress().getAddress(),
                        temperatureVariable, temperature);

                byte[] packetBytes = packet.getBytes();
                velbusBridgeHandler.sendPacket(packetBytes);
            }
        } else {
            logger.debug("The command '{}' is not supported by this handler.", command.getClass());
        }
    }

    @Override
    public void onPacketReceived(byte[] packet) {
        super.onPacketReceived(packet);

        logger.trace("onPacketReceived() was called");

        if (packet[0] == VelbusPacket.STX && packet.length >= 5) {
            byte command = packet[4];

            if (command == COMMAND_TEMP_SENSOR_SETTINGS_PART1 && packet.length >= 9) {
                byte currentTemperatureSetByte = packet[5];
                byte heatingModeComfortTemperatureSetByte = packet[6];
                byte heatingModeDayTemperatureSetByte = packet[7];
                byte heatingModeNightTemperatureSetByte = packet[8];
                byte heatingModeAntiFrostTemperatureSetByte = packet[9];

                double currentTemperatureSet = convertFromTwoComplementByte(currentTemperatureSetByte,
                        THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);
                double heatingModeComfortTemperatureSet = convertFromTwoComplementByte(
                        heatingModeComfortTemperatureSetByte, THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);
                double heatingModeDayTemperatureSet = convertFromTwoComplementByte(heatingModeDayTemperatureSetByte,
                        THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);
                double heatingModeNightTemperatureSet = convertFromTwoComplementByte(heatingModeNightTemperatureSetByte,
                        THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);
                double heatingModeAntiFrostTemperatureSet = convertFromTwoComplementByte(
                        heatingModeAntiFrostTemperatureSetByte, THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);

                updateState(CURRENT_TEMPERATURE_SETPOINT_CHANNEL,
                        new QuantityType<>(currentTemperatureSet, SIUnits.CELSIUS));
                updateState(HEATING_MODE_COMFORT_TEMPERATURE_SETPOINT_CHANNEL,
                        new QuantityType<>(heatingModeComfortTemperatureSet, SIUnits.CELSIUS));
                updateState(HEATING_MODE_DAY_TEMPERATURE_SETPOINT_CHANNEL,
                        new QuantityType<>(heatingModeDayTemperatureSet, SIUnits.CELSIUS));
                updateState(HEATING_MODE_NIGHT_TEMPERATURE_SETPOINT_CHANNEL,
                        new QuantityType<>(heatingModeNightTemperatureSet, SIUnits.CELSIUS));
                updateState(HEATING_MODE_ANTIFROST_TEMPERATURE_SETPOINT_CHANNEL,
                        new QuantityType<>(heatingModeAntiFrostTemperatureSet, SIUnits.CELSIUS));
            } else if (command == COMMAND_TEMP_SENSOR_SETTINGS_PART2 && packet.length >= 8) {
                byte coolingModeComfortTemperatureSetByte = packet[5];
                byte coolingModeDayTemperatureSetByte = packet[6];
                byte coolingModeNightTemperatureSetByte = packet[7];
                byte coolingModeSafeTemperatureSetByte = packet[8];

                double coolingModeComfortTemperatureSet = convertFromTwoComplementByte(
                        coolingModeComfortTemperatureSetByte, THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);
                double coolingModeDayTemperatureSet = convertFromTwoComplementByte(coolingModeDayTemperatureSetByte,
                        THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);
                double coolingModeNightTemperatureSet = convertFromTwoComplementByte(coolingModeNightTemperatureSetByte,
                        THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);
                double coolingModeSafeTemperatureSet = convertFromTwoComplementByte(coolingModeSafeTemperatureSetByte,
                        THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);

                updateState(COOLING_MODE_COMFORT_TEMPERATURE_SETPOINT_CHANNEL,
                        new QuantityType<>(coolingModeComfortTemperatureSet, SIUnits.CELSIUS));
                updateState(COOLING_MODE_DAY_TEMPERATURE_SETPOINT_CHANNEL,
                        new QuantityType<>(coolingModeDayTemperatureSet, SIUnits.CELSIUS));
                updateState(COOLING_MODE_NIGHT_TEMPERATURE_SETPOINT_CHANNEL,
                        new QuantityType<>(coolingModeNightTemperatureSet, SIUnits.CELSIUS));
                updateState(COOLING_MODE_SAFE_TEMPERATURE_SETPOINT_CHANNEL,
                        new QuantityType<>(coolingModeSafeTemperatureSet, SIUnits.CELSIUS));
            }
        }
    }

    protected boolean isThermostatChannel(ChannelUID channelUID) {
        return channelUID.equals(CURRENT_TEMPERATURE_SETPOINT_CHANNEL)
                || channelUID.equals(HEATING_MODE_COMFORT_TEMPERATURE_SETPOINT_CHANNEL)
                || channelUID.equals(HEATING_MODE_DAY_TEMPERATURE_SETPOINT_CHANNEL)
                || channelUID.equals(HEATING_MODE_NIGHT_TEMPERATURE_SETPOINT_CHANNEL)
                || channelUID.equals(HEATING_MODE_ANTIFROST_TEMPERATURE_SETPOINT_CHANNEL)
                || channelUID.equals(COOLING_MODE_COMFORT_TEMPERATURE_SETPOINT_CHANNEL)
                || channelUID.equals(COOLING_MODE_DAY_TEMPERATURE_SETPOINT_CHANNEL)
                || channelUID.equals(COOLING_MODE_NIGHT_TEMPERATURE_SETPOINT_CHANNEL)
                || channelUID.equals(COOLING_MODE_SAFE_TEMPERATURE_SETPOINT_CHANNEL);
    }

    protected byte determineTemperatureVariable(ChannelUID channelUID) {
        if (channelUID.equals(CURRENT_TEMPERATURE_SETPOINT_CHANNEL)) {
            return 0x00;
        } else if (channelUID.equals(HEATING_MODE_COMFORT_TEMPERATURE_SETPOINT_CHANNEL)) {
            return 0x01;
        } else if (channelUID.equals(HEATING_MODE_DAY_TEMPERATURE_SETPOINT_CHANNEL)) {
            return 0x02;
        } else if (channelUID.equals(HEATING_MODE_NIGHT_TEMPERATURE_SETPOINT_CHANNEL)) {
            return 0x03;
        } else if (channelUID.equals(HEATING_MODE_ANTIFROST_TEMPERATURE_SETPOINT_CHANNEL)) {
            return 0x04;
        } else if (channelUID.equals(COOLING_MODE_COMFORT_TEMPERATURE_SETPOINT_CHANNEL)) {
            return 0x07;
        } else if (channelUID.equals(COOLING_MODE_DAY_TEMPERATURE_SETPOINT_CHANNEL)) {
            return 0x08;
        } else if (channelUID.equals(COOLING_MODE_NIGHT_TEMPERATURE_SETPOINT_CHANNEL)) {
            return 0x09;
        } else if (channelUID.equals(COOLING_MODE_SAFE_TEMPERATURE_SETPOINT_CHANNEL)) {
            return 0x0A;
        } else {
            throw new IllegalArgumentException("The given channelUID is not a thermostat channel: " + channelUID);
        }
    }

    protected double convertFromTwoComplementByte(byte value, double resolution) {
        return ((value & 0x80) == 0x00) ? value * resolution : ((value & 0x7F) - 0x80) * resolution;
    }

    protected byte convertToTwoComplementByte(double value, double resolution) {
        return (byte) (value / resolution);
    }
}
