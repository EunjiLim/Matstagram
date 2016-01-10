package cs.kookmin.matstagram;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

/**
 * Created by Eunji on 2016-01-11.
 */
public class SearchActivity extends Activity{

    private ListView mListView;
    private CustomAdapter mAdapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mAdapter = new CustomAdapter();
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);

        mAdapter.add("hello");
    }
}
