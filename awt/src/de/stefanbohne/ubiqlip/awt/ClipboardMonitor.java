package de.stefanbohne.ubiqlip.awt;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Collections;
import java.util.Date;

import de.stefanbohne.ubiqlip.messaging.ClipboardData;

public class ClipboardMonitor {
	public static void main(String[] args) {
		final java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		ClipboardData old = new ClipboardData(Collections.<String, byte[]> emptyMap());
		while (true) {
			Transferable transferable = null;
			try {
				transferable = clipboard.getContents(null);
			} catch (IllegalStateException e) {}
			if (transferable != null) {
				ClipboardData cur = ClientApplication.transferable2ClipboardData(transferable).canonicalized();
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
					cur.debugPrint();
					old = cur;
				}
			}
		}
	}
}
