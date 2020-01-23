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

import de.gematik.ti.cardreader.provider.api.ICardReaderController;
import de.gematik.ti.cardreader.provider.api.entities.IProviderDescriptor;
import de.gematik.ti.cardreader.provider.api.entities.ProviderDescriptor;
import de.gematik.ti.cardreader.provider.spi.ICardReaderControllerProvider;

/**
 * include::{userguide}/UIDECRP_Structure.adoc[tag=IdentosCardReaderProvider]
 */
public class IdentosCardReaderProvider implements ICardReaderControllerProvider {
    private final ProviderDescriptor providerDescriptor;

    public IdentosCardReaderProvider() {
        providerDescriptor = new ProviderDescriptor("Gematik Identos-Provider");
        providerDescriptor.setClassName(this.getClass().toString());
        providerDescriptor.setDescription("Dieser Provider stellt die Identos Kartenleser bereit.");
        providerDescriptor.setLicense("Gematik interner Gebrauch, details tbd");
    }

    /**
     * Returns an instance of IdentosCardReaderController.class
     *
     * @return
     *          IdentosCardReaderController
     */
    @Override
    public ICardReaderController getCardReaderController() {
        return IdentosCardReaderController.getInstance();
    }

    /**
     * Returns the Providerdesciptor with short information about the Identos USB Provider
     *
     * @return
     *          Providerdesciptor
     */
    @Override
    public IProviderDescriptor getDescriptor() {
        return providerDescriptor;
    }

}
