package cz.digitalscope.abradio.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import cz.digitalscope.abradio.R;
import cz.digitalscope.abradio.activity.PlayerActivity;
import cz.digitalscope.abradio.service.PlaybackService;

public class SimpleListAdabter implements ListAdapter, OnClickListener, OnLongClickListener {

    public PlaybackService player = null;
    public LayoutInflater mInflater = null;
    public int type = 0;
    private Context context = null;
    private ListView lv = null;

    public SimpleListAdabter(PlaybackService player2, Context _context, int atype, ListView favList) {
        player = player2;
        type = atype;
        context = _context;
        lv = favList;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public HashMap<String, JSONObject> data() {
        if (type == 0) {
            return player.oblibene;
        } else
            return player.listeners;
    }


    @Override
    public JSONObject getItem(int arg0) {
        String id = (String) data().keySet().toArray()[arg0];
        return data().get(id);
    }

    @Override
    public long getItemId(int arg0) {
        try {
            return getItem(arg0).getLong("id");
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2) {
        JSONObject bean = getItem(arg0);

        View row = arg1;

        if (row == null) {
            row = mInflater.inflate(R.layout.expandable_list_item_with_image,
                    null);
        }
        TextView txtTitle = (TextView) row.findViewById(R.id.name);
        TextView txtDesc = (TextView) row.findViewById(R.id.description);

        try {
            String name = bean.getString("name");
            txtTitle.setText(name);
        } catch (JSONException e) {
            txtTitle.setText("");
            e.printStackTrace();
        }
        try {
            String txt = bean.getString("description");
            txtDesc.setText(txt);
        } catch (JSONException e) {
            txtDesc.setText("");
            e.printStackTrace();
        }
        String logo = "";
        try {
            logo = bean.getString("logo");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (logo != "") {
            if (player != null) {
                Drawable image = null;
                ImageView img = (ImageView) row.findViewById(R.id.image);
                image = player.getImage(logo, img, true);
                img.setImageDrawable(image);
            }
        }
        try {
            TextView id = (TextView) row.findViewById(R.id.viewID);
            id.setText(bean.getString("id"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        row.setOnLongClickListener(this);
        row.setOnClickListener(this);

        return row;
    }

    @Override
    public boolean onLongClick(View v) {
        final String id = (String) ((TextView) v.findViewById(R.id.viewID))
                .getText();

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if (type == 0) {
                            player.oblibene.remove(id);
                        } else {
                            player.listeners.remove(id);
                        }

                        lv.invalidateViews();
                        player.savedata();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // No button clicked
                        break;
                }

            }
        };

        Context c = v.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(c);

        try {
            builder.setMessage(
                    "Opravdu odstranit radio "
                            + data().get(id).getString("name") + "?")
                    .setPositiveButton("Ano", dialogClickListener)
                    .setNegativeButton("Ne", dialogClickListener).show();
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        String id = (String) ((TextView) v.findViewById(R.id.viewID))
                .getText();
        player.aktRadio = data().get(id);
        player.play();
        lv.invalidateViews();

        Intent intent = new Intent(context.getApplicationContext(), PlayerActivity.class);
        context.startActivity(intent);

    }

    @Override
    public int getItemViewType(int arg0) {
        return type;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        int pocet = data().size();
        return pocet == 0;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        int pocet = data().size();
        return pocet;
    }
}
