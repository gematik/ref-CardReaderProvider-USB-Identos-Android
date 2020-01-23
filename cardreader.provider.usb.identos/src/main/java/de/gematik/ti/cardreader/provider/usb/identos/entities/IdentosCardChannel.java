/*
 * Copyright (c) 2020 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.ti.cardreader.provider.usb.identos.entities;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.LinkedHashMap;
import java.util.Map;

import de.gematik.ti.cardreader.provider.api.card.CardException;
import de.gematik.ti.cardreader.provider.api.card.ICard;
import de.gematik.ti.cardreader.provider.api.card.ICardChannel;
import de.gematik.ti.cardreader.provider.api.command.CommandApdu;
import de.gematik.ti.cardreader.provider.api.command.ICommandApdu;
import de.gematik.ti.cardreader.provider.api.command.IResponseApdu;
import de.gematik.ti.cardreader.provider.api.command.ResponseApdu;
import de.gematik.ti.utils.codec.Hex;

/**
 * include::{userguide}/UIDECRP_Structure.adoc[tag=IdentosCardChannel]

 */
public class IdentosCardChannel implements ICardChannel {
    private static final ICommandApdu MANAGE_CHANNEL_COMMAND_CLOSE = new CommandApdu(0x00, 0x70, 0x80, 0x00);
    private static final int RESPONSE_SUCCESS = 0x9000;
    private static final int LOW_CHANNEL_NUMBER_VALUE = 4;
    private static final int MAX_CHANNEL_NO_VALUE = 20;
    private static final String CARDREADER_BUFFER = "cardReader_Buffer";
    private static final String CARD_MAXAPDUBUFFERSIZE = "card_maxApduBufferSize";
    private static final String CARD_MAXRESPAPDUBUFFERSIZE = "card_maxRespApduBufferSize";
    // SecureMessaging-Values not required for the time being, but should stay here for future
    // private static final String CARD_MAXAPDUBUFFERSIZESM = "card_maxApduBufferSizeSM";
    // private static final String CARD_MAXRESPAPDUBUFFERSIZESM = "card_maxRespApduBufferSizeSM"
    private static final Map<String, Integer> bufferSizeConfig = new LinkedHashMap() {
        {
            // If required configure following data in the android.app.config
            put(CARDREADER_BUFFER,256);
            put(CARD_MAXAPDUBUFFERSIZE, 1033);
            put(CARD_MAXRESPAPDUBUFFERSIZE, 65535);
            // SecureMessaging-Values not required for the time being, but should stay here for future
            // put(CARD_MAXAPDUBUFFERSIZESM, 1033);
            // put(CARD_MAXRESPAPDUBUFFERSIZESM, 1033);
        }
    };
    private final IdentosCard card;
    private final int channelNo;
    private boolean channelClosed = false;

    IdentosCardChannel(final IdentosCard card) {
        this(card, 0);
    }

    IdentosCardChannel(final IdentosCard card, final int channelNo) {
        this.card = card;
        this.channelNo = channelNo;
    }

    /**
     * Returns the connected card object
     *
     * @return IdentosCard
     */
    @Override
    public ICard getCard() {
        return card;
    }

    /**
     * Returns the number of channel
     *
     * @return channelNo
     */
    @Override
    public int getChannelNumber() {
        return channelNo;
    }

    /**
     * @return
     */
    @Override
    public boolean isExtendedLengthSupported() {
        return getMaxMessageLength() > 255 && getMaxResponseLength() > 255;
    }
    /**
     * TODO: secureMessaging: Do only if it it required, channel must be know before if secureMessaging used.
     *
     * @return
     */
    @Override
    public int getMaxMessageLength() {
        int maxMessageLengthCardReader = bufferSizeConfig.get(CARDREADER_BUFFER);
        int maxMessageLengthCard = bufferSizeConfig.get(CARD_MAXAPDUBUFFERSIZE);
        return Math.min(maxMessageLengthCard, maxMessageLengthCardReader);
    }

