package eu.damek.abradio.common;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;

import eu.damek.abradio.service.PlaybackService;

public class DownloadImage extends AsyncTask<String, Integer, Drawable> {

    private ImageView imgToShow = null;
    private PlaybackService player = null;
    private boolean cachable = true;

    public DownloadImage(ImageView img, PlaybackService _player,
                         boolean _cachable) {
        imgToShow = img;
        player = _player;
        cachable = _cachable;
    }

    @Override
    protected Drawable doInBackground(String... params) {
        Object content = null;
        Drawable image = null;
        for (String param : params)
            try {
                URL url = new URL(param);
                content = url.getContent();
                InputStream is = (InputStream) content;
                image = Drawable.createFromStream(is, "src");
                if (cachable) {
                    player.imageCache.put(params[0], image);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        return image;
    }

    @Override
    protected void onPostExecute(Drawable image) {
        if (imgToShow != null) {
            imgToShow.setImageDrawable(image);
        }
    }

}
