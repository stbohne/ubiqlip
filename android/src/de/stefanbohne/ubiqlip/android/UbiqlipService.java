package de.stefanbohne.ubiqlip.android;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.ClipboardManager;
import de.stefanbohne.ubiqlip.Ubiqlipboard;
import de.stefanbohne.ubiqlip.UbiqlipboardChangedListener;
import de.stefanbohne.ubiqlip.messaging.ClipboardData;
import de.stefanbohne.ubiqlip.messaging.MIMEParse;

public class UbiqlipService extends Service {
	private static Logger logger = LoggerFactory.getLogger(UbiqlipService.class);

	public class Binder extends android.os.Binder {
		public UbiqlipService getService() {
			return UbiqlipService.this;
		}
	}

	private final Binder binder = new Binder();
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private class NotificationHandler extends Handler {

		private final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		private int id = 0;

		@Override
		public void close() {}

		@Override
		public void flush() {}

		@Override
		public void publish(LogRecord record) {
			Notification notification = new Notification(R.drawable.ic_stat_ubiqlip, record.getMessage(), record.getMillis());
			notification.setLatestEventInfo(UbiqlipService.this, "Ubiqlip", record.getMessage(),
					PendingIntent.getActivity(UbiqlipService.this, 0,
							new Intent(UbiqlipService.this, UbiqlipActivity.class), 0));
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notificationManager.notify(id, notification);
			id += 1;
		}

	}

	private ClipboardManager clipboardManager;
	public Ubiqlipboard ubiqlipboard;

	class AndroidUbiqlipboardChangedListener implements UbiqlipboardChangedListener {
		@Override
		public void uniqlipboardChanged(ClipboardData clipboardData) {
			String key = clipboardData.bestMatch("text/plain;charset=UTF-8;q=1,text/*;charset=UTF-8;q=0.9,text/*;q=0.1");
			if (key.length() > 0) {
				MIMEParse.ParseResults mime = MIMEParse.parseMimeType(key);
				try {
					clipboardManager.setText(IOUtils.toString(clipboardData.data.get(key), "UTF-8"));
				} catch (IOException e) {
					logger.warn("Error reading clipboard data", e);
				}
			}
		}
	}

	ClipboardData readClipboardData() {
		if (clipboardManager.hasText()) {
			try {
				return new ClipboardData(Collections.singletonMap("text/plain; charset=UTF-8", IOUtils.toByteArray(new StringReader(clipboardManager.getText().toString()), "UTF-8")));
			} catch (IOException e) {
				logger.warn("Error converting clipboard data", e);
			}
		}
		return new ClipboardData(Collections.<String, byte[]> emptyMap());
	}

	@Override
	public void onCreate() {
		clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
		LogManager.getLogManager().getLogger("").addHandler(new NotificationHandler());
		try {
			ubiqlipboard = new Ubiqlipboard(
					"stefan",
					Ubiqlipboard.emptyPassword,
					readClipboardData(),
					new AndroidUbiqlipboardChangedListener());
		} catch (IOException e) {
			logger.error("Error binding server port", e);
		}

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!ubiqlipboard.server.isClosed()) {
					ubiqlipboard.setClipboardData(readClipboardData());
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						logger.warn("Unexpected exception", e);
					}
				}
			}
		});
		thread.setName("ubiqlip");
		thread.start();
	}

	@Override
	public void onDestroy() {
		ubiqlipboard.close();
	}

}
