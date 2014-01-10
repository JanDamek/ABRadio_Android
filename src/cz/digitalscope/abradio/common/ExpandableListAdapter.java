package cz.digitalscope.abradio.common;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.digitalscope.abradio.R;
import cz.digitalscope.abradio.service.PlaybackService;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    public Context context;
    private LayoutInflater mInflater;
    public PlaybackService player = null;

    public ExpandableListAdapter(Context context) {
        this.context = context;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public JSONObject getChild(int groupPosition, int childPosition) {
        if (player.categories() != null) {
            JSONObject cat;
            JSONArray rad;
            try {
                cat = player.categories().getJSONObject(groupPosition);
                try {
                    rad = cat.getJSONArray("radios");
                    try {
                        return rad.getJSONObject(childPosition);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else
            return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        if (player.categories() != null) {
            JSONObject cat;
            JSONArray rad;
            try {
                cat = player.categories().getJSONObject(groupPosition);
                try {
                    rad = cat.getJSONArray("radios");
                    try {
                        return rad.getJSONObject(childPosition).getLong("id");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return 0;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return 0;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return 0;
            }
        } else
            return 0;
    }

    @Override
    public int getChildrenCount(int groupPosition) {

        if (player.categories() != null) {
            JSONObject cat;
            JSONArray rad;
            try {
                cat = player.categories().getJSONObject(groupPosition);
                try {
                    rad = cat.getJSONArray("radios");
                    return rad.length();
                } catch (JSONException e) {
                    e.printStackTrace();
                    return 0;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return 0;
            }
        } else
            return 0;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        JSONObject bean = getChild(groupPosition, childPosition);

        if (convertView == null) {
            convertView = mInflater.inflate(
                    R.layout.expandable_list_item_with_image, null);
        }
        TextView txtTitle = (TextView) convertView.findViewById(R.id.name);
        TextView txtDesc = (TextView) convertView
                .findViewById(R.id.description);

        try {
            txtTitle.setText(bean.getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            txtDesc.setText(bean.getString("description"));
        } catch (JSONException e) {
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
                ImageView img = (ImageView) convertView
                        .findViewById(R.id.image);
                image = player.getImage(logo, img, true);
                img.setImageDrawable(image);
            }
        }

        return convertView;
    }

    @Override
    public Object getGroup(int groupPosition) {
        if (player.categories() != null) {
            JSONObject cat;
            try {
                cat = player.categories().getJSONObject(groupPosition);
                return cat.getString("title");
            } catch (JSONException e) {
                e.printStackTrace();
                return "";
            }
        } else
            return "";
    }

    @Override
    public int getGroupCount() {
        if (player.categories() != null) {
            return player.categories().length();
        }
        return 0;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater
                    .inflate(R.layout.expandable_group_row, null);
        }

        TextView tvGroupName = (TextView) convertView
                .findViewById(R.id.groupNameText);
        tvGroupName.setText(getGroup(groupPosition).toString());

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

}
