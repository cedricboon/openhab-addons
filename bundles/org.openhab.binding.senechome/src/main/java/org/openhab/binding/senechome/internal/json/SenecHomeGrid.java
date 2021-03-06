/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.senechome.internal.json;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

/**
 * Json model of senec home devices: This sub model provides the current power limitation by the inverter.
 *
 * @author Steven Schwarznau - Initial Contribution
 */
public class SenecHomeGrid implements Serializable {

    private static final long serialVersionUID = -7479338321370375451L;

    /**
     * grid value indicating the current power draw (for values larger zero) or supply (for negative values)
     */
    public @SerializedName("P_TOTAL") String currentGridValue;

    @Override
    public String toString() {
        return "SenecHomeGrid [currentGridValue=" + currentGridValue + "]";
    }
}
