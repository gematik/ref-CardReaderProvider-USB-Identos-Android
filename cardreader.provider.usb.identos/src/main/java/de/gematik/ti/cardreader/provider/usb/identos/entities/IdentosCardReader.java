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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.identos.android.id100.library.ccid.UsbReader;

import android.content.Context;
import android.hardware.usb.UsbManager;
import de.gematik.ti.cardreader.provider.api.ICardReader;
import de.gematik.ti.cardreader.provider.api.card.Atr;
import de.gematik.ti.cardreader.provider.api.card.CardException;
import de.gematik.ti.cardreader.provider.api.card.ICard;
import de.gematik.ti.cardreader.provider.usb.identos.control.IdentosCardReaderController;
import de.gematik.ti.cardreader.provider.usb.identos.control.IdentosEventListener;

/**
 * include::{userguide}/UIDECRP_Structure.adoc[tag=IdentosCardReader]
 */
public class IdentosCardReader implements ICardReader {
    private static final Logger LOG = LoggerFactory.getLogger(IdentosCardReader.class);

    private static final byte PROTO_ID_T_1 = 1;
    private final UsbReader cardReader;
    private final Context context;
    private final IdentosEventListener eventListener;
    private boolean initialized;

    /**
     * Constructor
     * @param context
     * @param identosCardReader
     */
    public IdentosCardReader(final Context context, final UsbReader identosCardReader) {
        this.context = context;
        this.cardReader = identosCardReader;
        eventListener = new IdentosEventListener(this);
        initialized = initialize(false);
    }

    /**
     * initializes, claim the necessary permissions and (if permissions granted) registers the cardReader for card events
     */
    @Override
    public void initialize() {
        new Thread(() -> {
            initialized = initialize(true);

            if (initialized) {
                LOG.debug("initialize() successful for device {}", IdentosCardReader.this.cardReader.getDevice().getSerialNumber());
            } else {
                LOG.error("initialize() not successful for device {}", IdentosCardReader.this.cardReader.getDevice().getSerialNumber());
            }
            IdentosCardReaderController.getInstance().ctInitCompleted(IdentosCardReader.this, initialized);
            LOG.debug("initialize(): sent information about init completed");
        }).start();
    }

    /**
     * Returns the current initialisation status
     *
     * @return
     *          true: if cardReader is initialized
     *          false: cardReader not operational
     */
    @Override
    public boolean isInitialized() {
        if (!initialized) {
            initialized = cardReader.isOpen() && cardReader.getOnCardEventListeners().contains(eventListener);
        }
        return initialized;
    }

    /**
     * Establishes a connection to the card. If a connection has previously established using the specified protocol, this method returns a card object.
     *
     * @param protocol
     * @return card object
     * @throws CardException
     */
    public ICard connect(String protocol) throws CardException {
        checkInitialized();
        try {
            if (protocol.equals("*") || protocol.equals(IdentosCard.PROTO_T_1)) {
                com.identos.android.id100.library.ccid.Atr atrIdentos = cardReader.connectCard(((byte) PROTO_ID_T_1));
                Atr atr = new Atr(atrIdentos.getBytes());
                return new IdentosCard(cardReader, atr);
            } else {
                throw new CardException("Unsupported protocol " + protocol + ", supported protocols: " + IdentosCard.PROTO_T_1);
            }
        } catch (IOException e) {
            throw new CardException(e.toString());
        }
    }

    /**
     * Establishes a connection to the card. If a connection has previously established this method returns a card object with protocol "T=1".
     *
     * @return card object
     * @throws CardException
     */
    @Override
    public ICard connect() throws CardException {
        return connect(IdentosCard.PROTO_T_1);
    }

    /**
     * Returns the unique name of this cardReader.
     *
     * @return this.name
     */
    @Override
    public String getName() {
        return cardReader.getDevice().getProductName() + "-" + cardReader.getDevice().getSerialNumber();
    }

    /**
     * Returns whether a card is present in this cardReader.
     *
     * @return
     *          true if card is present
     *          false if card is not present
     */
    @Override
    public boolean isCardPresent() throws CardException {
        checkInitialized();
        try {
            return cardReader.isCardPresent();
        } catch (IOException e) {
            throw new CardException(e.toString());
        }
    }

    /**
     * Waits until a card is absent in this reader or the timeout expires. If the method returns due to an expired timeout, it returns false. Otherwise it
     * return true.
     *
     * @param timeout
     * @return
     */
    public boolean waitForCardAbsent(long timeout) throws CardException {
        checkInitialized();
        try {
            cardReader.waitForCardEvent(UsbReader.CARD_EVENT_CARD_INSERTED, timeout);
        } catch (IOException e) {
            throw new CardException(e.toString());
        }
        return true;
    }

    /**
     * Waits until a card is present in this reader or the timeout expires. If the method returns due to an expired timeout, it returns false. Otherwise it
     * return true.
     *
     * @param timeout
     * @return
     */
    public boolean waitForCardPresent(long timeout) throws CardException {
        checkInitialized();
        try {
            cardReader.waitForCardEvent(UsbReader.CARD_EVENT_CARD_REMOVED, timeout);
        } catch (IOException e) {
            throw new CardException(e.toString());
        }
        return true;
    }

    /**
     * Overrides the toSting() method
     * @return
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns the Identos UsbReader
     * @return UsbReader
     */
    public UsbReader getReader() {
        return cardReader;
    }

    private synchronized boolean initialize(final boolean requestPermission) {
        boolean initComplete = false;

        boolean permissionGranted = hasPermission();

        if (!permissionGranted && requestPermission) {
            permissionGranted = IdentosCardReaderController.getInstance().waitForPermission(cardReader.getDevice());
            LOG.debug("initialize(): granted: " + permissionGranted);
        }

        if (permissionGranted) {
            try {
                if (!cardReader.isOpen()) {
                    cardReader.open(context);
                    LOG.debug("initialize(): card reader opened");

                }
                registerForEvents();
                initComplete = true;
                LOG.debug("initialize(): init completed");
            } catch (IOException e) {
                LOG.error("Initialization of device {}, serialNo: {} failed: ", getName(), cardReader.getDevice().getSerialNumber(), e);
            }
        }

        return initComplete;
    }

    private boolean hasPermission() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        return usbManager != null && usbManager.hasPermission(cardReader.getDevice());
    }

    private void registerForEvents() {
        if (!cardReader.getOnCardEventListeners().contains(eventListener)) {
            cardReader.addOnCardEventListener(eventListener);
        }
    }

    private void checkInitialized() throws CardException {
        if (!isInitialized()) {
            throw new CardException("Identos device " + getName() + " was not initialized.");
        }
    }
}
