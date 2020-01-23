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

import static de.gematik.ti.utils.codec.Hex.decode;

import com.identos.android.id100.library.ccid.UsbReader;
import de.gematik.ti.cardreader.provider.api.card.Atr;
import de.gematik.ti.cardreader.provider.api.card.CardException;
import de.gematik.ti.cardreader.provider.api.command.CommandApdu;
import de.gematik.ti.cardreader.provider.api.command.ICommandApdu;
import de.gematik.ti.cardreader.provider.api.command.IResponseApdu;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

public class IdentosCardChannelTest {
    private static UsbReader reader;
    private static IdentosCard identosCard;
    private static IdentosCardChannel cardChannel;
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        reader = Mockito.mock(UsbReader.class);
        Atr atr = new Atr(new byte[] { (byte) 0x3B, (byte) 0xDD, (byte) 0x00, (byte) 0xFF, (byte) 0x81, (byte) 0x50, (byte) 0xFE, (byte) 0xF0, (byte) 0x03,
                (byte) 0x01, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00, (byte) 0x00
        });
        identosCard = new IdentosCard(reader, atr);
        cardChannel = new IdentosCardChannel(identosCard);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testGetCard() {
        Assert.assertThat(cardChannel.getCard(), Is.is(identosCard));
    }

    @Test
    public void testGetChannelNumber() {
        Assert.assertThat(cardChannel.getChannelNumber(), Is.is(0));
    }

    @Test
    public void testTransmitICommandApdu() {
        ICommandApdu command = new CommandApdu(0x00, 0x70, 0x00, 0x00, 0x01);
        try {
            Mockito.when(reader.isOpen()).thenReturn(true);
            Mockito.when(reader.isCardConnected()).thenReturn(true);
            Mockito.when(reader.transmit(decode("0070000001"))).thenReturn(decode("8003019000"));

            IResponseApdu responseApdu = cardChannel.transmit(command);
            Assert.assertThat(responseApdu.getSW(), IsEqual.equalTo(0x9000));
        } catch (CardException e) {
            Assert.fail(e.toString());
        } catch (IOException e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void shouldSucceedExtendedLengthSupported() {
        IdentosCard cardMock = Mockito.mock(IdentosCard.class);
        IdentosCardChannel pcscCardChannelLocal = new IdentosCardChannel(cardMock, 0);
        boolean extendedLengthSupported = pcscCardChannelLocal.isExtendedLengthSupported();
        Assert.assertThat(extendedLengthSupported, Is.is(true));
    }

    @Test
    public void testGetMaxMessageLength() {
        IdentosCard cardMock = Mockito.mock(IdentosCard.class);
        IdentosCardChannel pcscCardChannelLocal = new IdentosCardChannel(cardMock, 0);
        int maxMessageLength = pcscCardChannelLocal.getMaxMessageLength();
        Assert.assertThat(maxMessageLength, Is.is(256));
    }

    @Test
    public void testGetMaxResponseLength() {
        IdentosCard cardMock = Mockito.mock(IdentosCard.class);
        IdentosCardChannel pcscCardChannelLocal = new IdentosCardChannel(cardMock, 0);
        int maxResponseLength = pcscCardChannelLocal.getMaxResponseLength();
        Assert.assertThat(maxResponseLength, Is.is(256));
    }

}
