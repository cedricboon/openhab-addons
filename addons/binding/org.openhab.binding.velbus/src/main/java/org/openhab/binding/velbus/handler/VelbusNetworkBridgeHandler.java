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

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.Socket;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.velbus.internal.VelbusPacketInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link VelbusNetworkBridgeHandler} is the handler for a Velbus network interface and connects it to
 * the framework.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusNetworkBridgeHandler extends VelbusBridgeHandler {
    private Logger logger = LoggerFactory.getLogger(VelbusNetworkBridgeHandler.class);

    private Socket socket;
    private OutputStream outputStream;
    private VelbusPacketInputStream inputStream;

    private boolean listenerStopped;

    public VelbusNetworkBridgeHandler(Bridge velbusBridge) {
        super(velbusBridge);
    }

    @Override
    public void initialize() {
        super.initialize();
        logger.debug("Initializing velbus network bridge handler.");

        String address = (String) getConfig().get(ADDRESS);
        BigDecimal port = (BigDecimal) getConfig().get(PORT);

        if (address != null && port != null) {
            int portInt = port.intValue();
            try {
                this.socket = new Socket(address, portInt);

                updateStatus(ThingStatus.ONLINE);
                logger.debug("Bridge online on network address {}:{}", address, portInt);

                this.outputStream = this.socket.getOutputStream();
                inputStream = new VelbusPacketInputStream(this.socket.getInputStream());
            } catch (IOException ex) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Failed to connect to network address " + address + ":" + port);
                logger.debug("Failed to connect to network address {}:{}", address, port);
            }

            // Start Velbus packet listener. This listener will act on all packets coming from
            // IP-interface.
            (new Thread(networkEvents)).start();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Network address or port not configured");
            logger.debug("Network address or port not configured");
        }
    }

    @Override
    protected void writePacket(byte[] packet) {
        try {
            outputStream.write(packet);
            outputStream.flush();
        } catch (IOException e) {
            logger.error("Serial port write error", e);
        }
    }

    @Override
    public void dispose() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error while closing socket", e);
            }
            socket = null;
        }
    }

    /**
     * Runnable that handles inbound communication from Velbus network interface.
     * <p>
     * The thread listens to the TCP socket opened at initialization of the {@link VelbusNetworkBridgeHandler} class
     * and interprets all inbound velbus packets.
     */
    private Runnable networkEvents = () -> {
        byte[] packet;

        listenerStopped = false;

        try {
            while (!listenerStopped & ((packet = inputStream.readPacket()) != null)) {
                readPacket(packet);
            }
        } catch (IOException e) {
            if (!listenerStopped) {
                logger.error("Network read error", e);
            }
        }
    };
}
