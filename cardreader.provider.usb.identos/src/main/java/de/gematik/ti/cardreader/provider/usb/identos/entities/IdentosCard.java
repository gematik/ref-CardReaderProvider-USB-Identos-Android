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
import java.util.Arrays;

import com.identos.android.id100.library.ccid.UsbReader;

import de.gematik.ti.cardreader.provider.api.card.Atr;
import de.gematik.ti.cardreader.provider.api.card.CardException;
import de.gematik.ti.cardreader.provider.api.card.CardProtocol;
import de.gematik.ti.cardreader.provider.api.card.ICard;
import de.gematik.ti.cardreader.provider.api.card.ICardChannel;
import de.gematik.ti.cardreader.provider.api.command.CommandApdu;
import de.gematik.ti.cardreader.provider.api.command.ICommandApdu;
import de.gematik.ti.cardreader.provider.api.command.IResponseApdu;

/**
 * include::{userguide}/UIDECRP_Structure.adoc[tag=IdentosCard]
 */
public class IdentosCard implements ICard {
    protected static final String PROTO_T_1 = "T=1";
    private static final ICommandApdu MANAGE_CHANNEL_COMMAND_OPEN = new CommandApdu(0x00, 0x70, 0x00, 0x00, 0x01);
    private static final int RESPONSE_SUCCESS = 0x9000;

    private final UsbReader reader;
    private final Atr atr;
    private Thread exclusiveThread;
    private final IdentosCardChannel basicChannel;

    public IdentosCard(final UsbReader reader, final Atr atr) {
        this.reader = reader;
        this.atr = new Atr(atr.getBytes());
        this.exclusiveThread = null;
        basicChannel = new IdentosCardChannel(this);
    }

    /**
     * Returns the ATR of this card
     *
     * @return the ATR of this card.
     */
    @Override
    public Atr getATR() {
        return atr;
    }

    /**
     * Returns the protocol in use for this card, for example "T=0" or "T=1".
     *
     * @return the protocol in use for this card, for example "T=0" or "T=1".
     */
    @Override
    public CardProtocol getProtocol() {
        return CardProtocol.T1;
    }

    @Override
    public ICardChannel openBasicChannel() throws CardException {
        return getBasicChannel();
    }

    /**
     * Returns the CardChannel for the basic logical channel. The basic logical channel has a channel number of 0.
     *
     * @return CardChannel for the basic logical channel. The basic logical channel has a channel number of 0.
     */
    public ICardChannel getBasicChannel() {
        checkCardOpen();
        return basicChannel;
    }

    /**
     * Opens a new logical channel to the card and returns it.
     *
     * @return a new logical channel to the card
     * @throws CardException
     */
    @Override
    public ICardChannel openLogicalChannel() throws CardException {
        checkCardOpen();
        checkExclusive();
        IResponseApdu response = basicChannel.transmit(MANAGE_CHANNEL_COMMAND_OPEN);
        if (response.getSW() != RESPONSE_SUCCESS) {
            throw new CardException("openLogicalChannel failed, response code: " + String.format("0x%04x", response.getSW()));
        }
        System.out.print(Arrays.toString(response.getData()));
        return new IdentosCardChannel(this, response.getData()[0]);
    }

    /**
     * Requests exclusive access to this card.
     * <p/>
     * <p>
     * Once a thread has invoked <code>beginExclusive</code>, only this thread is allowed to communicate with this card until it calls
     * <code>endExclusive</code>. Other threads attempting communication will receive a CardException.
     * <p/>
     * <p>
     * Applications have to ensure that exclusive access is correctly released. This can be achieved by executing the <code>beginExclusive()</code> and
     * <code>endExclusive</code> calls in a <code>try ... finally</code> block.
     * @throws CardException
     */
    public void beginExclusive() throws CardException {
        checkExclusive();
        exclusiveThread = Thread.currentThread();
    }

    /**
     * Releases the exclusive access previously established using <code>beginExclusive</code>.
     * @throws CardException
     */
    public void endExclusive() throws CardException {
        if (exclusiveThread == Thread.currentThread()) {
            exclusiveThread = null;
        } else {
            throw new CardException("This thread " + Thread.currentThread().getName() + " has no exclusive access and thus cannot terminate exclusive access");
        }
    }

    /**
     * Control commands not supported on Identos USB CardReader
     * @param i
     * @param bytes
     * @return
     * @throws CardException
     */
    public byte[] transmitControlCommand(final int i, final byte[] bytes) throws CardException {
        throw new CardException("This card reader " + reader.getDevice().getDeviceName() + " does not support control commands.");
    }

    /**
     * Disconnects the connection with this card.
     *
     * @param reset
     * @throws CardException
     */
    @Override
    public void disconnect(final boolean reset) throws CardException {
        if (reader.isOpen() && reader.isCardConnected()) {
            checkExclusive();
            try {
                reader.disconnectCard();
            } catch (IOException e) {
                throw new CardException(e.toString());
            } finally {
                exclusiveThread = null;
            }
        }
    }

    UsbReader getUsbReader() {
        return reader;
    }

    void checkExclusive() throws CardException {
        if (exclusiveThread == null) {
            return;
        }
        if (exclusiveThread != Thread.currentThread()) {
            throw new CardException("Another thread than this thread " + Thread.currentThread().getName() + " has exclusive access");
        }
    }

    void checkCardOpen() {
        if (!getUsbReader().isOpen() || !reader.isCardConnected()) {
            throw new IllegalStateException("card is not connected");
        }
    }

}
