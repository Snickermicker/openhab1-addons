/**
 * Copyright (c) 2010-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.rfxcom.internal.messages;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.openhab.binding.rfxcom.RFXComValueSelector;
import org.openhab.binding.rfxcom.internal.RFXComException;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;

/**
 * RFXCOM data class for lighting3 message.
 *
 * @author Damien Servant
 * @since 1.9.0
 */
public class RFXComLighting3Message extends RFXComBaseMessage {

    /*
     * Lighting3 packet layout (length 10)
     *
     * packetlength = 0
     * packettype = 1
     * subtype = 2
     * seqnbr = 3
     * system = 4
     * channel8_1 = 5
     * channel10_9 = 6
     * cmnd = 7
     * filler = 8 'bits 3-0
     * rssi = 8 'bits 7-4
     */
    public enum SubType {
        KOPPLA(0),

        UNKNOWN(255);

        private final int subType;

        SubType(int subType) {
            this.subType = subType;
        }

        SubType(byte subType) {
            this.subType = subType;
        }

        public byte toByte() {
            return (byte) subType;
        }

        public static SubType fromByte(int input) {
            for (SubType c : SubType.values()) {
                if (c.subType == input) {
                    return c;
                }
            }

            return SubType.UNKNOWN;
        }
    }

    public enum Commands {
        BRIGHT(0),
        DIM(8),
        ON(16),
        LEVEL1(17),
        LEVEL2(18),
        LEVEL3(19),
        LEVEL4(20),
        LEVEL5(21),
        LEVEL6(22),
        LEVEL7(23),
        LEVEL8(24),
        LEVEL9(25),
        OFF(26),
        PROGRAM(27),

        UNKNOWN(255);

        private final int command;

        Commands(int command) {
            this.command = command;
        }

        Commands(byte command) {
            this.command = command;
        }

        public byte toByte() {
            return (byte) command;
        }

        public static Commands fromByte(int input) {
            for (Commands c : Commands.values()) {
                if (c.command == input) {
                    return c;
                }
            }

            return Commands.UNKNOWN;
        }
    }

    private final static List<RFXComValueSelector> supportedValueSelectors = Arrays.asList(RFXComValueSelector.RAW_DATA,
            RFXComValueSelector.SIGNAL_LEVEL, RFXComValueSelector.COMMAND, RFXComValueSelector.DIMMING_LEVEL);

    public SubType subType = SubType.UNKNOWN;
    public Commands command = Commands.UNKNOWN;
    public byte dimmingLevel = 0;
    public byte signalLevel = 0;

    public RFXComLighting3Message() {
        packetType = PacketType.LIGHTING3;
    }

    public RFXComLighting3Message(byte[] data) {
        encodeMessage(data);
    }

    @Override
    public String toString() {
        String str = "";

        str += super.toString();
        str += "\n - Sub type = " + subType;
        str += "\n - Command = " + command;
        str += "\n - Dim level = " + dimmingLevel;
        str += "\n - Signal level = " + signalLevel;

        return str;
    }

    @Override
    public void encodeMessage(byte[] data) {

        super.encodeMessage(data);

        subType = SubType.fromByte(super.subType);
        dimmingLevel = data[6];
        command = Commands.fromByte(data[7]);
        signalLevel = (byte) ((data[11] & 0xF0) >> 4);
    }

    @Override
    public byte[] decodeMessage() {

        byte[] data = new byte[9];

        data[0] = (byte) (data.length - 1);
        data[1] = RFXComBaseMessage.PacketType.LIGHTING3.toByte();
        data[2] = subType.toByte();
        data[3] = seqNbr;
        data[4] = 0; // system (unused ?)
        data[5] = 0; // channel8_1 (unused ?)
        data[6] = dimmingLevel;
        data[7] = command.toByte();
        data[8] = (byte) ((signalLevel & 0x0F) << 4);

        return data;
    }

    @Override
    public String generateDeviceId() {
        return "";
    }

    /**
     * Convert a 0-15 scale value to a percent type.
     *
     * @param pt
     *            percent type to convert
     * @return converted value 0-15
     */
    public static int getDimLevelFromPercentType(PercentType pt) {
        return pt.toBigDecimal().multiply(BigDecimal.valueOf(15))
                .divide(PercentType.HUNDRED.toBigDecimal(), 0, BigDecimal.ROUND_UP).intValue();
    }

