/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velbus.internal;

/**
 * The {@link VelbusClockAlarm} represents a class that contains the state representation of a velbus clock alarm.
 *
 * @author Cedric Boon - Initial contribution
 */
public class VelbusClockAlarm {
    private boolean enabled;
    private byte wakeupHour;
    private byte wakeupMinute;
    private byte bedtimeHour;
    private byte bedtimeMinute;

    public VelbusClockAlarm() {
        this.enabled = true;
        this.wakeupHour = 7;
        this.wakeupMinute = 0;
        this.bedtimeHour = 23;
        this.bedtimeMinute = 0;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    public byte getWakeupHour() {
        return this.wakeupHour;
    }

    public void setWakeupHour(byte value) {
        this.wakeupHour = value;
    }

    public byte getWakeupMinute() {
        return this.wakeupMinute;
    }

    public void setWakeupMinute(byte value) {
        this.wakeupMinute = value;
    }

    public byte getBedtimeHour() {
        return this.bedtimeHour;
    }

    public void setBedtimeHour(byte value) {
        this.bedtimeHour = value;
    }

    public byte getBedtimeMinute() {
        return this.bedtimeMinute;
    }

    public void setBedtimeMinute(byte value) {
        this.bedtimeMinute = value;
    }
}
