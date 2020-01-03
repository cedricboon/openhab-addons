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

import static org.openhab.binding.velbus.internal.VelbusBindingConstants.PORT;

import java.util.TooManyListenersException;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
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

    public VelbusSerialBridgeHandler(Bridge velbusBridge) {
        super(velbusBridge);
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        logger.debug("Serial port event triggered");

        if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            readPackets();
        }
    }

    @Override
    protected void connect() {
        String port = (String) getConfig().get(PORT);
        if (port != null) {
            serialPort = new NRSerialPort(port, BAUD);
            if (serialPort.connect()) {
                initializeStreams(serialPort.getOutputStream(), serialPort.getInputStream());

                updateStatus(ThingStatus.ONLINE);
                logger.debug("Bridge online on serial port {}", port);

                try {
                    serialPort.addEventListener(this);
                    serialPort.notifyOnDataAvailable(true);
                } catch (TooManyListenersException e) {
                    onConnectionLost();
                    logger.debug("Failed to register event listener on serial port {}", port);
                }
            } else {
                onConnectionLost();
                logger.debug("Failed to connect to serial port {}", port);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Serial port name not configured");
            logger.debug("Serial port name not configured");
        }
    }

    @Override
    protected void disconnect() {
        if (serialPort != null) {
            serialPort.disconnect();
            serialPort = null;
        }

        super.disconnect();
    }
}
