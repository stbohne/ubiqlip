package de.stefanbohne.ubiqlip;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import de.stefanbohne.ubiqlip.messaging.ClipboardData;
import de.stefanbohne.ubiqlip.messaging.S11nInput;
import de.stefanbohne.ubiqlip.messaging.S11nOutput;

public class ClipboardMonitor {
	private static ClipboardData transferable2ClipboardData(Transferable transferable) {
		Map<String, byte[]> data = new HashMap<String, byte[]>();
		for (DataFlavor flavor : transferable.getTransferDataFlavors())
			try {
				if (flavor.getDefaultRepresentationClass() == String.class) {
					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					(new S11nOutput(bytes)).writeString((String)transferable.getTransferData(flavor));
					data.put(flavor.getMimeType(), bytes.toByteArray());
				} else if (flavor.getDefaultRepresentationClass() == InputStream.class) {
					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					IOUtils.copy((InputStream)transferable.getTransferData(flavor), bytes);
					data.put(flavor.getMimeType(), bytes.toByteArray());
				}
			} catch (Exception e) {
				// ignore
			}
		return new ClipboardData(data);
	}
	private static Transferable clipboardData2Transferable(ClipboardData data) {
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
	public static void main(String[] args) {
		final java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		ClipboardData old = new ClipboardData(Collections.<String, byte[]> emptyMap());
		while (true) {
			Transferable transferable = null;
			try {
				transferable = clipboard.getContents(null);
			} catch (IllegalStateException e) {}
			if (transferable != null) {
				ClipboardData cur = transferable2ClipboardData(transferable);
				if (!old.equals(cur)) {
					System.out.println("");
					System.out.println(new Date());
					for (DataFlavor flavor : transferable.getTransferDataFlavors()) {
						try {
							System.out.println(String.format("%s %s %s", flavor.getMimeType(), flavor.getRepresentationClass(), transferable.getTransferData(flavor).getClass()));
						} catch (Exception e) {
							System.out.println(e);
						}
					}
					System.out.println("---");
					old = cur;
				}
			}
		}
	}
}
