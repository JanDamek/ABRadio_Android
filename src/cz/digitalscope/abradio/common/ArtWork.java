package cz.digitalscope.abradio.common;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.digitalscope.abradio.service.PlaybackService;

public class ArtWork extends AsyncTask<String, Integer, Integer> {

    private ImageView artImg = null;
    private TextView artist = null;
    private TextView song = null;
    private PlaybackService player = null;
    private String xmlArtist;
    private String xmlSong;
    private HashMap<Integer, String> xmlImage;
    private Drawable vodoznak;

    public ArtWork(PlaybackService _player, ImageView _artImg,
                   TextView _artist, TextView _song, Drawable _vodoznak) {
        artImg = _artImg;
        song = _song;
        artist = _artist;
        player = _player;
        vodoznak = _vodoznak;
        xmlImage = new HashMap<Integer, String>();
    }

    @Override
    protected Integer doInBackground(String... params) {
        String xml;
        if (params[0].length() != 0)
            try {
                xml = new Communicator().executeHttpGet(params[0]);

                Pattern p = Pattern.compile("<artist>\\s*(.*)</artist>");
                Matcher m = p.matcher(xml);
                if (m.find()) {
                    xmlArtist = (m.group(1));
                    xmlArtist = xmlArtist.replace("<![CDATA[", "");
                    xmlArtist = xmlArtist.replace("]]>", "");
                }
                p = Pattern.compile("<song>\\s*(.*)</song>");
                m = p.matcher(xml);
                if (m.find()) {
                    xmlSong = (m.group(1));
                    xmlSong = xmlSong.replace("<![CDATA[", "");
                    xmlSong = xmlSong.replace("]]>", "");
                }
                p = Pattern.compile("<imageItems>\\s*(.*)</imageItems>");
                m = p.matcher(xml);
                if (m.find()) {
                    String img_raw = m.group(1);
                    String[] imgParts = img_raw.split("\t");
                    p = Pattern.compile("<image_\\s*(.*)</image_");
                    for (int i = 0; i < imgParts.length; i++) {
                        m = p.matcher(imgParts[i]);
                        if (m.find()) {
                            String imgSize = m.group(1);
                            String size = imgSize.substring(0,
                                    imgSize.indexOf(">"));
                            imgSize = imgSize
                                    .substring(imgSize.indexOf(">") + 1);
                            xmlImage.put(Integer.parseInt(size), imgSize);
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                xmlArtist = "";
                xmlSong = "";
            }
        else {
            xmlArtist = "";
            xmlSong = "";
        }
        return null;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (xmlArtist != "") {
            artist.setText(xmlArtist);
        }
        if (xmlSong != "") {
            song.setText(xmlSong);
        }

        if (xmlImage.size() > 0) {
            int height = artImg.getWidth();
            int findHeight = 0;
            Object[] imgKeys = xmlImage.keySet().toArray();
            for (int i = 0; i < imgKeys.length; i++) {
                if ((Integer) imgKeys[i] > height) {
                    if (findHeight > (Integer) imgKeys[i] || findHeight == 0) {
                        findHeight = (Integer) imgKeys[i];
                    }
                }
            }
            if (findHeight == 0) {
                for (int i = 0; i < imgKeys.length; i++) {
                    if ((Integer) imgKeys[i] < height) {
                        if (findHeight < (Integer) imgKeys[i]) {
                            findHeight = (Integer) imgKeys[i];
                        }
                    }
                }

            }
            if (findHeight != 0) {
                Drawable image = null;
                image = player.getImage(xmlImage.get(findHeight), artImg, false);
                artImg.setImageDrawable(image);
            } else {
                artImg.setImageDrawable(vodoznak);
            }
        } else
            artImg.setImageDrawable(vodoznak);
    }

}
