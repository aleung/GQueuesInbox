package leoliang.gqueuesinbox;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class ItemRepository {
    
    public class Item {
        private String key;
        private String description;
        private String note;
        private boolean hasSent;
        private long time;

        public String getDescription() {
            return description;
        }

        public String getKey() {
            return key;
        }

        public String getNote() {
            return note;
        }

        public long getTime() {
            return time;
        }

        public boolean hasSent() {
            return hasSent;
        }
    }

    private static final int ITEM_KEEP_TIME = 24 * 3600 * 1000; // 24 hours
    private static final String DESC = "description";
    private static final String NOTE = "notes";
    private static final String SENT = "hasSent";
    private static final String TIME = "createTime";
    private static final String LOG_TAG = "GQueuesInbox";

    private SharedPreferences sharedPreferences;

    public ItemRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(this.getClass().getName(), Context.MODE_PRIVATE);
    }

    public String add(String description, String notes) {
        String key = UUID.randomUUID().toString();
        JSONObject json = new JSONObject();
        try {
            json.put(DESC, description);
            json.put(NOTE, notes);
            json.put(SENT, false);
            json.put(TIME, System.currentTimeMillis());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Editor editor = sharedPreferences.edit();
        editor.putString(key, json.toString());
        editor.commit();
        clear();
        return key;
    }

    private void clear() {
        for (Item item : getAll()) {
            if (item.hasSent && ((System.currentTimeMillis() - item.time) > ITEM_KEEP_TIME)) {
                delete(item.key);
            }
        }
    }

    private void delete(String key) {
        Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.commit();
        clear();
    }

    public List<Item> getAll() {
        List<Item> items = new LinkedList<Item>();
        Map<String, ?> all = sharedPreferences.getAll();
        for (String key : all.keySet()) {
            try {
                Item item = new Item();
                JSONObject json = new JSONObject((String) all.get(key));
                item.key = key;
                item.description = json.getString(DESC);
                item.note = json.getString(NOTE);
                item.hasSent = json.getBoolean(SENT);
                item.time = json.getLong(TIME);
                items.add(item);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return items;
    }

    public void setHasSent(String key) {
        String value = sharedPreferences.getString(key, null);
        if (value == null) {
            Log.i(LOG_TAG, "Item with key " + key + " does not exist in shared preferences");
            return;
        }
        try {
            JSONObject json = new JSONObject(value);
            json.put(SENT, true);
            Editor editor = sharedPreferences.edit();
            editor.putString(key, json.toString());
            editor.commit();
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error in updating item. Key: " + key + " , JSON: " + value);
            throw new RuntimeException(e);
        }
        clear();
    }
}
