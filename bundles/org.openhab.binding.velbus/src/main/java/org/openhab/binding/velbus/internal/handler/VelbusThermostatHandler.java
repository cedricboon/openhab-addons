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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.CommonTriggerEvents;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.velbus.internal.packets.VelbusPacket;
import org.openhab.binding.velbus.internal.packets.VelbusSensorSettingsRequestPacket;
import org.openhab.binding.velbus.internal.packets.VelbusSetTemperaturePacket;
import org.openhab.binding.velbus.internal.packets.VelbusThermostatModePacket;
import org.openhab.binding.velbus.internal.packets.VelbusThermostatOperatingModePacket;

/**
 * The {@link VelbusThermostatHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Cedric Boon - Initial contribution
 */
@NonNullByDefault
public abstract class VelbusThermostatHandler extends VelbusTemperatureSensorHandler {
    private final double THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION = 0.5;

    private final StringType OPERATING_MODE_HEATING = new StringType("HEATING");
    private final StringType OPERATING_MODE_COOLING = new StringType("COOLING");

    private final byte OPERATING_MODE_MASK = (byte) 0x80;
    private final byte COOLING_MODE_MASK = (byte) 0x80;

    private final StringType MODE_COMFORT = new StringType("COMFORT");
    private final StringType MODE_DAY = new StringType("DAY");
    private final StringType MODE_NIGHT = new StringType("NIGHT");
    private final StringType MODE_SAFE = new StringType("SAFE");

    private final byte MODE_MASK = (byte) 0x70;
    private final byte COMFORT_MODE_MASK = (byte) 0x40;
    private final byte DAY_MODE_MASK = (byte) 0x20;
    private final byte NIGHT_MODE_MASK = (byte) 0x10;

