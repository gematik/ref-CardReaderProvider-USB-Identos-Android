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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.identos.android.id100.library.ccid.UsbReader;

import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.ArrayMap;

import de.gematik.ti.cardreader.provider.api.ICardReader;
import de.gematik.ti.cardreader.provider.api.listener.InitializationStatus;
import de.gematik.ti.cardreader.provider.usb.identos.entities.IdentosCardReader;
import de.gematik.ti.openhealthcard.common.AbstractAndroidCardReaderController;

/**
 * include::{userguide}/UIDECRP_Structure.adoc[tag=IdentosCardReaderController]
 */
@Singleton
public final class IdentosCardReaderController extends AbstractAndroidCardReaderController {
    private static final String ACTION_USB_PERMISSION_IDENTOS = "com.identos.android.USB_PERMISSION";

    private static final Logger LOG = LoggerFactory.getLogger(IdentosCardReaderController.class);

    private static volatile IdentosCardReaderController instance;
    private final Map<Integer, ICardReader> cardReaders = Collections.synchronizedMap(new ArrayMap<>());
    private IdentosUsbReceiver identosUsbReceiver = null;

    private IdentosCardReaderController() {
    }

    /**
     * Returns an instance of IdentosCardReaderController
     * @return this
     */
    public static IdentosCardReaderController getInstance() {
        if (instance == null) {
            instance = new IdentosCardReaderController();
        }
        return instance;
    }

    /**
     Returns a list of connected cardReaders
     * @return cardReaders
     */
    @Override
    public Collection<ICardReader> getCardReaders() {
        checkContext();

        if (cardReaders.isEmpty()) {
            LOG.debug("getCardReaders(): going to read Identos card reader devices");
            readDevices();
        }
        if (identosUsbReceiver == null) {
            createUsbReceiver();
        }

        return cardReaders.values();
    }

    /**
     * Overrides the method `setContext` to set up a USB BroadcastReceiver after Context set
     * @param context Android Application Context
     */
    @Override
    public void setContext(Context context) {
        super.setContext(context);
        if (identosUsbReceiver == null && context != null) {
            createUsbReceiver();
        }
    }

    synchronized void addNewAndInform(final IdentosCardReader cardReader) {
        int id = cardReader.getReader().getDevice().getDeviceId();
        cardReaders.put(id, cardReader);
        InitializationStatus status = InitializationStatus.INIT_NECESSARY;
        if (cardReader.isInitialized()) {
            status = InitializationStatus.INIT_SUCCESS;
        }

        informAboutReaderConnection(cardReader, status);
    }

    void removeAndInform(final UsbDevice device) {
        int id = device.getDeviceId();
        ICardReader cardReader = cardReaders.get(id);
        if (cardReader != null) {
            cardReaders.remove(id);
            informAboutReaderDisconnection(cardReader);
        }
    }

    /**
     * Waits for a USB card reader device to get the permission accessing it. This methods creates a broadcast receiver and calls requestPermission with USB
     * device and the necessary permission string for that particular device. It waits until broadcast was received. Granting information of received broadcast
     * is returned. This method must not be called on main thread in order to prevent blocking it.
     * @param device
     * @return
     *          true if usb permission is granted
     *          false if usb permission is declined
     */
    public boolean waitForPermission(final UsbDevice device) {
        return waitForPermission(device, ACTION_USB_PERMISSION_IDENTOS);
    }

    /**
     * Informs about the connection of a new CardReader and forwards the status of the initialization
     * @param ct
     * @param success
     */
    public synchronized void ctInitCompleted(final IdentosCardReader ct, boolean success) {
        InitializationStatus status = InitializationStatus.INIT_FAILED;
        if (success) {
            status = InitializationStatus.INIT_SUCCESS;
        }

        informAboutReaderConnection(ct, status);
    }

    private void readDevices() {
        List<UsbReader> id100Readers = UsbReader.getUsbReaders(getContext());
        LOG.debug("readDevices(): found {} Identos devices", id100Readers.size());
        for (UsbReader reader : id100Readers) {
            LOG.debug("readDevices(): found device serial number: " + reader.getDevice().getSerialNumber() + "Vendor-ID: " + reader.getDevice().getVendorId()
                    + " Product-Id: " + reader.getDevice().getProductId());
            cardReaders.put(reader.getDevice().getDeviceId(), new IdentosCardReader(getContext(), reader));
        }
    }

    private void createUsbReceiver() {
        checkContext();

        identosUsbReceiver = new IdentosUsbReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction("UsbReceiveActivity: ACTION_USB_DEVICE_ATTACHED");
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        getContext().registerReceiver(identosUsbReceiver, filter);
    }

    public IdentosUsbReceiver getIdentosUsbReceiver() {
        return identosUsbReceiver;
    }
}
