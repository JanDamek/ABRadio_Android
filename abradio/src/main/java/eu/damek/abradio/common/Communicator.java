package eu.damek.abradio.common;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public class Communicator {

	public String executeHttpGet(String url) {
		try {
			URL address = new URL(url);
			final URLConnection urlConnection = address.openConnection();
			final InputStream content = (InputStream) urlConnection.getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(content));
			StringBuilder total = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				total.append(line);
			}
			return total.toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
}
