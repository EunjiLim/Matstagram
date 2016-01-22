package br.com.dina.oauth.instagram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import br.com.dina.oauth.instagram.InstagramDialog.OAuthDialogListener;

/**
 * 
 * @author Thiago Locatelli <thiago.locatelli@gmail.com>
 * @author Lorensius W. L T <lorenz@londatiga.net>
 * 
 */
public class InstagramApp {
	
	private InstagramSession mSession;
	private InstagramDialog mDialog;
	private OAuthAuthenticationListener mListener;
	private ProgressDialog mProgress;
	private String mAuthUrl;
	private String mTokenUrl;
	private String mAccessToken;
	private Context mCtx;
	
	private String mClientId;
	private String mClientSecret;
	

	private static int WHAT_FINALIZE = 0;
	private static int WHAT_ERROR = 1;
	private static int WHAT_FETCH_INFO = 2;

	/**
	 * Callback url, as set in 'Manage OAuth Costumers' page
	 * (https://developer.github.com/)
	 */

	public static String mCallbackUrl = "";
	private static final String AUTH_URL = "https://api.instagram.com/oauth/authorize/";
	private static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
	private static final String API_URL = "https://api.instagram.com/v1";

	private static final String TAG = "InstagramAPI";

	private TreeMap<String, Integer> treeMap = new TreeMap<>();

	public InstagramApp(Context context, String clientId, String clientSecret,
			String callbackUrl) {
		
		mClientId = clientId;
		mClientSecret = clientSecret;
		mCtx = context;
		mSession = new InstagramSession(context);
		mAccessToken = mSession.getAccessToken();
		mCallbackUrl = callbackUrl;
		mTokenUrl = TOKEN_URL + "?client_id=" + clientId + "&client_secret="
				+ clientSecret + "&redirect_uri=" + mCallbackUrl + "&grant_type=authorization_code";
		mAuthUrl = AUTH_URL + "?client_id=" + clientId + "&redirect_uri="
				+ mCallbackUrl + "&response_type=code&display=touch&scope=likes+comments+relationships";
		
		OAuthDialogListener listener = new OAuthDialogListener() {
			@Override
			public void onComplete(String code) {
				getAccessToken(code);
			}

			@Override
			public void onError(String error) {
				mListener.onFail("Authorization failed");
			}
		};

		mDialog = new InstagramDialog(context, mAuthUrl, listener);
		mProgress = new ProgressDialog(context);
		mProgress.setCancelable(false);
	}

