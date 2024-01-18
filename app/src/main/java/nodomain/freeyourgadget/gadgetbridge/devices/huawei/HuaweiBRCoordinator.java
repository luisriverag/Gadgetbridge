/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiBRSupport;

public abstract class HuaweiBRCoordinator extends AbstractBLClassicDeviceCoordinator implements HuaweiCoordinatorSupplier {

    private final HuaweiCoordinator huaweiCoordinator = new HuaweiCoordinator(this);
    private GBDevice device;

    @Override
    public HuaweiCoordinator getHuaweiCoordinator() {
        return huaweiCoordinator;
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid huaweiService = new ParcelUuid(HuaweiConstants.UUID_SERVICE_HUAWEI_SERVICE);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(huaweiService).build();
        return Collections.singletonList(filter);
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new HuaweiSettingsCustomizer(device);
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }
    
    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_ASK;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        long deviceId = device.getId();
        QueryBuilder<?> qb = session.getHuaweiActivitySampleDao().queryBuilder();
        qb.where(HuaweiActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();

        QueryBuilder<HuaweiWorkoutSummarySample> qb2 = session.getHuaweiWorkoutSummarySampleDao().queryBuilder();
        List<HuaweiWorkoutSummarySample> workouts = qb2.where(HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(deviceId)).build().list();
        for (HuaweiWorkoutSummarySample sample : workouts) {
            session.getHuaweiWorkoutDataSampleDao().queryBuilder().where(
                    HuaweiWorkoutDataSampleDao.Properties.WorkoutId.eq(sample.getWorkoutId())
            ).buildDelete().executeDeleteWithoutDetachingEntities();
        }

        session.getHuaweiWorkoutSummarySampleDao().queryBuilder().where(HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();

        session.getBaseActivitySummaryDao().queryBuilder().where(BaseActivitySummaryDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public String getManufacturer() {
        return "Huawei";
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device, int position) {
        return huaweiCoordinator.supportsSmartAlarm(device, position);
    }

    @Override
    public boolean forcedSmartWakeup(GBDevice device, int alarmPosition) {
        return huaweiCoordinator.forcedSmartWakeup(device, alarmPosition);
    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }

    @Override
    public boolean supportsAlarmSnoozing() {
        return false;
    }

    @Override
    public boolean supportsAlarmTitle(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAlarmDescription(GBDevice device) {
        // TODO: only name is supported
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return huaweiCoordinator.supportsWeather();
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @Override
    public boolean supportsAppsManagement(GBDevice device) {
        return false;
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return huaweiCoordinator.getAlarmSlotCount(device);
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsMusicInfo() {
        return getHuaweiCoordinator().supportsMusic();
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new HuaweiSampleProvider(device, session);
    }

    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{};
    }

    @Override
    public HuaweiDeviceType getHuaweiType() {
        return HuaweiDeviceType.BR;
    }

    @Override
    public void setDevice(GBDevice device) {
        this.device = device;
    }

    @Override
    public GBDevice getDevice() {
        return this.device;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return HuaweiBRSupport.class;
    }
}
