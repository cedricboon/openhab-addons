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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.velbus.internal.VelbusClockAlarm;
import org.openhab.binding.velbus.internal.VelbusClockAlarmConfiguration;
import org.openhab.binding.velbus.internal.VelbusMemoryMap;
import org.openhab.binding.velbus.internal.packets.VelbusPacket;
import org.openhab.binding.velbus.internal.packets.VelbusSetDatePacket;
import org.openhab.binding.velbus.internal.packets.VelbusSetLocalClockAlarmPacket;
import org.openhab.binding.velbus.internal.packets.VelbusSetRealtimeClockPacket;

/**
 * The {@link VelbusSensorWithAlarmClockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusSensorWithAlarmClockHandler extends VelbusSensorHandler {
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = new HashSet<>(Arrays.asList(THING_TYPE_VMB2PBN,
            THING_TYPE_VMB6PBN, THING_TYPE_VMB7IN, THING_TYPE_VMB8PBU, THING_TYPE_VMBPIRC, THING_TYPE_VMBPIRM));

    private final byte ALARM_1_ENABLED_MASK = 0x01;
    private final byte ALARM_2_ENABLED_MASK = 0x04;

    private final ChannelUID CLOCK_ALARM_1_ENABLED = new ChannelUID(thing.getUID(), "clockAlarm#CLOCKALARM1ENABLED");
    private final ChannelUID CLOCK_ALARM_1_WAKEUP_HOUR = new ChannelUID(thing.getUID(),
            "clockAlarm#CLOCKALARM1WAKEUPHOUR");
    private final ChannelUID CLOCK_ALARM_1_WAKEUP_MINUTE = new ChannelUID(thing.getUID(),
            "clockAlarm#CLOCKALARM1WAKEUPMINUTE");
    private final ChannelUID CLOCK_ALARM_1_BEDTIME_HOUR = new ChannelUID(thing.getUID(),
            "clockAlarm#CLOCKALARM1BEDTIMEHOUR");
    private final ChannelUID CLOCK_ALARM_1_BEDTIME_MINUTE = new ChannelUID(thing.getUID(),
            "clockAlarm#CLOCKALARM1BEDTIMEMINUTE");
    private final ChannelUID CLOCK_ALARM_2_ENABLED = new ChannelUID(thing.getUID(), "clockAlarm#CLOCKALARM2ENABLED");
    private final ChannelUID CLOCK_ALARM_2_WAKEUP_HOUR = new ChannelUID(thing.getUID(),
            "clockAlarm#CLOCKALARM2WAKEUPHOUR");
    private final ChannelUID CLOCK_ALARM_2_WAKEUP_MINUTE = new ChannelUID(thing.getUID(),
            "clockAlarm#CLOCKALARM2WAKEUPMINUTE");
    private final ChannelUID CLOCK_ALARM_2_BEDTIME_HOUR = new ChannelUID(thing.getUID(),
            "clockAlarm#CLOCKALARM2BEDTIMEHOUR");
    private final ChannelUID CLOCK_ALARM_2_BEDTIME_MINUTE = new ChannelUID(thing.getUID(),
            "clockAlarm#CLOCKALARM2BEDTIMEMINUTE");

    private VelbusClockAlarmConfiguration alarmClockConfiguration = new VelbusClockAlarmConfiguration();
    private ScheduledFuture<?> timeUpdateJob;

    public VelbusSensorWithAlarmClockHandler(Thing thing) {
        this(thing, 0);
    }

    public VelbusSensorWithAlarmClockHandler(Thing thing, int numberOfSubAddresses) {
        super(thing, numberOfSubAddresses);
    }

    @Override
    public void initialize() {
        super.initialize();

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

    @Override
    public void dispose() {
        timeUpdateJob.cancel(true);
    }

    private void startTimeUpdates(int timeUpdatesInterval) {
        VelbusBridgeHandler velbusBridgeHandler = getVelbusBridgeHandler();
        if (velbusBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        timeUpdateJob = scheduler.scheduleWithFixedDelay(() -> {
            updateDateTime(velbusBridgeHandler);
        }, 0, timeUpdatesInterval, TimeUnit.SECONDS);
    }

    protected void updateDateTime(VelbusBridgeHandler velbusBridgeHandler) {
        Calendar calendar = Calendar.getInstance();
        updateDate(velbusBridgeHandler, calendar);
        updateTime(velbusBridgeHandler, calendar);
    }

    protected void updateTime(VelbusBridgeHandler velbusBridgeHandler, Calendar calendar) {
        VelbusSetRealtimeClockPacket packet = new VelbusSetRealtimeClockPacket(getModuleAddress().getAddress(),
                calendar);

        byte[] packetBytes = packet.getBytes();
        velbusBridgeHandler.sendPacket(packetBytes);
    }

    protected void updateDate(VelbusBridgeHandler velbusBridgeHandler, Calendar calendar) {
        VelbusSetDatePacket packet = new VelbusSetDatePacket(getModuleAddress().getAddress(), calendar);

        byte[] packetBytes = packet.getBytes();
        velbusBridgeHandler.sendPacket(packetBytes);
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
            int alarmClockConfigurationMemoryAddress = VelbusMemoryMap
                    .getAlarmConfigurationMemoryAddress(this.thing.getThingTypeUID());

            sendReadMemoryBlockPacket(velbusBridgeHandler, alarmClockConfigurationMemoryAddress, 0);
            sendReadMemoryBlockPacket(velbusBridgeHandler, alarmClockConfigurationMemoryAddress, 4);
            sendReadMemoryPacket(velbusBridgeHandler, alarmClockConfigurationMemoryAddress, 8);
        } else if (isAlarmClockChannel(channelUID)) {
            byte alarmNumber = determineAlarmNumber(channelUID);
            VelbusClockAlarm alarmClock = alarmClockConfiguration.getAlarmClock(alarmNumber);

            if ((channelUID.equals(CLOCK_ALARM_1_ENABLED) || channelUID.equals(CLOCK_ALARM_2_ENABLED))
                    && command instanceof OnOffType) {
                boolean enabled = (command == OnOffType.ON) ? true : false;
                alarmClock.setEnabled(enabled);
            } else if (channelUID.equals(CLOCK_ALARM_1_WAKEUP_HOUR)
                    || channelUID.equals(CLOCK_ALARM_2_WAKEUP_HOUR) && command instanceof DecimalType) {
                byte wakeupHour = ((DecimalType) command).byteValue();
                alarmClock.setWakeupHour(wakeupHour);
            } else if (channelUID.equals(CLOCK_ALARM_1_WAKEUP_MINUTE)
                    || channelUID.equals(CLOCK_ALARM_2_WAKEUP_MINUTE) && command instanceof DecimalType) {
                byte wakeupMinute = ((DecimalType) command).byteValue();
                alarmClock.setWakeupMinute(wakeupMinute);
            } else if (channelUID.equals(CLOCK_ALARM_1_BEDTIME_HOUR)
                    || channelUID.equals(CLOCK_ALARM_2_BEDTIME_HOUR) && command instanceof DecimalType) {
                byte bedTimeHour = ((DecimalType) command).byteValue();
                alarmClock.setBedtimeHour(bedTimeHour);
            } else if (channelUID.equals(CLOCK_ALARM_1_BEDTIME_MINUTE)
                    || channelUID.equals(CLOCK_ALARM_2_BEDTIME_MINUTE) && command instanceof DecimalType) {
                byte bedTimeMinute = ((DecimalType) command).byteValue();
                alarmClock.setBedtimeMinute(bedTimeMinute);
            }

            VelbusSetLocalClockAlarmPacket packet = new VelbusSetLocalClockAlarmPacket(getModuleAddress().getAddress(),
                    alarmNumber, alarmClock);
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

            if ((command == COMMAND_MEMORY_DATA_BLOCK || command == COMMAND_MEMORY_DATA) && packet.length >= 7) {
                byte highMemoryAddress = packet[5];
                byte lowMemoryAddress = packet[6];
                int memoryAddress = (highMemoryAddress * 0x100) + lowMemoryAddress;

                int alarmClockConfigurationMemoryAddress = VelbusMemoryMap
                        .getAlarmConfigurationMemoryAddress(this.thing.getThingTypeUID());

                VelbusClockAlarm alarmClock1 = this.alarmClockConfiguration.getAlarmClock1();
                VelbusClockAlarm alarmClock2 = this.alarmClockConfiguration.getAlarmClock2();

                if (command == COMMAND_MEMORY_DATA_BLOCK && packet.length >= 11) {
                    byte memoryData1 = packet[7];
                    byte memoryData2 = packet[8];
                    byte memoryData3 = packet[9];
                    byte memoryData4 = packet[10];

                    if (memoryAddress == alarmClockConfigurationMemoryAddress) {
                        alarmClock1.setEnabled((memoryData1 & ALARM_1_ENABLED_MASK) > 0);
                        alarmClock2.setEnabled((memoryData1 & ALARM_2_ENABLED_MASK) > 0);
                        alarmClock1.setWakeupHour(memoryData2);
                        alarmClock1.setWakeupMinute(memoryData3);
                        alarmClock1.setBedtimeHour(memoryData4);

                        updateAlarmClockStateForMemoryBlock1();

                    } else if (memoryAddress == alarmClockConfigurationMemoryAddress + 4) {
                        alarmClock1.setBedtimeMinute(memoryData1);
                        alarmClock2.setWakeupHour(memoryData2);
                        alarmClock2.setWakeupMinute(memoryData3);
                        alarmClock2.setBedtimeHour(memoryData4);

                        updateAlarmClockStateForMemoryBlock2();
                    }

                } else if (command == COMMAND_MEMORY_DATA && packet.length >= 8) {
                    byte memoryData = packet[7];

                    if (memoryAddress == alarmClockConfigurationMemoryAddress + 8) {
                        alarmClock2.setBedtimeMinute(memoryData);

                        updateAlarmClockStateForMemoryBlock3();
                    }
                }
            }
        }
    }

    protected void updateAlarmClockStateForMemoryBlock1() {
        VelbusClockAlarm alarmClock1 = this.alarmClockConfiguration.getAlarmClock1();
        VelbusClockAlarm alarmClock2 = this.alarmClockConfiguration.getAlarmClock2();

        updateState(CLOCK_ALARM_1_ENABLED, alarmClock1.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        updateState(CLOCK_ALARM_2_ENABLED, alarmClock2.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        updateState(CLOCK_ALARM_1_WAKEUP_HOUR, new DecimalType(alarmClock2.getWakeupHour()));
        updateState(CLOCK_ALARM_1_WAKEUP_MINUTE, new DecimalType(alarmClock2.getWakeupMinute()));
        updateState(CLOCK_ALARM_1_BEDTIME_HOUR, new DecimalType(alarmClock2.getBedtimeHour()));
    }

    protected void updateAlarmClockStateForMemoryBlock2() {
        VelbusClockAlarm alarmClock1 = this.alarmClockConfiguration.getAlarmClock1();
        VelbusClockAlarm alarmClock2 = this.alarmClockConfiguration.getAlarmClock2();

        updateState(CLOCK_ALARM_1_BEDTIME_MINUTE, new DecimalType(alarmClock1.getBedtimeMinute()));
        updateState(CLOCK_ALARM_2_WAKEUP_HOUR, new DecimalType(alarmClock2.getWakeupHour()));
        updateState(CLOCK_ALARM_2_WAKEUP_MINUTE, new DecimalType(alarmClock2.getWakeupMinute()));
        updateState(CLOCK_ALARM_2_BEDTIME_HOUR, new DecimalType(alarmClock2.getBedtimeHour()));
    }

    protected void updateAlarmClockStateForMemoryBlock3() {
        VelbusClockAlarm alarmClock2 = this.alarmClockConfiguration.getAlarmClock2();

        updateState(CLOCK_ALARM_2_BEDTIME_MINUTE, new DecimalType(alarmClock2.getBedtimeMinute()));
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
}
