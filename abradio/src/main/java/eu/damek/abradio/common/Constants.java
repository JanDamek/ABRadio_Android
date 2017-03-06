package eu.damek.abradio.common;

public class Constants {

	public static final String SETTING_NAME = "nasatveni";

	public static final String SERVICE_PREFIX = "eu.damek.abradio.playbackservice.";
	public static final String SERVICE_CHANGE_NAME = SERVICE_PREFIX + "CHANGE";
	public static final String SERVICE_CLOSE_NAME = SERVICE_PREFIX + "CLOSE";
	public static final String SERVICE_UPDATE_NAME = SERVICE_PREFIX + "UPDATE";

	public static final String EXTRA_TITLE = "title";
	public static final String EXTRA_DOWNLOADED = "downloaded";
	public static final String EXTRA_DURATION = "duration";
	public static final String EXTRA_POSITION = "position";	
	
	//delka prodlevy pro aktualizaci JSON v ms
	public static final int konstToUpdate = 6000000;

	//adresa exportu v JSON
	public static final String DataURL = "http://m.abradio.cz/xml/export.json";
	//adresa pro facebook a kontakty
	public static final String facebook_url = "http://www.facebook.com/pages/ABradiocz/111460366985";
	public static final String napiste_nam_mail = "app@abradio.cz";

}
