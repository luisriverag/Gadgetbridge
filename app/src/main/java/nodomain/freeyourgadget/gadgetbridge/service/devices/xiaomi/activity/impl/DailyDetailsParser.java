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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DailyDetailsParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(DailyDetailsParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        final int version = fileId.getVersion();
        final int headerSize;
        final int recordSize;
        switch (version) {
            case 1:
            case 2:
                headerSize = 4;
                recordSize = 10;
                break;
            case 3:
                headerSize = 5;
                recordSize = 12;
                break;
            default:
                LOG.warn("Unable to parse daily details version {}", fileId.getVersion());
                return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        final byte[] header = new byte[headerSize];
        buf.get(header);

        if ((buf.limit() - buf.position()) % recordSize != 0) {
            LOG.warn("Remaining data in the buffer is not a multiple of {}", recordSize);
            return false;
        }

        final List<XiaomiActivitySample> samples = new ArrayList<>();

        while (buf.position() < buf.limit()) {
            final XiaomiActivitySample sample = new XiaomiActivitySample();

            sample.setSteps(buf.getShort());

            final byte[] unknown1 = new byte[4];
            buf.get(unknown1);  // TODO intensity and kind?

            sample.setHeartRate(buf.get() & 0xff);

            final byte[] unknown2 = new byte[3];
            buf.get(unknown2);  // TODO intensity and kind?

            if (version == 3) {
                sample.setSpo2(buf.get() & 0xff);
                sample.setStress(buf.get() & 0xff);
            }

            samples.add(sample);
        }

        // save all the samples that we got
        final Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(fileId.getTimestamp());

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final GBDevice gbDevice = support.getDevice();
            final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
            final SampleProvider<XiaomiActivitySample> sampleProvider = (SampleProvider<XiaomiActivitySample>) coordinator.getSampleProvider(gbDevice, session);
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            for (final XiaomiActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setTimestamp((int) (timestamp.getTimeInMillis() / 1000));
                sample.setProvider(sampleProvider);

                timestamp.add(Calendar.MINUTE, 1);
            }
            sampleProvider.addGBActivitySamples(samples.toArray(new XiaomiActivitySample[0]));

            timestamp.add(Calendar.MINUTE, -1);

            return true;
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving activity samples", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving activity samples", e);
            return false;
        }
    }
}