	private void getAccessToken(final String code) {
		mProgress.setMessage("Getting access token ...");
		mProgress.show();

		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Getting access token");
				int what = WHAT_FETCH_INFO;
				try {
					URL url = new URL(TOKEN_URL);
					//URL url = new URL(mTokenUrl + "&code=" + code);
					Log.i(TAG, "Opening Token URL " + url.toString());
					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					urlConnection.setRequestMethod("POST");
					urlConnection.setDoInput(true);
					urlConnection.setDoOutput(true);
					//urlConnection.connect();
					OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
					writer.write("client_id="+mClientId+
								"&client_secret="+mClientSecret+
								"&grant_type=authorization_code" +
								"&redirect_uri="+mCallbackUrl+
								"&code=" + code);
				    writer.flush();
					String response = streamToString(urlConnection.getInputStream());
					Log.i(TAG, "response " + response);
					JSONObject jsonObj = (JSONObject) new JSONTokener(response).nextValue();
					
					mAccessToken = jsonObj.getString("access_token");
					Log.i(TAG, "Got access token: " + mAccessToken);
					
					String id = jsonObj.getJSONObject("user").getString("id");
					String user = jsonObj.getJSONObject("user").getString("username");
					String name = jsonObj.getJSONObject("user").getString("full_name");					
					
					mSession.storeAccessToken(mAccessToken, id, user, name);
					
				} catch (Exception ex) {
					what = WHAT_ERROR;
					ex.printStackTrace();
				}

				mHandler.sendMessage(mHandler.obtainMessage(what, 1, 0));
			}
		}.start();
	}
	
	private void fetchUserName() {
		mProgress.setMessage("Fetching user name ...");
		
		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Fetching user info");
				int what = WHAT_FINALIZE;
				try {
					URL url = new URL(API_URL + "/users/"+ mSession.getId() + "/?access_token=" + mAccessToken);

					Log.i(TAG, "Opening URL " + url.toString());
					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					urlConnection.setRequestMethod("GET");
					int responseCode = urlConnection.getResponseCode();
					Log.i(TAG, String.valueOf(responseCode));
					Log.i(TAG, urlConnection.getInputStream().toString());
					String response = streamToString(urlConnection.getInputStream());
					Log.i(TAG,response);
					JSONObject jsonObj = (JSONObject) new JSONTokener(response).nextValue();
					String name = jsonObj.getJSONObject("data").getString("full_name");
					String bio = jsonObj.getJSONObject("data").getString("bio");
					Log.i(TAG, "Got name: " + name + ", bio [" + bio + "]");
				} catch (Exception ex) {
					what = WHAT_ERROR;
					ex.printStackTrace();

				}
				mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
			}
		}.start();	
		
	}

	public void getUserRecent(){
		treeMap = new TreeMap<>(); //Initialization
		Thread thread = new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "/users/self/media/recent");
				int what = WHAT_FINALIZE;
				try {
					URL url = new URL(API_URL + "/users/self/media/recent/?access_token=" + mAccessToken);

					Log.i(TAG, "Opening URL " + url.toString());
					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					urlConnection.setRequestMethod("GET");
					int responseCode = urlConnection.getResponseCode();
					Log.i(TAG, String.valueOf(responseCode));
					Log.i(TAG, urlConnection.getInputStream().toString());
					String response = streamToString(urlConnection.getInputStream());
					JSONObject jsonObj = (JSONObject) new JSONTokener(response).nextValue();
					JSONArray jsonArr = (JSONArray) jsonObj.get("data");
					for(int i=0 ; i < jsonArr.length() ; i++){
						JSONObject jsonObj2 = jsonArr.getJSONObject(i);
						JSONArray jsonArr2 = jsonObj2.getJSONArray("tags");
						for(int j=0 ; j < jsonArr2.length() ; j++){
							String hashTag = jsonArr2.get(j).toString();
							Log.i(TAG, hashTag);
							if(treeMap.containsKey(hashTag)) {
								treeMap.put(hashTag, treeMap.get(hashTag) + 1);
								Log.i(TAG, Integer.toString(treeMap.get(hashTag)));
							}
							else {
								treeMap.put(hashTag, 1);
								Log.i(TAG,Integer.toString(treeMap.get(hashTag)));
							}
						}
					}
				} catch (Exception ex) {
					what = WHAT_ERROR;
					ex.printStackTrace();
				}
				mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
			}
		};
		thread.start();
		try {
			thread.join(); //for synchronization
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Iterator itr = sortByValue(treeMap).iterator();
		while(itr.hasNext()){
			String temp = (String) itr.next();
			Log.i(TAG, temp + " = " + treeMap.get(temp));
		}
	}


	private void getSearchResult(final String hashtag){
		mProgress.setMessage("Searching ...");

		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Searching by " + hashtag );
				int what = WHAT_FINALIZE;
				try {
					URL url = new URL(API_URL + "/tags/"+ hashtag + "media/recent?count=50&access_token=" + mAccessToken);

					Log.i(TAG, "Opening URL " + url.toString());
					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					urlConnection.setRequestMethod("GET");
					int responseCode = urlConnection.getResponseCode();
					Log.i(TAG, String.valueOf(responseCode));
					Log.i(TAG, urlConnection.getInputStream().toString());
					String response = streamToString(urlConnection.getInputStream());
					JSONObject jsonObj = (JSONObject) new JSONTokener(response).nextValue();
					JSONArray jsonArr = (JSONArray) jsonObj.get("data");
					//String name = jsonObj.getJSONObject("data").getString("full_name");
					//String bio = jsonObj.getJSONObject("data").getString("bio");
					//Log.i(TAG, "Got name: " + name + ", bio [" + bio + "]");
				} catch (Exception ex) {
					what = WHAT_ERROR;
					ex.printStackTrace();

				}
				mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
			}
		}.start();
	}


	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, msg.toString());
			if (msg.what == WHAT_ERROR) {
				mProgress.dismiss();
				if(msg.arg1 == 1) {
					mListener.onFail("Failed to get access token");
				}
				else if(msg.arg1 == 2) {
					mListener.onFail("Failed to get user information");
				}
			} 
			else if(msg.what == WHAT_FETCH_INFO) {
				fetchUserName();
			}
			else {
				mProgress.dismiss();
				mListener.onSuccess();
			}
		}
	};

	public boolean hasAccessToken() {
		return (mAccessToken != null);
	}

	public void setListener(OAuthAuthenticationListener listener) {
		mListener = listener;
	}

	public String getUserName() {
		return mSession.getUsername();
	}

	public String getId() {
		return mSession.getId();
	}
	
	public String getName() {
		return mSession.getName();
	}

	public TreeMap<String, Integer> getTreeMap() {
		return treeMap;
	}
	public void authorize() {
		//Intent webAuthIntent = new Intent(Intent.ACTION_VIEW);
        //webAuthIntent.setData(Uri.parse(AUTH_URL));
        //mCtx.startActivity(webAuthIntent);
		mDialog.show();
	}

	private String streamToString(InputStream is) throws IOException {
		String str = "";

		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));

				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}

				reader.close();
			} finally {
				is.close();
			}

			str = sb.toString();
		}

		return str;
	}

	public void resetAccessToken() {
		if (mAccessToken != null) {
			mSession.resetAccessToken();
			mAccessToken = null;
		}
	}

	public interface OAuthAuthenticationListener {
		public abstract void onSuccess();

		public abstract void onFail(String error);
	}

	List sortByValue(final Map map){
		List<String> list = new ArrayList();
		list.addAll(map.keySet());

		Collections.sort(list, new Comparator() {

			public int compare(Object o1, Object o2) {
				Object v1 = map.get(o1);
				Object v2 = map.get(o2);

				return ((Comparable) v1).compareTo(v2);
			}

		});
		Collections.reverse(list); // 주석시 오름차순
		return list;
	}
}