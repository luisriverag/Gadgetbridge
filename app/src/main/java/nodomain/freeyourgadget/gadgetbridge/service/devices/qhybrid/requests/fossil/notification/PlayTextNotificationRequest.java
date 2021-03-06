package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;

public class PlayTextNotificationRequest extends PlayNotificationRequest {
    public PlayTextNotificationRequest(String packageName, String sender, String message, int messageId, FossilWatchAdapter adapter) {
        super(3, 2, packageName, sender, message, messageId, adapter);
    }

    public PlayTextNotificationRequest(String packageName, FossilWatchAdapter adapter) {
        super(3, 2, packageName, adapter);
    }
}
