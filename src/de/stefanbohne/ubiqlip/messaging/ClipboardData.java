package de.stefanbohne.ubiqlip.messaging;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ClipboardData implements S11nable {

	public final Map<String, byte[]> data;

	public ClipboardData(Map<String, byte[]> data) {
		this.data = Collections.unmodifiableMap(data);
	}
	public ClipboardData(S11nInput in) throws IOException {
		Map<String, byte[]> data = new HashMap<String, byte[]>();
		for (int count = in.readInt(); count > 0; count -= 1) {
			data.put(in.readString(),
					 in.readByteArray());
		}
		this.data = Collections.unmodifiableMap(data);
	}
	@Override
	public void s5ize(S11nOutput out) throws IOException {
		out.writeInt(data.size());
		for (Map.Entry<String, byte[]> d : data.entrySet()) {
			out.writeString(d.getKey());
			out.writeByteArray(d.getValue());
		}
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;
		else if (that.getClass() == ClipboardData.class) {
			if (data.size() != ((ClipboardData)that).data.size())
				return false;
			for (Map.Entry<String, byte[]> d : data.entrySet())
				if (!((ClipboardData)that).data.containsKey(d.getKey()))
					return false;
				else if (!Arrays.equals(d.getValue(), ((ClipboardData)that).data.get(d.getKey())))
					return false;
			return true;
		} else
			return super.equals(that);
	}
	@Override
	public int hashCode() {
		return data.hashCode();
	}

	public void debugPrint() {
		for (Map.Entry<String, byte[]> d : data.entrySet()) {
			System.out.print(String.format("%s %s ", d.getKey(), d.getValue().length));
			for (int i = 0; i < d.getValue().length; i += 1)
				System.out.print((char)d.getValue()[i]);
			System.out.println();
		}
	}

}