    /**
     * Convert a 0-15 scale value to a percent type.
     *
     * @param pt
     *            percent type to convert
     * @return converted value 0-15
     */
    public static PercentType getPercentTypeFromDimLevel(int value) {
        value = Math.min(value, 15);

        return new PercentType(BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(15), 0, BigDecimal.ROUND_UP).intValue());
    }

    @Override
    public State convertToState(RFXComValueSelector valueSelector) throws RFXComException {

        org.openhab.core.types.State state = UnDefType.UNDEF;

        if (valueSelector.getItemClass() == NumberItem.class) {

            if (valueSelector == RFXComValueSelector.SIGNAL_LEVEL) {

                state = new DecimalType(signalLevel);

            } else if (valueSelector == RFXComValueSelector.DIMMING_LEVEL) {

                state = new DecimalType(dimmingLevel);

            } else {
                throw new RFXComException("Can't convert " + valueSelector + " to NumberItem");
            }

        } else if (valueSelector.getItemClass() == DimmerItem.class
                || valueSelector.getItemClass() == RollershutterItem.class) {

            if (valueSelector == RFXComValueSelector.DIMMING_LEVEL) {
                state = RFXComLighting3Message.getPercentTypeFromDimLevel(dimmingLevel);

            } else {
                throw new RFXComException("Can't convert " + valueSelector + " to DimmerItem/RollershutterItem");
            }

        } else if (valueSelector.getItemClass() == SwitchItem.class) {

            if (valueSelector == RFXComValueSelector.COMMAND) {

                switch (command) {
                    case OFF:
                        state = OnOffType.OFF;
                        break;

                    case ON:
                    case BRIGHT:
                    case DIM:
                        state = OnOffType.ON;
                        break;

                    default:
                        throw new RFXComException("Can't convert " + command + " to SwitchItem");
                }

            } else {
                throw new RFXComException("Can't convert " + valueSelector + " to SwitchItem");
            }

        } else if (valueSelector.getItemClass() == ContactItem.class) {

            if (valueSelector == RFXComValueSelector.CONTACT) {

                switch (command) {
                    case OFF:
                        state = OpenClosedType.CLOSED;
                        break;

                    case ON:
                    case BRIGHT:
                    case DIM:
                        state = OpenClosedType.OPEN;
                        break;

                    default:
                        throw new RFXComException("Can't convert " + command + " to ContactItem");
                }

            } else {
                throw new RFXComException("Can't convert " + valueSelector + " to ContactItem");
            }

        } else if (valueSelector.getItemClass() == StringItem.class) {

            if (valueSelector == RFXComValueSelector.RAW_DATA) {

                state = new StringType(DatatypeConverter.printHexBinary(rawMessage));

            } else {
                throw new RFXComException("Can't convert " + valueSelector + " to StringItem");
            }

        } else {

            throw new RFXComException("Can't convert " + valueSelector + " to " + valueSelector.getItemClass());

        }

        return state;
    }

    @Override
    public void convertFromState(RFXComValueSelector valueSelector, String id, Object subType, Type type,
            byte seqNumber) throws RFXComException {

        this.subType = ((SubType) subType);
        seqNbr = seqNumber;

        switch (valueSelector) {
            case COMMAND:
                if (type instanceof OnOffType) {
                    command = (type == OnOffType.ON ? Commands.ON : Commands.OFF);
                    dimmingLevel = 0;
                } else if (type instanceof DecimalType) {
                    command = Commands.fromByte(((DecimalType) type).intValue());
                    dimmingLevel = 0;
                } else {
                    throw new RFXComException("Can't convert " + type + " to Command");
                }
                break;

            case DIMMING_LEVEL:
                if (type instanceof OnOffType) {
                    command = (type == OnOffType.ON ? Commands.ON : Commands.OFF);
                    dimmingLevel = 0;
                } else if (type instanceof PercentType) {
                    command = Commands.DIM;
                    dimmingLevel = (byte) getDimLevelFromPercentType((PercentType) type);

                    if (dimmingLevel == 0) {
                        command = Commands.OFF;
                    }

                } else if (type instanceof IncreaseDecreaseType) {
                    command = Commands.DIM;
                    // Evert: I do not know how to get previous object state...
                    dimmingLevel = 5;

                } else {
                    throw new RFXComException("Can't convert " + type + " to Command");
                }
                break;

            default:
                throw new RFXComException("Can't convert " + type + " to " + valueSelector);

        }
    }

    @Override
    public Object convertSubType(String subType) throws RFXComException {

        for (SubType s : SubType.values()) {
            if (s.toString().equals(subType)) {
                return s;
            }
        }

        throw new RFXComException("Unknown sub type " + subType);
    }

    @Override
    public List<RFXComValueSelector> getSupportedValueSelectors() throws RFXComException {
        return supportedValueSelectors;
    }

}