    /**
     * TODO: secureMessaging: Do only if it it required, channel must be know before if secureMessaging used.
     *
     * @return
     */
    @Override
    public int getMaxResponseLength() {
        int maxResponseLengthCardReader = bufferSizeConfig.get(CARDREADER_BUFFER);
        int maxResponseLengthCard = bufferSizeConfig.get(CARD_MAXRESPAPDUBUFFERSIZE);
        return Math.min(maxResponseLengthCard, maxResponseLengthCardReader);
    }    /**
     * Returns the responseAPDU after transmitting a commandAPDU
     *
     * @param commandAPDU
     * @return responseAPDU
     * @throws CardException
     */
    @Override
    public IResponseApdu transmit(final ICommandApdu commandAPDU) throws CardException {
        IResponseApdu responseApdu;
        ICommandApdu command = commandAPDU;

        checkChannelClosed();
        card.checkCardOpen();
        card.checkExclusive();

        if (channelNo > 0) {
            command = modifyCommandForLogicalChannel(command);
        } else {
            command = commandAPDU;
        }
        try {

            byte[] response = card.getUsbReader().transmit(command.getBytes());
            responseApdu = new ResponseApdu(response);
        } catch (IOException e) {
            throw new CardException(e.toString());
        }

        return responseApdu;
    }

    /**
     * Returns the length of the response byte array after sending an command APDU
     *
     * @param command
     * @param response
     * @return length of the response byte array
     * @throws CardException
     */
    public int transmit(final ByteBuffer command, final ByteBuffer response) throws CardException {
        checkChannelClosed();
        card.checkCardOpen();
        card.checkExclusive();

        if ((command == null) || (response == null)) {
            throw new NullPointerException();
        }
        if (response.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (command == response) {
            throw new IllegalArgumentException("command and response must not be the same object");
        }

        byte[] commandBytes = new byte[command.remaining()];
        command.get(commandBytes);

        int length;

        try {
            byte[] responseBytes = card.getUsbReader().transmit(commandBytes);
            response.put(responseBytes);
            length = responseBytes.length;
        } catch (IOException e) {
            throw new CardException(e.toString());
        }

        return length;
    }

    /**
     * Closes the logical channel
     *
     * @throws CardException
     */
    @Override
    public void close() throws CardException {
        if (channelNo != 0) {
            checkChannelClosed();
            card.checkCardOpen();
            card.checkExclusive();
            try {
                ICommandApdu command = modifyCommandForLogicalChannel(MANAGE_CHANNEL_COMMAND_CLOSE);
                IResponseApdu res = transmit(command);
                if (res.getSW() != RESPONSE_SUCCESS) {
                    throw new CardException("closing logical channel " + channelNo + " failed.");
                }
            } finally {
                channelClosed = true;
            }
        } else {
            throw new CardException("Basic channel cannot be closed.");
        }

    }

    ICommandApdu modifyCommandForLogicalChannel(final ICommandApdu command) throws CardException {
        byte cla = (byte) command.getCla();
        if (channelNo < LOW_CHANNEL_NUMBER_VALUE) {
            cla &= 0xfc; // NOCS(hoa): set 1st and 2nd bit to 0
            cla |= channelNo; // set 1st and 2nd bit to channelNo
        } else if (channelNo < MAX_CHANNEL_NO_VALUE) {
            cla |= 0x40; // NOCS(hoa): set bit 7 to indicate channelNo > 3, see [ISO 7816-4#5.1.1]
            cla |= channelNo - LOW_CHANNEL_NUMBER_VALUE;
        } else {
            throw new CardException("Channel number: " + channelNo + " not allowed");
        }

        return new CommandApdu(cla, command.getIns(), command.getP1(), command.getP2(), command.getData());
    }

    private void checkChannelClosed() throws CardException {
        if (channelClosed) {
            throw new CardException("Logical Channel " + channelNo + " is already closed");
        }
    }
}
