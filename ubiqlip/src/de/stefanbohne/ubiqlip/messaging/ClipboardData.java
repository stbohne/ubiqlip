package de.stefanbohne.ubiqlip.messaging;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;


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
	public String toString() {
		StringBuilder result = new StringBuilder("ClipboardData(");
		for (Map.Entry<String, byte[]> e : data.entrySet())
			result.append(e.getKey()).append(":").append(ArrayUtils.toString(e.getValue()));
		return result.append(")").toString();
	}
	public ClipboardData canonicalized() {
		Map<String, MIMEParse.ParseResults> parsed = new HashMap<String, MIMEParse.ParseResults>();
		Map<String, List<String>> byType = new HashMap<String, List<String>>();
		for (Map.Entry<String, byte[]> e : data.entrySet()) {
			MIMEParse.ParseResults p = MIMEParse.parseMimeType(e.getKey());
			parsed.put(e.getKey(), p);
			if (!byType.containsKey(p.type + "/" + p.subType))
				byType.put(p.type + "/" + p.subType, new ArrayList<String>());
			byType.get(p.type + "/" + p.subType).add(e.getKey());
		}
		Map<String, byte[]> result = new HashMap<String, byte[]>();
		for (Map.Entry<String, List<String>> e : byType.entrySet()) {
			String key = MIMEParse.bestMatch(e.getValue(), "text/*;charset=UTF-8;q=1,text/*;charset=UTF-16LE;q=0.9,text/*;charset=*;q=0.5,*/*;q=0.1");
			if (parsed.get(key).type == "text") {
				byte[] bytes;
				Charset charset = parsed.get(key).params.containsKey("charset") ? Charset.forName(parsed.get(key).params.get("charset")) : Charset.defaultCharset();
				if (charset != Charset.forName("UTF-8")) {
					try {
						bytes = IOUtils.toByteArray(new StringReader(IOUtils.toString(data.get(key), charset.name())), Charset.forName("UTF-8"));
					} catch (IllegalCharsetNameException e1) {
						throw new RuntimeException(e1);
					} catch (UnsupportedCharsetException e1) {
						throw new RuntimeException(e1);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						continue;
					}
				} else
					bytes = data.get(key);
				result.put("text/" + parsed.get(key).subType + ";charset=UTF-8", bytes);
			} else
				result.put(parsed.get(key).type + "/" + parsed.get(key).subType + ";charset=UTF-8", data.get(key));
		}
		return new ClipboardData(result);
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
				System.out.print(String.format("%02X", d.getValue()[i]));
			System.out.println();
		}
	}

	public String bestMatch(String mediaRange) {
		return MIMEParse.bestMatch(data.keySet(), mediaRange);
	}
	public boolean lessEquals(ClipboardData that) {
		if (this == that)
			return true;
		if (data.size() > that.data.size())
			return false;
		for (Map.Entry<String, byte[]> d : data.entrySet())
			if (!that.data.containsKey(d.getKey()))
				return false;
			else if (d.getValue().length > 0 && !Arrays.equals(d.getValue(), that.data.get(d.getKey())))
				return false;
		return true;
	}

}
