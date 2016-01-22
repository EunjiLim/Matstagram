package cs.kookmin.matstagram;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.TreeMap;

import br.com.dina.oauth.instagram.InstagramApp;

import static cs.kookmin.matstagram.instagram.InstagramData.CALLBACK_URL;
import static cs.kookmin.matstagram.instagram.InstagramData.CLIENT_ID;
import static cs.kookmin.matstagram.instagram.InstagramData.CLIENT_SECRET;


/**
 * Created by Eunji on 2016-01-11.
 */
public class SearchActivity extends Activity{

    private ListView mListView;
    private CustomAdapter mAdapter;
    private Button btnSearch;
    private EditText etSearch;

    private static final String TAG = "InstagramAPI";

    private InstagramApp mApp;
    TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mApp = new InstagramApp(this, CLIENT_ID,
                CLIENT_SECRET, CALLBACK_URL);
        mApp.setListener(listener);

        mAdapter = new CustomAdapter();
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);

        mAdapter.add("hello");

        etSearch = (EditText) findViewById(R.id.etSearch2);
        btnSearch = (Button) findViewById(R.id.btnSearch2);
        btnSearch.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             mApp.getUserRecent();
                                         }
                                     }
        );
    }

    InstagramApp.OAuthAuthenticationListener listener = new InstagramApp.OAuthAuthenticationListener() {

        @Override
        public void onSuccess() {
        }

        @Override
        public void onFail(String error) {
            Log.i("InstagramAPI", "onFail");
            Toast.makeText(SearchActivity.this, error, Toast.LENGTH_SHORT).show();
        }
    };
}
