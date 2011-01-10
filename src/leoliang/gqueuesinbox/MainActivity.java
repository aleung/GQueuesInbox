package leoliang.gqueuesinbox;

import leoliang.gqueuesinbox.ItemRepository.Item;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

    public class ItemListAdapter extends BaseAdapter {

        private final LayoutInflater inflater;

        public ItemListAdapter(Context context) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        private String calculateAge(long time) {
            long ageSeconds = (System.currentTimeMillis() - time) / 1000;
            if (ageSeconds < 60) {
                return "just now";
            } else if (ageSeconds < 3600) {
                int ageMinutes = (int) (ageSeconds / 60);
                return ageMinutes + " mins";
            } else {
                int ageHours = (int) (ageSeconds / 3600);
                return ageHours + " hours";
            }
        }

        @Override
        public int getCount() {
            return repository.getAll().size();
        }

        @Override
        public Object getItem(int position) {
            return repository.getAll().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = inflater.inflate(R.layout.item_view, null);
            TextView descriptionView = (TextView) view.findViewById(R.id.description);
            TextView ageView = (TextView) view.findViewById(R.id.age);
            Item item = (Item) getItem(position);
            descriptionView.setText(item.getDescription());
            String age = calculateAge(item.getTime());
            ageView.setText(age);
            if (!item.hasSent()) {
                descriptionView.setTypeface(Typeface.DEFAULT_BOLD);
            }
            return view;
        }

    }

    private final static String LOG_TAG = "GQueuesInbox";

    private ItemRepository repository;

    private EditText descriptionEdit;
    private EditText noteEdit;
    private ListView listView;

    private void addItem() {
        String description = descriptionEdit.getText().toString();
        String note = noteEdit.getText().toString();
        if ((description.length() == 0) && (note.length() == 0)) {
            return;
        }
        repository.add(description.length() > 0 ? description : "New item", note);
        clearUserInputArea();
        ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
        startService(new Intent(this, SendService.class));
    }

    private void clearUserInputArea() {
        descriptionEdit.setText("");
        descriptionEdit.requestFocus();
        noteEdit.setText("");
    }

    private void gotoGQueuesWeb() {
        Uri uri = Uri.parse("http://www.gqueues.com");
        startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repository = new ItemRepository(getApplicationContext());

        setContentView(R.layout.main);

        descriptionEdit = (EditText) findViewById(R.id.DescriptionEdit);
        noteEdit = (EditText) findViewById(R.id.NoteEdit);

        Button sendButton = (Button) findViewById(R.id.SendButton);
        sendButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });

        listView = (ListView) findViewById(R.id.HistoryList);
        ListAdapter listAdapter = new ItemListAdapter(this);
        listView.setAdapter(listAdapter);
        
        Button webButton = (Button) findViewById(R.id.webButton);
        webButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoGQueuesWeb();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_prefs) {
            Intent intent = new Intent().setClass(this, PreferenceActivity.class);
            startActivity(intent);
        }
        return true;
    }
}