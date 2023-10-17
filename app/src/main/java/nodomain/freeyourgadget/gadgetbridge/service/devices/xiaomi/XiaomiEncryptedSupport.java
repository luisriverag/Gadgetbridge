/*  Copyright (C) 2023 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;

public class XiaomiEncryptedSupport extends XiaomiSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiEncryptedSupport.class);

    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb";

    public static final UUID UUID_SERVICE_XIAOMI_FE95 = UUID.fromString((String.format(BASE_UUID, "fe95")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0050 = UUID.fromString((String.format(BASE_UUID, "0050")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_COMMAND_READ = UUID.fromString((String.format(BASE_UUID, "0051")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_COMMAND_WRITE = UUID.fromString((String.format(BASE_UUID, "0052")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_ACTIVITY_DATA = UUID.fromString((String.format(BASE_UUID, "0053")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0054 = UUID.fromString((String.format(BASE_UUID, "0054")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_DATA_UPLOAD = UUID.fromString((String.format(BASE_UUID, "0055")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0056 = UUID.fromString((String.format(BASE_UUID, "0056")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0057 = UUID.fromString((String.format(BASE_UUID, "0057")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0058 = UUID.fromString((String.format(BASE_UUID, "0058")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0059 = UUID.fromString((String.format(BASE_UUID, "0059")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_005A = UUID.fromString((String.format(BASE_UUID, "005a")));

    public static final UUID UUID_SERVICE_XIAOMI_FDAB = UUID.fromString((String.format(BASE_UUID, "fdab")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0001 = UUID.fromString((String.format(BASE_UUID, "0001")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0002 = UUID.fromString((String.format(BASE_UUID, "0002")));
    public static final UUID UUID_CHARACTERISTIC_XIAOMI_UNKNOWN_0003 = UUID.fromString((String.format(BASE_UUID, "0003")));

    public XiaomiEncryptedSupport() {
        super();
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_HUMAN_INTERFACE_DEVICE);
        addSupportedService(UUID_SERVICE_XIAOMI_FE95);
        addSupportedService(UUID_SERVICE_XIAOMI_FDAB);
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        final BluetoothGattCharacteristic btCharacteristicCommandRead = getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_COMMAND_READ);
        final BluetoothGattCharacteristic btCharacteristicCommandWrite = getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_COMMAND_WRITE);
        final BluetoothGattCharacteristic btCharacteristicActivityData = getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_ACTIVITY_DATA);
        final BluetoothGattCharacteristic btCharacteristicDataUpload = getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_DATA_UPLOAD);

        if (btCharacteristicCommandRead == null || btCharacteristicCommandWrite == null) {
            LOG.warn("Characteristics are null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
            return builder;
        }

        // TODO move this initialization to upstream class
        this.characteristicCommandRead = new XiaomiCharacteristic(this, btCharacteristicCommandRead, authService);
        this.characteristicCommandRead.setEncrypted(true);
        this.characteristicCommandRead.setHandler(this::handleCommandBytes);
        this.characteristicCommandWrite = new XiaomiCharacteristic(this, btCharacteristicCommandWrite, authService);
        this.characteristicCommandRead.setEncrypted(true);
        this.characteristicActivityData = new XiaomiCharacteristic(this, btCharacteristicActivityData, authService);
        this.characteristicActivityData.setHandler(healthService.getActivityFetcher()::addChunk);
        this.characteristicCommandRead.setEncrypted(true);
        this.characteristicDataUpload = new XiaomiCharacteristic(this, btCharacteristicDataUpload, authService);
        this.characteristicCommandRead.setEncrypted(true);

        // FIXME why is this needed?
        getDevice().setFirmwareVersion("...");
        //getDevice().setFirmwareVersion2("...");

        builder.requestMtu(247);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_COMMAND_READ), true);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_COMMAND_WRITE), true);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_ACTIVITY_DATA), true);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_XIAOMI_DATA_UPLOAD), true);

        authService.startEncryptedHandshake(builder);

        return builder;
    }
}