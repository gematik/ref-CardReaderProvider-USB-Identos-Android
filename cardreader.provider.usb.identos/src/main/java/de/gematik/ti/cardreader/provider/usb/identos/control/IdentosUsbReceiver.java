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

import com.identos.android.id100.library.ccid.UsbReader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import de.gematik.ti.cardreader.provider.usb.identos.entities.IdentosCardReader;

/**
 * include::{userguide}/UIDECRP_Structure.adoc[tag=IdentosUsbReceiver]
 */
public class IdentosUsbReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(IdentosUsbReceiver.class);

    public IdentosUsbReceiver() {
    }

    /**
     * Handled the received intent actions
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (device != null && context != null) {
            switch (intent.getAction()) {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    onDeviceAttached(context, device);
                    break;

                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    onDeviceDetached(device);
                    break;
            }
        }
    }

    private void onDeviceAttached(Context context, UsbDevice device) {
        if (UsbReader.isSupportedReader(device)) {
            new Thread(() -> {
                LOG.debug("onDeviceAttached: " + device.toString());
                UsbReader reader = new UsbReader(device);
                IdentosCardReader cardReader = new IdentosCardReader(context, reader);
                IdentosCardReaderController.getInstance().addNewAndInform(cardReader);
            }).start();
        }

    }

    private void onDeviceDetached(UsbDevice device) {
        if (UsbReader.isSupportedReader(device)) {
            new Thread(() -> {
                LOG.debug("onDeviceDetached: " + device.toString());
                IdentosCardReaderController.getInstance().removeAndInform(device);
            }).start();
        }
    }
}
