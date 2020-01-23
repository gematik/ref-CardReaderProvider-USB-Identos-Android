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

package de.gematik.ti.cardreader.provider.usb.identos.activities;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;

import de.gematik.ti.cardreader.provider.usb.identos.control.IdentosCardReaderController;

public class UsbReceiveActivity extends Activity {

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();

        if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {

            IdentosCardReaderController.getInstance().getIdentosUsbReceiver().onReceive(this, intent);
        }

        finish();
    }
}
