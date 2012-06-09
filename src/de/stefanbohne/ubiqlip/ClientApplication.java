package de.stefanbohne.ubiqlip;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.io.IOUtils;

import de.stefanbohne.ubiqlip.messaging.ClipboardData;
import de.stefanbohne.ubiqlip.messaging.S11nInput;
import de.stefanbohne.ubiqlip.messaging.S11nOutput;

public class ClientApplication {
	private TrayIcon trayIcon;
	private Ubiqlipboard ubiqlip;
	private void createTrayIcon() throws IOException {
		URL url = ClientApplication.class.getResource("ubiqlip.png");
		Image image = ImageIO.read(url);
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ubiqlip.close();
				SystemTray.getSystemTray().remove(trayIcon);
				System.exit(0);
			}
		});
		MenuItem connectToPeerItem = new MenuItem("Connect to Peer");
		connectToPeerItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				String address = JOptionPane.showInputDialog(null,
						"Enter the peer's ip address and port:",
						"Connect To Peer",
						JOptionPane.QUESTION_MESSAGE);
				if (address != null) {
					if (!address.contains(":"))
						JOptionPane.showMessageDialog(null,
								"You must enter a port.",
								"Connect to Peer",
								JOptionPane.ERROR_MESSAGE);
					else
						ubiqlip.connectToPeer(
								address.substring(0, address.lastIndexOf(':')),
								Integer.parseInt(address.substring(address.lastIndexOf(':') + 1)));
				}
			}
		});
		PopupMenu menu = new PopupMenu();
		menu.add(connectToPeerItem);
		menu.add("Connect to server");
		menu.addSeparator();
		menu.add(exitItem);
		trayIcon = new TrayIcon(image, "Ubiqlip", menu);
	}
	private class TrayIconHandler extends Handler {

		@Override
		public void close() {}

		@Override
		public void flush() {}

		@Override
		public void publish(LogRecord record) {
			trayIcon.displayMessage("Ubiqlip", record.getMessage(),
					record.getLevel().intValue() >= Level.SEVERE.intValue() ? TrayIcon.MessageType.ERROR :
					record.getLevel().intValue() >= Level.WARNING.intValue() ? TrayIcon.MessageType.WARNING :
					record.getLevel().intValue() >= Level.INFO.intValue() ? TrayIcon.MessageType.INFO :
					TrayIcon.MessageType.NONE);
		}

	}
	private ClipboardData transferable2ClipboardData(Transferable transferable) {
		Map<String, byte[]> data = new HashMap<String, byte[]>();
		for (DataFlavor flavor : transferable.getTransferDataFlavors())
			try {
				if (flavor.getDefaultRepresentationClass() == String.class) {
					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					(new S11nOutput(bytes)).writeString((String)transferable.getTransferData(flavor));
					if (bytes.size() > 0)
						data.put(flavor.getMimeType(), bytes.toByteArray());
				} else if (flavor.getDefaultRepresentationClass() == InputStream.class) {
					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					IOUtils.copy((InputStream)transferable.getTransferData(flavor), bytes);
					if (bytes.size() > 0)
						data.put(flavor.getMimeType(), bytes.toByteArray());
				}
			} catch (Exception e) {
				// ignore
			}
		return new ClipboardData(data);
	}
	private Transferable clipboardData2Transferable(ClipboardData data) {
		final Map<DataFlavor, Object> transferData = new HashMap<DataFlavor, Object>();
		for (Map.Entry<String, byte[]> d : data.data.entrySet())
			try {
				DataFlavor flavor = new DataFlavor(d.getKey());
				if (flavor.getDefaultRepresentationClass() == String.class) {
					transferData.put(flavor, new S11nInput(new ByteArrayInputStream(d.getValue())).readString());
				} else if (flavor.getDefaultRepresentationClass() == InputStream.class) {
					transferData.put(flavor, new ByteArrayInputStream(d.getValue()));
				}
			} catch (Exception e) {
				// ignore
			}
		return new Transferable() {
			@Override
			public Object getTransferData(DataFlavor flavor)
					throws UnsupportedFlavorException, IOException {
				if (!isDataFlavorSupported(flavor))
					throw new UnsupportedFlavorException(flavor);
				return transferData.get(flavor);
			}
			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return transferData.keySet().toArray(new DataFlavor[0]);
			}
			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return transferData.containsKey(flavor);
			}

		};
	}
	public void run() throws AWTException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		if (new File("logging.properties").exists()) {
			InputStream loggingConfig = null;
			try {
				loggingConfig = new FileInputStream("logging.properties");
				LogManager.getLogManager().readConfiguration(loggingConfig);
			} catch (Exception e) {
				// ingore
			} finally {
				IOUtils.closeQuietly(loggingConfig);
			}
		}
		final java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		createTrayIcon();
		LogManager.getLogManager().getLogger("").addHandler(new TrayIconHandler());
		ubiqlip = new Ubiqlipboard(
			"stefan",
			Ubiqlipboard.emptyPassword,
			new ClipboardData(Collections.<String, byte[]> emptyMap()),
			new UbiqlipboardChangedListener() {
				@Override
				public void uniqlipboardChanged(ClipboardData clipboardData) {
					synchronized (ClientApplication.this) {
						boolean succeeded = false;
						while (!succeeded)
							try {
								clipboardData.debugPrint();
								clipboard.setContents(clipboardData2Transferable(clipboardData), null);
								assert(transferable2ClipboardData(clipboard.getContents(null)).equals(clipboardData));
								succeeded = true;
							} catch (IllegalStateException e) {}
					}
				}
			});
		StringBuilder addresses = new StringBuilder();
		for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();)
			for (Enumeration<InetAddress> e2 = e.nextElement().getInetAddresses(); e2.hasMoreElements();) {
				InetAddress ia = e2.nextElement();
				if (!ia.isLoopbackAddress() && ia instanceof Inet4Address) {
					if (addresses.length() != 0)
						addresses.append(", ");
					addresses.append(ia.getHostAddress());
				}
			}
		trayIcon.setToolTip(String.format("%s on %s port %s",
				trayIcon.getToolTip(),
				addresses.toString(),
				ubiqlip.server.getLocalPort()));
		SystemTray.getSystemTray().add(trayIcon);

		ClipboardData debounce = null;
		int debounceCountdown = 0;
		while (true) {
			synchronized (this) {
				Transferable transferable = null;
				try {
					transferable = clipboard.getContents(null);
				} catch (IllegalStateException e) {
					// ignore
				}
				if (transferable != null) {
					ClipboardData tmp = transferable2ClipboardData(transferable);
					if (debounce == null || !tmp.equals(debounce)) {
						debounce = tmp;
						debounceCountdown = 3;
					} else if (debounceCountdown > 0) {
						debounceCountdown -= 1;
						if (debounceCountdown == 0) {
							ubiqlip.setClipboardData(debounce);
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								// ignore
							}
						}
					}
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
	public static void main(String[] args) throws IOException, AWTException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		(new ClientApplication()).run();
	}
}
