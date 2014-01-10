package cz.digitalscope.abradio.icecast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IcyStreamMeta {

	protected URL streamUrl;
	private Map<String, String> metadata;
	private boolean isError;

	public IcyStreamMeta(URL streamUrl) {
		setStreamUrl(streamUrl);

		isError = false;
	}

	/**
	 * Get artist using stream's title
	 * 
	 * @return String
	 * @throws IOException
	 */
	public String getArtist() {
		Map<String, String> data;
		String title = "";
		try {
			data = getMetadata();

			if (!data.containsKey("StreamTitle"))
				return "";

			String streamTitle = data.get("StreamTitle");
			int minusIndex = streamTitle.indexOf("-");
			if (minusIndex > 0) {
				title = streamTitle.substring(0, minusIndex);
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
		return title.trim();
	}

	/**
	 * Get title using stream's title
	 * 
	 * @return String
	 * @throws IOException
	 */
	public String getTitle() {
		Map<String, String> data;
		String artist = "";
		try {
			data = getMetadata();

			if (!data.containsKey("StreamTitle"))
				return "";

			String streamTitle = data.get("StreamTitle");
			artist = streamTitle.substring(streamTitle.indexOf("-") + 1);
		} catch (IOException e) {

			e.printStackTrace();
		}
		return artist.trim();
	}

	public Map<String, String> getMetadata() throws IOException {
		if (metadata == null) {
			refreshMeta();
		}

		return metadata;
	}

	public void refreshMeta() {
		retreiveMetadata();
	}

	private void retreiveMetadata() {
		URLConnection con;
		try {
			con = streamUrl.openConnection();

			con.setRequestProperty("Icy-MetaData", "1");
			con.setRequestProperty("Connection", "close");
//			con.setRequestProperty("Accept", null);
			con.connect();

			int metaDataOffset = 0;
			Map<String, List<String>> headers = con.getHeaderFields();
			InputStream stream = con.getInputStream();

			if (headers.containsKey("icy-metaint")) {
				// Headers are sent via HTTP
				metaDataOffset = Integer.parseInt(headers.get("icy-metaint")
						.get(0));
			} else {
				// Headers are sent within a stream
				StringBuilder strHeaders = new StringBuilder();
				char c;
				while ((c = (char) stream.read()) != -1) {
					strHeaders.append(c);
					if (strHeaders.length() > 5
							&& (strHeaders.substring((strHeaders.length() - 4),
									strHeaders.length()).equals("\r\n\r\n"))) {
						// end of headers
						break;
					}
				}

				// Match headers to get metadata offset within a stream
				Pattern p = Pattern
						.compile("\\r\\n(icy-metaint):\\s*(.*)\\r\\n");
				Matcher m = p.matcher(strHeaders.toString());
				if (m.find()) {
					metaDataOffset = Integer.parseInt(m.group(2));
				}
			}

			// In case no data was sent
			if (metaDataOffset == 0) {
				isError = true;
				return;
			}

			// Read metadata
			int b;
			int count = 0;
			int metaDataLength = 4080; // 4080 is the max length
			boolean inData = false;
			StringBuilder metaData = new StringBuilder();
			// Stream position should be either at the beginning or right after
			// headers
			while ((b = stream.read()) != -1) {
				count++;

				// Length of the metadata
				if (count == metaDataOffset + 1) {
					metaDataLength = b * 16;
				}

				if (count > metaDataOffset + 1
						&& count < (metaDataOffset + metaDataLength)) {
					inData = true;
				} else {
					inData = false;
				}
				if (inData) {
					if (b != 0) {
						metaData.append((char) b);
					}
				}
				if (count > (metaDataOffset + metaDataLength)) {
					break;
				}

			}

			// Set the data
			metadata = IcyStreamMeta.parseMetadata(metaData.toString());

			// Close
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isError() {
		return isError;
	}

	public URL getStreamUrl() {
		return streamUrl;
	}

	public void setStreamUrl(URL streamUrl) {
		this.metadata = null;
		this.streamUrl = streamUrl;
		this.isError = false;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> parseMetadata(String metaString) {
		@SuppressWarnings("rawtypes")
		Map<String, String> metadata = new HashMap();
		String[] metaParts = metaString.split(";");
		Pattern p = Pattern.compile("^([a-zA-Z]+)=\\'([^\\']*)\\'$");
		Matcher m;
		String metaname = "", meta = "";
		for (int i = 0; i < metaParts.length; i++) {
			m = p.matcher(metaParts[i]);
			if (m.find()) {
				metaname = m.group(1);
				meta = m.group(2);
			} else {
				Pattern p1 = Pattern.compile("StreamTitle=\\s*(.*)$");
				Matcher m1 = p1.matcher(metaParts[i]);
				if (m1.find()) {
					metaname = "StreamTitle";
					meta = m1.group(1);
				}
			}
			if (meta != "" || metaname != "") {
				String str = "";
				meta.trim();

				for (int i1 = 0; i1 < meta.length(); i1++) {
					if (!(meta.charAt(i1) == 39 && (i1 == 0 || i1 == meta
							.length() - 1))) {
						if (i1 + 1 < meta.length() && meta.charAt(i1) >= 195
								&& meta.charAt(i1) <= 197) {
							str += convert(meta.charAt(i1), meta.charAt(i1 + 1));
							i1++;
						} else
							str += meta.charAt(i1);
					}
				}

				metadata.put(metaname, str);
			}
		}

		return metadata;
	}

	private static String convert(char ch1, char ch2) {
		String znak;
		switch (ch1) {
		case 195:
			switch (ch2) {
			case 173:
				znak = "í";
				break;
			case 161:
				znak = "á";
				break;
			case 169:
				znak = "é";
				break;
			case 189:
				znak = "ý";
				break;
			case 186:
				znak = "ú";
				break;
			case 157:
				znak = "Ý";
				break;
			case 129:
				znak = "Á";
				break;
			case 141:
				znak = "Í";
				break;
			case 137:
				znak = "É";
				break;
			case 154:
				znak = "Ú";
				break;
			case 179:
				znak = "ó";
				break;
			case 147:
				znak = "Ó";
				break;
			case 130:
				znak = "'";
				break;

			default:
				znak = Character.toString(ch1) + "-" + Character.toString(ch2);
				break;
			}
			break;
		case 197:
			switch (ch2) {
			case 174:
				znak = "Ů";
				break;
			case 189:
				znak = "Ž";
				break;
			case 160:
				znak = "Š";
				break;
			case 161:
				znak = "š";
				break;
			case 153:
				znak = "ř";
				break;
			case 190:
				znak = "ž";
				break;
			case 175:
				znak = "ů";
				break;
			case 152:
				znak = "Ř";
				break;
			case 165:
				znak = "ť";
				break;
			case 164:
				znak = "Ť";
				break;
			case 136:
				znak = "ň";
				break;
			case 135:
				znak = "Ň";
				break;

			default:
				znak = Character.toString(ch1) + "-" + Character.toString(ch2);
				break;
			}
			break;
		case 196:
			switch (ch2) {
			case 0xa1:
				znak = "š";
				break;
			case 0xaf:
				znak = "ů";
				break;
			case 0x8c:
				znak = "Č";
				break;
			case 0x8d:
				znak = "č";
				break;
			case 0x99:
				znak = "ř";
				break;
			case 165:
				znak = "ť";
				break;
			case 155:
				znak = "ě";
				break;
			case 154:
				znak = "Ě";
				break;
			case 143:
				znak = "ď";
				break;
			case 142:
				znak = "Ď";
				break;
			case 190:
				znak = "ľ";
				break;
			case 189:
				znak = "Ľ";
				break;

			default:
				znak = Character.toString(ch1) + "-" + Character.toString(ch2);
				break;
			}
			break;

		default:
			// neni definovan
			znak = Character.toString(ch1) + Character.toString(ch2);
			break;
		}

		return znak;
	}
}
