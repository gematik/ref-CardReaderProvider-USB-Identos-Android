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


import com.identos.android.id100.library.ccid.UsbReader;
import de.gematik.ti.cardreader.provider.api.card.Atr;
import de.gematik.ti.cardreader.provider.api.card.CardException;
import de.gematik.ti.cardreader.provider.api.card.CardProtocol;
import de.gematik.ti.cardreader.provider.api.card.ICardChannel;
import de.gematik.ti.utils.codec.Hex;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.IOException;

public class IdentosCardTest {

    private static UsbReader reader;
    private static IdentosCard identosCard;
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        reader = Mockito.mock(UsbReader.class);
        Atr atr = new Atr(new byte[] { (byte) 0x3B, (byte) 0xDD, (byte) 0x00, (byte) 0xFF, (byte) 0x81, (byte) 0x50, (byte) 0xFE, (byte) 0xF0, (byte) 0x03,
                (byte) 0x01, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00, (byte) 0x00
        });
        identosCard = new IdentosCard(reader, atr);
    }

    @AfterClass
    public static void tearDownAfterClass() {
    }

    @Test
    public void testGetATR() {
        Assert.assertThat(identosCard.getATR().getBytes(), IsEqual.equalTo(new byte[] { (byte) 0x3B, (byte) 0xDD, (byte) 0x00, (byte) 0xFF, (byte) 0x81, (byte) 0x50, (byte) 0xFE, (byte) 0xF0, (byte) 0x03,
                (byte) 0x01, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00, (byte) 0x00
        }));
    }

    @Test
    public void testGetProtocol() {
        Assert.assertThat(identosCard.getProtocol(), Is.is(CardProtocol.T1));
    }

    @Test
    public void testOpenBasicChannel() {
        try {
            Mockito.when(reader.isOpen()).thenReturn(true);
            Mockito.when(reader.isCardConnected()).thenReturn(true);
            ICardChannel channel = identosCard.openBasicChannel();
            Assert.assertThat(channel.getChannelNumber(), Is.is(0));
        } catch (CardException e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testGetBasicChannel() {
            Mockito.when(reader.isOpen()).thenReturn(true);
            Mockito.when(reader.isCardConnected()).thenReturn(true);
            ICardChannel channel = identosCard.getBasicChannel();
            Assert.assertThat(channel.getChannelNumber(), Is.is(0));
    }

    @Test
    public void testOpenLogicalChannel() {

        try {
            Mockito.when(reader.isOpen()).thenReturn(true);
            Mockito.when(reader.isCardConnected()).thenReturn(true);
            Mockito.when(reader.transmit(Hex.decode("0070000001"))).thenReturn(Hex.decode("019000"));
            ICardChannel channel = identosCard.openLogicalChannel();
            Assert.assertThat(channel.getChannelNumber(), Is.is(1));
        } catch (CardException | IOException e) {
            Assert.fail(e.toString());
        }
    }


    @Test
    public void testDisconnect() {
        try {
            Mockito.when(reader.isOpen()).thenReturn(true);
            Mockito.when(reader.isCardConnected()).thenReturn(true);
            identosCard.disconnect(false);
            ExpectedException.none();
        } catch (CardException e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testGetUsbReader() {
        Assert.assertThat(identosCard.getUsbReader(), Is.is(reader));
    }

    @Test
    public void testCheckExclusive() {
        //identosCard.checkExclusive();
        Assert.assertThat("", Is.is(""));
    }

    @Test
    public void testCheckCardOpen() {
        Assert.assertThat("", Is.is(""));
    }

}
