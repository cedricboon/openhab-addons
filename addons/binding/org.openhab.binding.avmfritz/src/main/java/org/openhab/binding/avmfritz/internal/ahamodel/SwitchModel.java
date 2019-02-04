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
package org.openhab.binding.avmfritz.internal.ahamodel;

import static org.openhab.binding.avmfritz.internal.BindingConstants.*;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * See {@link DeviceListModel}.
 *
 * @author Robert Bausdorf - Initial contribution
 * @author Christoph Weitkamp - Added new channels `locked`, `mode` and `radiator_mode`
 *
 */
@XmlRootElement(name = "switch")
@XmlType(propOrder = { "state", "mode", "lock", "devicelock" })
public class SwitchModel {
    public static final BigDecimal ON = BigDecimal.ONE;
    public static final BigDecimal OFF = BigDecimal.ZERO;
    public static final String MODE_FRITZ_AUTO = "auto";
    public static final String MODE_FRITZ_MANUAL = "manuell";

    private BigDecimal state;
    private String mode;
    private BigDecimal lock;
    private BigDecimal devicelock;

    public BigDecimal getState() {
        return state;
    }

    public void setState(BigDecimal state) {
        this.state = state;
    }

    public String getMode() {
        if (MODE_FRITZ_AUTO.equals(mode)) {
            return MODE_AUTO;
        } else {
            return MODE_MANUAL;
        }
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public BigDecimal getLock() {
        return lock;
    }

    public void setLock(BigDecimal lock) {
        this.lock = lock;
    }

    public BigDecimal getDevicelock() {
        return devicelock;
    }

    public void setDevicelock(BigDecimal devicelock) {
        this.devicelock = devicelock;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("state", getState()).append("mode", getMode()).append("lock", getLock())
                .append("devicelock", getDevicelock()).toString();
    }
}
