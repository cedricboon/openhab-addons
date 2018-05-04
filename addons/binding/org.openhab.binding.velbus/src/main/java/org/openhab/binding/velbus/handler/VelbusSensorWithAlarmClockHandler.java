/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.handler;

import static org.openhab.binding.velbus.VelbusBindingConstants.COMMAND_TEMP_SENSOR_SETTINGS_PART1;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.velbus.internal.VelbusAlarmClockConfiguration;
import org.openhab.binding.velbus.internal.packets.VelbusPacket;
import org.openhab.binding.velbus.internal.packets.VelbusReadMemoryBlockPacket;
import org.openhab.binding.velbus.internal.packets.VelbusReadMemoryPacket;
import org.openhab.binding.velbus.internal.packets.VelbusSetLocalClockAlarmPacket;

/**
 * The {@link VelbusSensorWithAlarmClockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Cedric Boon - Initial contribution
 */
public abstract class VelbusSensorWithAlarmClockHandler extends VelbusSensorHandler {
    private final ChannelUID CLOCK_ALARM_1_ENABLED = new ChannelUID(thing.getUID(), "CLOCKALARM1ENABLED");
    private final ChannelUID CLOCK_ALARM_1_WAKEUP_HOUR = new ChannelUID(thing.getUID(), "CLOCKALARM1WAKEUPHOUR");
    private final ChannelUID CLOCK_ALARM_1_WAKEUP_MINUTE = new ChannelUID(thing.getUID(), "CLOCKALARM1WAKEUPMINUTE");
    private final ChannelUID CLOCK_ALARM_1_BEDTIME_HOUR = new ChannelUID(thing.getUID(), "CLOCKALARM1BEDTIMEHOUR");
    private final ChannelUID CLOCK_ALARM_1_BEDTIME_MINUTE = new ChannelUID(thing.getUID(), "CLOCKALARM1BEDTIMEMINUTE");
    private final ChannelUID CLOCK_ALARM_2_ENABLED = new ChannelUID(thing.getUID(), "CLOCKALARM2ENABLED");
    private final ChannelUID CLOCK_ALARM_2_WAKEUP_HOUR = new ChannelUID(thing.getUID(), "CLOCKALARM2WAKEUPHOUR");
    private final ChannelUID CLOCK_ALARM_2_WAKEUP_MINUTE = new ChannelUID(thing.getUID(), "CLOCKALARM2WAKEUPMINUTE");
    private final ChannelUID CLOCK_ALARM_2_BEDTIME_HOUR = new ChannelUID(thing.getUID(), "CLOCKALARM2BEDTIMEHOUR");
    private final ChannelUID CLOCK_ALARM_2_BEDTIME_MINUTE = new ChannelUID(thing.getUID(), "CLOCKALARM2BEDTIMEMINUTE");

    private VelbusAlarmClockConfiguration[] alarmClockConfigurations = new VelbusAlarmClockConfiguration[] {
            new VelbusAlarmClockConfiguration(), new VelbusAlarmClockConfiguration() };

    public VelbusSensorWithAlarmClockHandler(Thing thing, int numberOfSubAddresses) {
        super(thing, numberOfSubAddresses);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);

