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

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.openhab.binding.velbus.internal.VelbusModuleAddress;
import org.openhab.binding.velbus.internal.VelbusPacketListener;
import org.openhab.binding.velbus.internal.packets.VelbusStatusRequestPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base ThingHandler for all Velbus handlers.
 *
 * @author Cedric Boon - Initial contribution
 */
public abstract class VelbusThingHandler extends BaseThingHandler implements VelbusPacketListener {
    /** Logger Instance */
    protected final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private VelbusBridgeHandler velbusBridgeHandler;
    private VelbusModuleAddress velbusModuleAddress;
    private int numberOfChannels;
    private String acceptedItemType;

    public VelbusThingHandler(Thing thing, int numberOfChannels, int numberOfSubAddresses, String acceptedItemType) {
        super(thing);

        this.velbusModuleAddress = createVelbusModuleAddress(thing, numberOfSubAddresses);
        this.numberOfChannels = numberOfChannels;
        this.acceptedItemType = acceptedItemType;
    }

    private VelbusModuleAddress createVelbusModuleAddress(Thing thing, int numberOfSubAddresses) {
        byte address = hexToByte((String) getConfig().get(MODULE_ADDRESS));

        byte[] subAddresses = new byte[numberOfSubAddresses];
        for (int i = 0; i < numberOfSubAddresses; i++) {
            String propertyKey = SUB_ADDRESS + (i + 1);
            if (getThing().getProperties().containsKey(propertyKey)) {
                String subAddress = getThing().getProperties().get(propertyKey);
                subAddresses[i] = hexToByte(subAddress);
            } else {
                subAddresses[i] = (byte) 0xFF;
            }
        }

        return new VelbusModuleAddress(address, numberOfSubAddresses);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing velbus handler.");
        Bridge bridge = getBridge();
        initializeThing(bridge == null ? null : bridge.getStatus());
        initializeChannelNames();
        initializeChannelStates();
    }

    protected VelbusModuleAddress getModuleAddress() {
        return velbusModuleAddress;
    }

    @SuppressWarnings("null")
    protected void updateChannelLabel(ChannelUID channelUID, String channelName, String acceptedItemType) {
        if (channelUID != null && channelName != null) {
            Channel existingChannel = thing.getChannel(channelUID.getId());
            if (existingChannel != null) {
                String acceptedItem = existingChannel.getAcceptedItemType();
                Configuration configuration = existingChannel.getConfiguration();
                Set<String> defaultTags = existingChannel.getDefaultTags();
                String description = existingChannel.getDescription();
                ChannelKind kind = existingChannel.getKind();
                Map<String, String> properties = existingChannel.getProperties();
                ChannelTypeUID type = existingChannel.getChannelTypeUID();

                ThingBuilder thingBuilder = editThing();
                Channel channel = ChannelBuilder.create(channelUID, acceptedItem).withConfiguration(configuration)
                        .withDefaultTags(defaultTags).withDescription(description != null ? description : "")
                        .withKind(kind).withLabel(channelName).withProperties(properties).withType(type).build();
                thingBuilder.withoutChannel(channelUID).withChannel(channel);
                updateThing(thingBuilder.build());
            }
        }
    }

    private void initializeThing(ThingStatus bridgeStatus) {
        logger.debug("initializeThing thing {} with address {} bridge status {}", getThing().getUID(),
                velbusModuleAddress.getAddress(), bridgeStatus);

        // note: this call implicitly registers our handler as a listener on
        // the bridge
        if (getVelbusBridgeHandler() != null) {
            if (bridgeStatus == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    private void initializeChannelNames() {
        for (int i = 1; i <= numberOfChannels; i++) {
            String propertyKey = CHANNEL + i;
            if (getThing().getProperties().containsKey(propertyKey)) {
                String channelName = getThing().getProperties().get(propertyKey);
                Channel channel = thing.getChannel("CH" + i);
                if (channel != null && !channelName.equals(channel.getLabel())) {
                    updateChannelLabel(channel.getUID(), channelName, acceptedItemType);
                }
            }
        }
    }

    private void initializeChannelStates() {
        VelbusStatusRequestPacket packet = new VelbusStatusRequestPacket(getModuleAddress().getAddress());

        byte[] packetBytes = packet.getBytes();
        velbusBridgeHandler.sendPacket(packetBytes);
    }

    private byte hexToByte(String hex) {
        return (byte) ((Character.digit(hex.charAt(hex.length() - 2), 16) << 4)
                + Character.digit(hex.charAt(hex.length() - 1), 16));
    }

    protected synchronized VelbusBridgeHandler getVelbusBridgeHandler() {
        if (this.velbusBridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler bridgeHandler = bridge.getHandler();
            if (bridgeHandler instanceof VelbusBridgeHandler) {
                this.velbusBridgeHandler = (VelbusBridgeHandler) bridgeHandler;

                byte[] activeAddresses = velbusModuleAddress.getActiveAddresses();

                for (int i = 0; i < activeAddresses.length; i++) {
                    this.velbusBridgeHandler.registerPacketListener(activeAddresses[i], this);
                }
            } else {
                return null;
            }
        }
        return this.velbusBridgeHandler;
    }
}
