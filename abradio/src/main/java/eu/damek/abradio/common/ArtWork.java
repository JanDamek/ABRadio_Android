package eu.damek.abradio.common;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import eu.damek.abradio.service.PlaybackService;

public class ArtWork extends AsyncTask<String, Integer, Integer> {

    private ImageView artImg = null;
    private TextView artist = null;
    private TextView song = null;
    private PlaybackService player = null;
    private String xmlArtist;
    private String xmlSong;
    private HashMap<Integer,String> xmlImage;
    private Drawable vodoznak;

    public ArtWork(PlaybackService _player, ImageView _artImg,
                   TextView _artist, TextView _song, Drawable _vodoznak) {
        artImg = _artImg;
        song = _song;
        artist = _artist;
        player = _player;
        vodoznak = _vodoznak;
        xmlImage = new HashMap<>();
    }

    @Override
    protected Integer doInBackground(String... params) {
        String xml;
        if (params[0].length() != 0 && params[0].contains("http"))
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document dom = builder.parse(params[0]);
                Element root = dom.getDocumentElement();
                root.normalize();

                final Element item1 = (Element) root.getElementsByTagName("Item").item(0);

                final Element artist = (Element) item1.getElementsByTagName("artist").item(0);
                xmlArtist = artist.getFirstChild().getNodeValue();
                final Element song = (Element) item1.getElementsByTagName("song").item(0);
                xmlSong = song.getFirstChild().getNodeValue();

                final NodeList imageItems = item1.getElementsByTagName("imageItems").item(0).getChildNodes();
                for (int i=0;imageItems.getLength()>i;i++){
                    final Node item = imageItems.item(i);
                    final String nodeName = item.getNodeName();
                    if (nodeName.contains("image_")) {
                        final String nodeValue = item.getFirstChild().getNodeValue();
                        String size = nodeName.replace("image_","");
                        xmlImage.put(Integer.valueOf(size),nodeValue);
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
        if (!xmlArtist.equalsIgnoreCase("")) {
            artist.setText(xmlArtist);
        }
        if (!xmlSong.equalsIgnoreCase("")) {
            song.setText(xmlSong);
        }

        if (xmlImage.size() > 0) {
            int height = artImg.getWidth();
            int findHeight = 0;
            Object[] imgKeys = xmlImage.keySet().toArray();
            for (Object imgKey : imgKeys) {
                if ((Integer) imgKey > height) {
                    if (findHeight > (Integer) imgKey || findHeight == 0) {
                        findHeight = (Integer) imgKey;
                    }
                }
            }
            if (findHeight == 0) {
                for (Object imgKey : imgKeys) {
                    if ((Integer) imgKey < height) {
                        if (findHeight < (Integer) imgKey) {
                            findHeight = (Integer) imgKey;
                        }
                    }
                }

            }
            if (findHeight != 0) {
                Drawable image;
                image = player.getImage(xmlImage.get(findHeight), artImg, false);
                artImg.setImageDrawable(image);
            } else {
                artImg.setImageDrawable(vodoznak);
            }
        } else
            artImg.setImageDrawable(vodoznak);
    }

}
