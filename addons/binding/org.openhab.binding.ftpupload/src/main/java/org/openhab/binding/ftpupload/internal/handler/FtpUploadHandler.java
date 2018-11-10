/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ftpupload.internal.handler;

import static org.openhab.binding.ftpupload.internal.FtpUploadBindingConstants.*;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.openhab.binding.ftpupload.internal.config.FtpUploadConfig;
import org.openhab.binding.ftpupload.internal.ftp.FtpServer;
import org.openhab.binding.ftpupload.internal.ftp.FtpServerEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link FtpUploadHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Pauli Anttila - Initial contribution
 */
public class FtpUploadHandler extends BaseThingHandler implements FtpServerEventListener {

    private Logger logger = LoggerFactory.getLogger(FtpUploadHandler.class);

    private FtpUploadConfig configuration;
    private FtpServer ftpServer;

    public FtpUploadHandler(Thing thing, FtpServer ftpServer) {
        super(thing);
        this.ftpServer = ftpServer;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand for channel {}: {}", channelUID.getId(), command.toString());
        logger.debug("Command sending not supported by this binding");

        if (command.equals(RefreshType.REFRESH)) {
            ftpServer.printStats();
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing handler for FTP Upload Binding");
        configuration = getConfigAs(FtpUploadConfig.class);
        logger.debug("Using configuration: {}", configuration.toString());

        ftpServer.addEventListener(this);
        try {
            ftpServer.addAuthenticationCredentials(configuration.userName, configuration.password);
        } catch (IllegalArgumentException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        }

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        ftpServer.removeAuthenticationCredentials(configuration.userName);
        ftpServer.removeEventListener(this);
    }

    @Override
    public void fileReceived(String userName, String filename, byte[] data) {
        if (configuration.userName.equals(userName)) {
            updateStatus(ThingStatus.ONLINE);
            updateChannels(filename, data);
            updateTriggers(filename);
        }
    }

    private String guessMimeTypeFromData(byte[] data) {
        String mimeType = HttpUtil.guessContentTypeFromData(data);
        logger.debug("Mime type guess from content: {}", mimeType);
        if (mimeType == null) {
            mimeType = RawType.DEFAULT_MIME_TYPE;
        }
        logger.debug("Mime type: {}", mimeType);
        return mimeType;
    }

    private void updateChannels(String filename, byte[] data) {
        for (Channel channel : thing.getChannels()) {
            String channelConf = (String) channel.getConfiguration().get(PARAM_FILENAME_PATTERN);
            if (channelConf != null) {
                if (filenameMatch(filename, channelConf)) {
                    if ("Image".equals(channel.getAcceptedItemType())) {
                        updateState(channel.getUID().getId(), new RawType(data, guessMimeTypeFromData(data)));
                    }
                }
            }
        }
    }

    private void updateTriggers(String filename) {
        for (Channel channel : thing.getChannels()) {
            String channelConf = (String) channel.getConfiguration().get(PARAM_FILENAME_PATTERN);
            if (channelConf != null) {
                if (filenameMatch(filename, channelConf)) {
                    if ("TRIGGER".equals(channel.getKind().toString())) {
                        triggerChannel(channel.getUID().getId(), EVENT_IMAGE_RECEIVED);
                    }
                }
            }
        }
    }

    private boolean filenameMatch(String filename, String pattern) {
        try {
            return Pattern.compile(pattern).matcher(filename).find();
        } catch (PatternSyntaxException e) {
            logger.warn("Invalid filename pattern '{}', reason: {}", pattern, e.getMessage());
        }
        return false;
    }
}
