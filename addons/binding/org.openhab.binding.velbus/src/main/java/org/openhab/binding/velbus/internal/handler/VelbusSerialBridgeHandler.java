/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal.handler;

import static org.openhab.binding.velbus.internal.VelbusBindingConstants.PORT;

import java.io.IOException;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.velbus.internal.VelbusPacketInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.NRSerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

/**
 * {@link VelbusSerialBridgeHandler} is the handler for a Velbus Serial-interface and connects it to
 * the framework.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusSerialBridgeHandler extends VelbusBridgeHandler implements SerialPortEventListener {
    private Logger logger = LoggerFactory.getLogger(VelbusSerialBridgeHandler.class);

    private static final int BAUD = 9600;
    private NRSerialPort serialPort;
    private OutputStream outputStream;
    private VelbusPacketInputStream inputStream;

    public VelbusSerialBridgeHandler(Bridge velbusBridge) {
        super(velbusBridge);
    }

    @Override
    public void initialize() {
        super.initialize();
        logger.debug("Initializing velbus bridge handler.");

        String port = (String) getConfig().get(PORT);
        if (port != null) {
            serialPort = new NRSerialPort(port, BAUD);
            if (serialPort.connect()) {
                updateStatus(ThingStatus.ONLINE);
                logger.debug("Bridge online on serial port {}", port);

                outputStream = serialPort.getOutputStream();
                inputStream = new VelbusPacketInputStream(serialPort.getInputStream());

                try {
                    serialPort.addEventListener(this);
                    serialPort.notifyOnDataAvailable(true);
                } catch (TooManyListenersException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Failed to register event listener on serial port " + port);
                    logger.debug("Failed to register event listener on serial port {}", port);
                }
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Failed to connect to serial port " + port);
                logger.debug("Failed to connect to serial port {}", port);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Serial port name not configured");
            logger.debug("Serial port name not configured");
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
        if (serialPort != null) {
            serialPort.disconnect();
            serialPort = null;
        }
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        logger.debug("Serial port event triggered");

        if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                byte[] packet;
                while ((packet = inputStream.readPacket()) != null) {
                    readPacket(packet);
                }
            } catch (IOException e) {
                logger.error("Serial port read error", e);
            }
        }
    }
}
