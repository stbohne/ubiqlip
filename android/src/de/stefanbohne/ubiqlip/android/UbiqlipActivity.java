package de.stefanbohne.ubiqlip.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.stefanbohne.ubiqlip.LongOperation;

public class UbiqlipActivity extends Activity {
	private UbiqlipService service;
	private Button okButton;
	private TextView peerAddress;
	private ProgressBar progress;
	private TextView progressText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(this, UbiqlipService.class));
		setContentView(R.layout.main);
		okButton = (Button)findViewById(R.id.okButton);
		okButton.setEnabled(false);
		peerAddress = (TextView)findViewById(R.id.peerAddress);
		progress = (ProgressBar)findViewById(R.id.progress);
		progressText = (TextView)findViewById(R.id.progressText);

		bindService(new Intent(this, UbiqlipService.class), new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				okButton.setEnabled(true);
				UbiqlipActivity.this.service = ((UbiqlipService.Binder)service).getService();
			}
			@Override
			public void onServiceDisconnected(ComponentName name) {

			}
		}, 0);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				progress.setVisibility(View.VISIBLE);
				progress.setProgress(0);
				progressText.setVisibility(View.VISIBLE);
				String address = peerAddress.getText().toString();
				if (!address.matches("[^:]+:[0-9]+")) {
					progressText.setText("Invalid address format.");
					return;
				}

				service.ubiqlipboard.connectToPeer(
					address.substring(0, address.indexOf(':')),
				   	Integer.parseInt(address.substring(address.indexOf(':') + 1)),
				   	new LongOperation() {
						@Override
						public void setText(final String title) {
							progressText.post(new Runnable() {
								@Override
								public void run() { progressText.append(title + "\n"); }
							});
						}
						@Override
						public void setProgress(final double p) {
							progress.post(new Runnable() {
								@Override
								public void run() { progress.setProgress((int)(p * 100)); }
							});
						}
						@Override
						public void finish(final boolean success) {
							okButton.post(new Runnable() {
								@Override
								public void run() {
									progress.setProgress(success ? 1 : 0);
									okButton.setEnabled(true);
									peerAddress.setEnabled(true);
								}
							});
						}
					});
				okButton.setEnabled(false);
				peerAddress.setEnabled(false);
			}
		});
	}

}