        VelbusBridgeHandler velbusBridgeHandler = getVelbusBridgeHandler();
        if (velbusBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        if (isAlarmClockChannel(channelUID) && command instanceof RefreshType) {
            int alarmClockConfigurationMemoryAddress = getAlarmClockConfigurationMemoryAddress();

            VelbusReadMemoryBlockPacket packet1 = new VelbusReadMemoryBlockPacket(getModuleAddress().getAddress(),
                    alarmClockConfigurationMemoryAddress);
            byte[] packetBytes = packet1.getBytes();
            velbusBridgeHandler.sendPacket(packetBytes);

            alarmClockConfigurationMemoryAddress = alarmClockConfigurationMemoryAddress + 4;

            VelbusReadMemoryBlockPacket packet2 = new VelbusReadMemoryBlockPacket(getModuleAddress().getAddress(),
                    alarmClockConfigurationMemoryAddress);
            packetBytes = packet2.getBytes();
            velbusBridgeHandler.sendPacket(packetBytes);

            alarmClockConfigurationMemoryAddress = alarmClockConfigurationMemoryAddress + 4;

            VelbusReadMemoryPacket packet3 = new VelbusReadMemoryPacket(getModuleAddress().getAddress(),
                    alarmClockConfigurationMemoryAddress);
            packetBytes = packet3.getBytes();
            velbusBridgeHandler.sendPacket(packetBytes);
        } else if (isAlarmClockChannel(channelUID)) {
            byte alarmNumber = determineAlarmNumber(channelUID);
            VelbusAlarmClockConfiguration alarmClockConfiguration = alarmClockConfigurations[alarmNumber];

            if ((channelUID.equals(CLOCK_ALARM_1_ENABLED) || channelUID.equals(CLOCK_ALARM_2_ENABLED))
                    && command instanceof OnOffType) {
                boolean enabled = (command == OnOffType.ON) ? true : false;
                alarmClockConfiguration.setEnabled(enabled);
            } else if (channelUID.equals(CLOCK_ALARM_1_WAKEUP_HOUR)
                    || channelUID.equals(CLOCK_ALARM_2_WAKEUP_HOUR) && command instanceof DecimalType) {
                byte wakeupHour = ((DecimalType) command).byteValue();
                alarmClockConfiguration.setWakeupHour(wakeupHour);
            } else if (channelUID.equals(CLOCK_ALARM_1_WAKEUP_MINUTE)
                    || channelUID.equals(CLOCK_ALARM_2_WAKEUP_MINUTE) && command instanceof DecimalType) {
                byte wakeupMinute = ((DecimalType) command).byteValue();
                alarmClockConfiguration.setWakeupMinute(wakeupMinute);
            } else if (channelUID.equals(CLOCK_ALARM_1_BEDTIME_HOUR)
                    || channelUID.equals(CLOCK_ALARM_2_BEDTIME_HOUR) && command instanceof DecimalType) {
                byte bedTimeHour = ((DecimalType) command).byteValue();
                alarmClockConfiguration.setBedtimeHour(bedTimeHour);
            } else if (channelUID.equals(CLOCK_ALARM_1_BEDTIME_MINUTE)
                    || channelUID.equals(CLOCK_ALARM_2_BEDTIME_MINUTE) && command instanceof DecimalType) {
                byte bedTimeMinute = ((DecimalType) command).byteValue();
                alarmClockConfiguration.setBedtimeMinute(bedTimeMinute);
            }

            VelbusSetLocalClockAlarmPacket packet = new VelbusSetLocalClockAlarmPacket(getModuleAddress().getAddress(),
                    alarmNumber, alarmClockConfiguration);
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
            byte command = packet[4];

            if (command == COMMAND_TEMP_SENSOR_SETTINGS_PART1 && packet.length >= 9) {
            }
        }
    }

    protected boolean isAlarmClockChannel(ChannelUID channelUID) {
        return channelUID.equals(CLOCK_ALARM_1_ENABLED) || channelUID.equals(CLOCK_ALARM_1_WAKEUP_HOUR)
                || channelUID.equals(CLOCK_ALARM_1_WAKEUP_MINUTE) || channelUID.equals(CLOCK_ALARM_1_BEDTIME_HOUR)
                || channelUID.equals(CLOCK_ALARM_1_BEDTIME_MINUTE) || channelUID.equals(CLOCK_ALARM_2_ENABLED)
                || channelUID.equals(CLOCK_ALARM_2_WAKEUP_HOUR) || channelUID.equals(CLOCK_ALARM_2_WAKEUP_MINUTE)
                || channelUID.equals(CLOCK_ALARM_2_BEDTIME_HOUR) || channelUID.equals(CLOCK_ALARM_2_BEDTIME_MINUTE);
    }

    protected byte determineAlarmNumber(ChannelUID channelUID) {
        if (channelUID.equals(CLOCK_ALARM_1_ENABLED) || channelUID.equals(CLOCK_ALARM_1_WAKEUP_HOUR)
                || channelUID.equals(CLOCK_ALARM_1_WAKEUP_MINUTE) || channelUID.equals(CLOCK_ALARM_1_BEDTIME_HOUR)
                || channelUID.equals(CLOCK_ALARM_1_BEDTIME_MINUTE)) {
            return 1;
        } else if (channelUID.equals(CLOCK_ALARM_2_ENABLED) || channelUID.equals(CLOCK_ALARM_2_WAKEUP_HOUR)
                || channelUID.equals(CLOCK_ALARM_2_WAKEUP_MINUTE) || channelUID.equals(CLOCK_ALARM_2_BEDTIME_HOUR)
                || channelUID.equals(CLOCK_ALARM_2_BEDTIME_MINUTE)) {
            return 2;
        } else {
            throw new IllegalArgumentException("The given channelUID is not an alarm clock channel: " + channelUID);
        }
    }

    protected abstract int getAlarmClockConfigurationMemoryAddress();
}
