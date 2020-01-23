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

package de.gematik.ti.cardreader.provider.usb.identos.controller;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import com.identos.android.id100.library.ccid.UsbReader;
import de.gematik.ti.cardreader.provider.api.events.CardReaderConnectedEvent;
import de.gematik.ti.cardreader.provider.api.events.CardReaderDisconnectedEvent;
import de.gematik.ti.cardreader.provider.api.events.card.CardPresentEvent;
import de.gematik.ti.cardreader.provider.usb.identos.control.IdentosCardReaderController;
import de.gematik.ti.cardreader.provider.usb.identos.control.IdentosUsbReceiver;
import de.gematik.ti.cardreader.provider.usb.identos.control.MockContext;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

public class IdentosUsbReceiverTest {

    private static IdentosUsbReceiver identosUsbReceiver;
    private static ConnectionListener listener = new ConnectionListener();
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        identosUsbReceiver = new IdentosUsbReceiver();
        EventBus.getDefault().register(listener);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * TODO: PowerMock is required, but it makes prbolem with android project, therefor ignored before a solution being found
     */
    @Ignore
    @Test
    public void testOnReceiveContextIntent() {
        // mock intent
        Intent intent = Mockito.mock(Intent.class);
        UsbDevice device = Mockito.mock(UsbDevice.class);
        UsbInterface usbInterface = Mockito.mock(UsbInterface.class);
        Mockito.when(device.getInterface(0)).thenReturn(usbInterface);

        // bulkInEndpoint
        UsbEndpoint endpoint0 = Mockito.mock(UsbEndpoint.class);
        Mockito.when(endpoint0.getType()).thenReturn(2);
        Mockito.when(usbInterface.getEndpoint(0)).thenReturn(endpoint0);

        // interruptEndpoint
        UsbEndpoint endpoint1 = Mockito.mock(UsbEndpoint.class);
        Mockito.when(endpoint1.getType()).thenReturn(3);
        Mockito.when(usbInterface.getEndpoint(1)).thenReturn(endpoint1);

        //bulkOutEndpoint
        UsbEndpoint endpoint2 = Mockito.mock(UsbEndpoint.class);
        Mockito.when(endpoint2.getDirection()).thenReturn(0);
        Mockito.when(endpoint2.getType()).thenReturn(2);
        Mockito.when(usbInterface.getEndpoint(2)).thenReturn(endpoint2);


        //PowerMockito.mockStatic(UsbReader.class);
        //PowerMockito.when(UsbReader.isSupportedReader(device)).thenReturn(true);
        Mockito.when(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)).thenReturn(device);
        Mockito.when(intent.getAction()).thenReturn(UsbManager.ACTION_USB_DEVICE_ATTACHED);

        identosUsbReceiver.onReceive(new MockContext(), intent);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {

        }
        Assert.assertThat(listener.getConnectedReaders() , Is.is( 1));
    }


    public static class ConnectionListener {

        private int connectedReaders = 0;
        private int disconnectedReaders = 0;
        private int initializedReaders = 0;

        @Subscribe
        public void cardReaderConnected(final CardReaderConnectedEvent connectedEvent) {
            System.out.println(connectedReaders+ "; "+connectedEvent);
            connectedReaders += 1;
        }

        @Subscribe
        public void cardReaderDisconnected(final CardReaderDisconnectedEvent disconnectedEvent) {
            disconnectedReaders += 1;
        }

        public int getConnectedReaders() {
            return connectedReaders;
        }

        public int getDisConnectedReaders() {
            return disconnectedReaders;
        }

    }

}
