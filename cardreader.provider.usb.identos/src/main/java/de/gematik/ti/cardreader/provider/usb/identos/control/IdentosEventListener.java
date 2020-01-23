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

package de.gematik.ti.cardreader.provider.usb.identos.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.identos.android.id100.library.ccid.CcidReader;
import com.identos.android.id100.library.ccid.OnCardEventListener;
import com.identos.android.id100.library.ccid.UsbReader;

import de.gematik.ti.cardreader.provider.api.CardEventTransmitter;
import de.gematik.ti.cardreader.provider.api.ICardReader;

/**
 * include::{userguide}/UIDECRP_Structure.adoc[tag=IdentosEventListener]
 */
public class IdentosEventListener implements OnCardEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(IdentosEventListener.class);
    private final CardEventTransmitter cardEventTransmitter;

    public IdentosEventListener(ICardReader cardReader) {
        cardEventTransmitter = IdentosCardReaderController.getInstance().createCardEventTransmitter(cardReader);
    }

    /**
     * Method to handle received card events
     * @param ccidReader
     * @param i
     */
    @Override
    public void onCardEvent(final CcidReader ccidReader, int i) {
        if (ccidReader instanceof UsbReader) {
            UsbReader reader = (UsbReader) ccidReader;
            LOG.debug("card event received: {}, from reader: {}", i, reader.getDevice().getSerialNumber());
            switch (i) {
                case 0:
                    cardEventTransmitter.informAboutCardAbsent();
                    break;
                case 1:
                    cardEventTransmitter.informAboutCardPresent();
                    break;
                default:
                    cardEventTransmitter.informAboutCardUnknown();
            }
        }
    }
}
