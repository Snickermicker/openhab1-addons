/**
 * Copyright (c) 2010-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lcn.mappingtarget;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.lcn.common.LcnAddrMod;
import org.openhab.binding.lcn.connection.Connection;
import org.openhab.binding.lcn.connection.ModInfo;
import org.openhab.binding.lcn.input.ModStatusBinSensors;
import org.openhab.binding.lcn.input.ModStatusKeyLocks;
import org.openhab.binding.lcn.input.ModStatusLedsAndLogicOps;
import org.openhab.binding.lcn.input.ModStatusOutput;
import org.openhab.binding.lcn.input.ModStatusRelays;
import org.openhab.binding.lcn.input.ModStatusVar;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.types.Command;

/**
 * Visualizes a relay state.
 *
 * @author Tobias J�ttner
 */
class RelayVisualize extends TargetWithLcnAddr {

    /** Pattern to parse relay visualizations. */
    private static final Pattern PATTERN_RELAY_STATE = Pattern.compile("(?<relayId>[12345678])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Target relay to visualize (0..7). */
    private final int relayId;

    /**
     * Constructor.
     * 
     * @param addr the target LCN module
     * @param relayId 0..7
     */
    RelayVisualize(LcnAddrMod addr, int relayId) {
        super(addr);
        this.relayId = relayId;
    }

    /**
     * Tries to parse the given input text.
     * 
     * @param input the text to parse
     * @return the parsed {@link RelayVisualize} or null
     */
    static Target tryParseTarget(String input) {
        CmdAndAddressRet header = CmdAndAddressRet.parse(input, false);
        if (header != null) {
            Matcher matcher;
            switch (header.getCmd().toUpperCase()) {
                case "RELAY_STATE":
                    if ((matcher = PATTERN_RELAY_STATE.matcher(header.getRestInput())).matches()) {
                        return new RelayVisualize((LcnAddrMod) header.getAddr(),
                                Integer.parseInt(matcher.group("relayId")) - 1);
                    }
                    break;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void send(Connection conn, Item item, Command cmd) {
    }

    /** {@inheritDoc} */
    @Override
    public void register(Connection conn) {
        ModInfo info = conn.updateModuleData((LcnAddrMod) this.addr);
        if (!info.requestStatusRelays.isActive()) {
            info.requestStatusRelays.nextRequestIn(0, System.nanoTime());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean visualizationHandleOutputStatus(ModStatusOutput pchkInput, Command cmd, Item item,
            EventPublisher eventPublisher) {
        return false;
    }

    /**
     * Visualization for {@link OpenClosedType} and {@link OnOffType}.
     * {@inheritDoc}
     */
    @Override
    public boolean visualizationHandleRelaysStatus(ModStatusRelays pchkInput, Command cmd, Item item,
            EventPublisher eventPublisher) {
        if (pchkInput.getLogicalSourceAddr().equals(this.addr)) {
            if (item.getAcceptedDataTypes().contains(OpenClosedType.class)) {
                eventPublisher.postUpdate(item.getName(),
                        pchkInput.getState(this.relayId) ? OpenClosedType.CLOSED : OpenClosedType.OPEN);
                return true;
            } else if (item.getAcceptedDataTypes().contains(OnOffType.class)) {
                eventPublisher.postUpdate(item.getName(),
                        pchkInput.getState(this.relayId) ? OnOffType.ON : OnOffType.OFF);
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean visualizationBinSensorsStatus(ModStatusBinSensors pchkInput, Command cmd, Item item,
            EventPublisher eventPublisher) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean visualizationVarStatus(ModStatusVar pchkInput, Command cmd, Item item,
            EventPublisher eventPublisher) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean visualizationLedsAndLogicOpsStatus(ModStatusLedsAndLogicOps pchkInput, Command cmd, Item item,
            EventPublisher eventPublisher) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean visualizationKeyLocksStatus(ModStatusKeyLocks pchkInput, Command cmd, Item item,
            EventPublisher eventPublisher) {
        return false;
    }

}