    private final ChannelUID CURRENT_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "thermostat#CURRENTTEMPERATURESETPOINT");
    private final ChannelUID HEATING_MODE_COMFORT_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "thermostat#HEATINGMODECOMFORTTEMPERATURESETPOINT");
    private final ChannelUID HEATING_MODE_DAY_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "thermostat#HEATINGMODEDAYTEMPERATURESETPOINT");
    private final ChannelUID HEATING_MODE_NIGHT_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "thermostat#HEATINGMODENIGHTTEMPERATURESETPOINT");
    private final ChannelUID HEATING_MODE_ANTIFROST_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "thermostat#HEATINGMODEANTIFROSTTEMPERATURESETPOINT");
    private final ChannelUID COOLING_MODE_COMFORT_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "thermostat#COOLINGMODECOMFORTTEMPERATURESETPOINT");
    private final ChannelUID COOLING_MODE_DAY_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "thermostat#COOLINGMODEDAYTEMPERATURESETPOINT");
    private final ChannelUID COOLING_MODE_NIGHT_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "thermostat#COOLINGMODENIGHTTEMPERATURESETPOINT");
    private final ChannelUID COOLING_MODE_SAFE_TEMPERATURE_SETPOINT_CHANNEL = new ChannelUID(thing.getUID(),
            "thermostat#COOLINGMODESAFETEMPERATURESETPOINT");
    private final ChannelUID OPERATING_MODE_CHANNEL = new ChannelUID(thing.getUID(), "thermostat#OPERATINGMODE");
    private final ChannelUID MODE_CHANNEL = new ChannelUID(thing.getUID(), "thermostat#MODE");
    private final ChannelUID HEATER_CHANNEL = new ChannelUID(thing.getUID(), "thermostat#HEATER");
    private final ChannelUID BOOST_CHANNEL = new ChannelUID(thing.getUID(), "thermostat#BOOST");
    private final ChannelUID PUMP_CHANNEL = new ChannelUID(thing.getUID(), "thermostat#PUMP");
    private final ChannelUID COOLER_CHANNEL = new ChannelUID(thing.getUID(), "thermostat#COOLER");
    private final ChannelUID ALARM1_CHANNEL = new ChannelUID(thing.getUID(), "thermostat#ALARM1");
    private final ChannelUID ALARM2_CHANNEL = new ChannelUID(thing.getUID(), "thermostat#ALARM2");
    private final ChannelUID ALARM3_CHANNEL = new ChannelUID(thing.getUID(), "thermostat#ALARM3");
    private final ChannelUID ALARM4_CHANNEL = new ChannelUID(thing.getUID(), "thermostat#ALARM4");

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
        } else if (isThermostatChannel(channelUID)
                && (command instanceof QuantityType<?> || command instanceof DecimalType)) {
            byte temperatureVariable = determineTemperatureVariable(channelUID);
            QuantityType<?> temperatureInDegreesCelcius = (command instanceof QuantityType<?>)
                    ? ((QuantityType<?>) command).toUnit(SIUnits.CELSIUS)
                    : new QuantityType<>(((DecimalType) command), SIUnits.CELSIUS);

            if (temperatureInDegreesCelcius != null) {
                byte temperature = convertToTwoComplementByte(temperatureInDegreesCelcius.doubleValue(),
                        THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);

                VelbusSetTemperaturePacket packet = new VelbusSetTemperaturePacket(getModuleAddress().getAddress(),
                        temperatureVariable, temperature);

                byte[] packetBytes = packet.getBytes();
                velbusBridgeHandler.sendPacket(packetBytes);
            }
        } else if (channelUID.equals(OPERATING_MODE_CHANNEL) && command instanceof StringType) {
            byte commandByte = ((StringType) command).equals(OPERATING_MODE_HEATING) ? COMMAND_SET_HEATING_MODE
                    : COMMAND_SET_COOLING_MODE;

            VelbusThermostatOperatingModePacket packet = new VelbusThermostatOperatingModePacket(
                    getModuleAddress().getAddress(), commandByte);

            byte[] packetBytes = packet.getBytes();
            velbusBridgeHandler.sendPacket(packetBytes);
        } else if (channelUID.equals(MODE_CHANNEL) && command instanceof StringType) {
            byte commandByte = COMMAND_SWITCH_TO_SAFE_MODE;

            StringType stringTypeCommand = (StringType) command;
            if (stringTypeCommand.equals(MODE_COMFORT)) {
                commandByte = COMMAND_SWITCH_TO_COMFORT_MODE;
            } else if (stringTypeCommand.equals(MODE_DAY)) {
                commandByte = COMMAND_SWITCH_TO_DAY_MODE;
            } else if (stringTypeCommand.equals(MODE_NIGHT)) {
                commandByte = COMMAND_SWITCH_TO_NIGHT_MODE;
            }

            VelbusThermostatModePacket packet = new VelbusThermostatModePacket(getModuleAddress().getAddress(),
                    commandByte);

            byte[] packetBytes = packet.getBytes();
            velbusBridgeHandler.sendPacket(packetBytes);
        } else {
            logger.debug("The command '{}' is not supported by this handler.", command.getClass());
        }
    }

    @Override
    public void onPacketReceived(byte[] packet) {
        super.onPacketReceived(packet);

        logger.trace("onPacketReceived() was called");

        if (packet[0] == VelbusPacket.STX && packet.length >= 5) {
            byte address = packet[2];
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
            } else if (command == COMMAND_TEMP_SENSOR_STATUS && packet.length >= 9) {
                byte operatingMode = packet[5];
                byte targetTemperature = packet[9];

                if ((operatingMode & OPERATING_MODE_MASK) == COOLING_MODE_MASK) {
                    updateState(OPERATING_MODE_CHANNEL, OPERATING_MODE_COOLING);
                } else {
                    updateState(OPERATING_MODE_CHANNEL, OPERATING_MODE_HEATING);
                }

                if ((operatingMode & MODE_MASK) == COMFORT_MODE_MASK) {
                    updateState(MODE_CHANNEL, MODE_COMFORT);
                } else if ((operatingMode & MODE_MASK) == DAY_MODE_MASK) {
                    updateState(MODE_CHANNEL, MODE_DAY);
                } else if ((operatingMode & MODE_MASK) == NIGHT_MODE_MASK) {
                    updateState(MODE_CHANNEL, MODE_NIGHT);
                } else {
                    updateState(MODE_CHANNEL, MODE_SAFE);
                }

                double targetTemperatureValue = convertFromTwoComplementByte(targetTemperature,
                        THERMOSTAT_TEMPERATURE_SETPOINT_RESOLUTION);
                updateState(CURRENT_TEMPERATURE_SETPOINT_CHANNEL,
                        new QuantityType<>(targetTemperatureValue, SIUnits.CELSIUS));
            } else if (address != this.getModuleAddress().getAddress() && command == COMMAND_PUSH_BUTTON_STATUS) {
                byte outputChannelsJustActivated = packet[5];
                byte outputChannelsJustDeactivated = packet[6];

                triggerThermostatChannels(outputChannelsJustActivated, CommonTriggerEvents.PRESSED);
                triggerThermostatChannels(outputChannelsJustDeactivated, CommonTriggerEvents.RELEASED);
            }
        }
    }

    private void triggerThermostatChannels(byte outputChannels, String event) {
        if ((outputChannels & 0x01) == 0x01) {
            triggerChannel(HEATER_CHANNEL, event);
        }
        if ((outputChannels & 0x02) == 0x02) {
            triggerChannel(BOOST_CHANNEL, event);
        }
        if ((outputChannels & 0x04) == 0x04) {
            triggerChannel(PUMP_CHANNEL, event);
        }
        if ((outputChannels & 0x08) == 0x08) {
            triggerChannel(COOLER_CHANNEL, event);
        }
        if ((outputChannels & 0x10) == 0x10) {
            triggerChannel(ALARM1_CHANNEL, event);
        }
        if ((outputChannels & 0x20) == 0x20) {
            triggerChannel(ALARM2_CHANNEL, event);
        }
        if ((outputChannels & 0x40) == 0x40) {
            triggerChannel(ALARM3_CHANNEL, event);
        }
        if ((outputChannels & 0x80) == 0x80) {
            triggerChannel(ALARM4_CHANNEL, event);
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
                || channelUID.equals(COOLING_MODE_SAFE_TEMPERATURE_SETPOINT_CHANNEL)
                || channelUID.equals(OPERATING_MODE_CHANNEL) || channelUID.equals(MODE_CHANNEL);
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
