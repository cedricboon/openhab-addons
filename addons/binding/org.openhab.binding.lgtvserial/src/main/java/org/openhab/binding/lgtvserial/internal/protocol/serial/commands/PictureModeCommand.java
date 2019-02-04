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
package org.openhab.binding.lgtvserial.internal.protocol.serial.commands;

/**
 * This class handles the picture mode D/X command.
 *
 * @author Richard Lavoie - Initial contribution
 *
 */
public class PictureModeCommand extends BaseStringCommand {

    protected PictureModeCommand(int setId) {
        super('d', 'x', setId);
    }

}
