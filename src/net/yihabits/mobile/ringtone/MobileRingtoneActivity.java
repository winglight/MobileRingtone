package net.yihabits.mobile.ringtone;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.yihabits.mobile.ringtone.R;
import net.yihabits.mobile.ringtone.db.RingtoneDAO;
import net.yihabits.mobile.ringtone.db.RingtoneDBOpenHelper;
import net.yihabits.mobile.ringtone.db.RingtoneModel;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.api.translate.Language;
import com.google.api.translate.Translate;

import android.app.Activity;
import android.app.ListActivity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.Gallery.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MobileRingtoneActivity extends ListActivity {
	private static final int PICK_CONTACT = 1;
	private String uid;
	private String LOGTAG = "MobileRingtoneActivity";
	private String selected_ringtone = "";
	private String locale = "";
	private int addmore_count = 0;
	private int selected_item = -1;
	private int current_row = 0; // for scroll to the row

	private RingtoneDAO dba;
	private int source = 0; // 0 - online ; 1 - local
	private int category = 0; // 0 - all
	private int language = 0; // 0 - all
	private int orderby = 0; // 0 - order by date ; 1 - order by popular
	private int search = 0; // 0 - no search ; 1 - search
	private String keyword = "";
	private int page = 0;
	
	private ArrayList<RingtoneModel> ringtoneList;

	private DownloadUtil util;

	private LinearLayout downloadPanel;
	
	private LinearLayout searchPanel;

	private LinearLayout btnPanel;
	
	private Spinner filterSpinner;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		
		// initialize DownloadUtil
		if (util == null) {
			util = new DownloadUtil(this);
			util.initBaseDir();
		}
		
		if(ringtoneList == null){
			ringtoneList = new ArrayList<RingtoneModel>();
		}

		locale = this.getResources().getConfiguration().locale.getLanguage();

		initBtn();

		dba = RingtoneDAO.getInstance(this);

		// ad initialization
		// Create the adView
		AdView adView = new AdView(this, AdSize.BANNER, "a14de7374f21cb5");
		// Lookup your LinearLayout assuming it��s been given
		// the attribute android:id="@+id/mainLayout"
		LinearLayout layout = (LinearLayout) findViewById(R.id.ad_layout);
		// Add the adView to it
		layout.addView(adView);
		// Initiate a generic request to load it with an ad
		adView.loadAd(new AdRequest());

		// listen phone state
		MyPhoneStateListener phoneListener = new MyPhoneStateListener();
		phoneListener.setContext(this);

		TelephonyManager telephony = (TelephonyManager)

		this.getSystemService(Context.TELEPHONY_SERVICE);

		telephony.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);

		// initialize panels
		downloadPanel = (LinearLayout) findViewById(R.id.downloadPanel);
		downloadPanel.setVisibility(View.GONE);
		
		searchPanel = (LinearLayout) findViewById(R.id.search_layout);
		searchPanel.setVisibility(View.GONE);

		btnPanel = (LinearLayout) findViewById(R.id.btn_layout);
		
	}

	public class MyPhoneStateListener extends PhoneStateListener {

		private Context context;

		public Context getContext() {
			return context;
		}

		public void setContext(Context context) {
			this.context = context;
		}

		public void onCallStateChanged(int state, String incomingNumber) {

			switch (state) {

			case TelephonyManager.CALL_STATE_IDLE: {

				Log.d("DEBUG", "IDLE");

				if (DownloadUtil.player != null
						&& DownloadUtil.player.isInterrupted()) {
					DownloadUtil.player.playatpause();
				}
				break;
			}

			case TelephonyManager.CALL_STATE_OFFHOOK:

				Log.d("DEBUG", "OFFHOOK");

			case TelephonyManager.CALL_STATE_RINGING:

				Log.d("DEBUG", "RINGING");

				// pause player
				if (DownloadUtil.player != null) {
					DownloadUtil.player.pause();
				}

				break;

			}

		}

	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
		case (PICK_CONTACT):
			if (resultCode == Activity.RESULT_OK) {
				Uri contactData = data.getData();
				Cursor c = managedQuery(contactData, null, null, null, null);
				if (c.moveToFirst()) {
					String name = c
							.getString(c
									.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
					// set ringtone for the contact
					ContentValues values = new ContentValues();
					values.put(ContactsContract.Contacts.CUSTOM_RINGTONE,
							this.selected_ringtone);
					this.getContentResolver().update(contactData, values, null,
							null);

					toastMsg(this.getString(R.string.setRingtoneSuccess)
							+ " - " + name);
				}
			}
			break;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// We do nothing here. We're only handling this to keep orientation
		// or keyboard hiding from causing the WebView activity to restart.
	}

	@Override
	protected void onStart() {
		super.onStart();

		// initialize 2 spinners
		initSpinner();

		// register context menu of listview
		registerForContextMenu(getListView());
		
		getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		dba.close();
	}

	private void initSpinner() {
		//initialize counter of addmore
		addmore_count = 0;
		
		//categories
		final String[] categories = new String[] {
				getString(R.string.category_all),
				getString(R.string.category_funny),
				getString(R.string.category_others),
				getString(R.string.category_pop),
				getString(R.string.category_comic),
				getString(R.string.category_dj),
				getString(R.string.category_movie),
				getString(R.string.category_message) };
		
		// 1.category spinner
		Spinner categorySpinner = (Spinner) findViewById(R.id.categorySpinner);
		ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this,
				R.layout.spinner_layout, categories);
		mAdapter.setDropDownViewResource(R.layout.spinner_layout);
		categorySpinner.setAdapter(mAdapter);
		categorySpinner
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> adapterView,
							View view, int i, long l) {
						source = 0;
						
						// change category
						category = i;
						
						//set search
						search = 0;
						
						//change the title
						String suffix = categories[i] ;
						if(language > 0){
							suffix += " - " + (MobileRingtoneActivity.this.getResources().getStringArray(R.array.filter))[language];
						}
						suffix += " - " +  (MobileRingtoneActivity.this.getResources().getStringArray(R.array.orderby))[orderby];
						setTitle(getString(R.string.app_name_suffix, suffix));

						// set the page 0
						page = 0;
						
						//clear list
						ringtoneList.clear();
						
						//set selected_item = -1
						selected_item = -1;

						// get more ringtones by categor + order
						addMore();
						addmore_count++;

						// go to the selected category
						RingtoneAdapter adapter = new RingtoneAdapter(
								MobileRingtoneActivity.this);

						setListAdapter(adapter);
						
						displaySearchBox(false);
					}

					public void onNothingSelected(AdapterView<?> adapterView) {
						return;
					}
				});

		// 2.order by spinner
		Spinner orderSpinner = (Spinner) findViewById(R.id.orderSpinner);
		ArrayAdapter<CharSequence> mAdapter2 = ArrayAdapter.createFromResource(
                this, R.array.orderby, R.layout.spinner_layout);
		orderSpinner.setAdapter(mAdapter2);
		orderSpinner
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> adapterView,
							View view, int i, long l) {
						//if only show local musics
						if(i == 2){
							source = 1;
						}else{
							source = 0;
						}
						
						if(addmore_count > 0){
							//for sure, it's not passed at the beginning of app startup 
						
							if((i > 0 || orderby > 0) && orderby != i){
						// change order
						orderby = i;
						
						//change the title
						String suffix = categories[category] ;
						if(language > 0){
							suffix += " - " + (MobileRingtoneActivity.this.getResources().getStringArray(R.array.filter))[language];
						}
						suffix += " - " +  (MobileRingtoneActivity.this.getResources().getStringArray(R.array.orderby))[orderby];
						setTitle(getString(R.string.app_name_suffix, suffix));
						
						//set search
						search = 0;

						// set the page 0
						page = 0;
						
						//clear list
						ringtoneList.clear();
						
						//set selected_item = -1
						selected_item = -1;

						// get more ringtones by categor + order
						addMore();

						// go to the selected category
						RingtoneAdapter adapter = new RingtoneAdapter(
								MobileRingtoneActivity.this);

						setListAdapter(adapter);
							}
						}
						
						displaySearchBox(false);
					}

					public void onNothingSelected(AdapterView<?> adapterView) {
						return;
					}
				});
		
		// 3.language filter spinner
		
		filterSpinner = (Spinner) findViewById(R.id.filterSpinner);
		ArrayAdapter<CharSequence> mAdapter3 = ArrayAdapter.createFromResource(
                this, R.array.filter, R.layout.spinner_layout);
		filterSpinner.setAdapter(mAdapter3);
		filterSpinner
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> adapterView,
							View view, int i, long l) {
						source = 0;
						
						if(addmore_count > 0){
							//for sure, it's not passed at the beginning of app startup 
							
							if((i > 0 || language > 0) && language != i){
						// change language
						language = i;
						
						//change the title
						String suffix = categories[category] ;
						if(language > 0){
							suffix += " - " + (MobileRingtoneActivity.this.getResources().getStringArray(R.array.filter))[language];
						}
						suffix += " - " +  (MobileRingtoneActivity.this.getResources().getStringArray(R.array.orderby))[orderby];
						setTitle(getString(R.string.app_name_suffix, suffix));
						
						//set search
						search = 0;

						// set the page 0
						page = 0;
						
						//clear list
						ringtoneList.clear();
						
						//set selected_item = -1
						selected_item = -1;

						// get more ringtones by categor + order
						addMore();

						// go to the selected category
						RingtoneAdapter adapter = new RingtoneAdapter(
								MobileRingtoneActivity.this);

						setListAdapter(adapter);
						
						filterSpinner.setVisibility(View.GONE);
							}
						}
						
						displaySearchBox(false);
						
					}

					public void onNothingSelected(AdapterView<?> adapterView) {
						return;
					}
				});
	}

	@Override
	protected void onListItemClick(final ListView list, View v, final int position, long id) {
		super.onListItemClick(list, v, position, id);

		// for the last row
		if (position == ringtoneList.size() ) {
			addMore();

		} else {

			// set selected
			list.post(new Runnable() {
		        @Override
		        public void run() {
					selected_item = position;
					((BaseAdapter)getListAdapter()).notifyDataSetChanged();
//		            View v = list.getChildAt(position);
//		            if (v != null) {
//		                ViewHolder holder = (ViewHolder) v.getTag();
//		                holder.icon.setVisibility(View.VISIBLE);
//		            }
		        }
		    });
			
//			 vh.icon.setVisibility(View.VISIBLE);

			// play the music
			util.runSaveUrl(position, R.id.menu_play_ringtone, ringtoneList
					.get(position).getUrl());
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		// super.onCreateContextMenu(menu, v, menuInfo);
		
		if(((AdapterContextMenuInfo)menuInfo).position != ringtoneList.size()){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
		}

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		util.runSaveUrl(info.position, item.getItemId(),
				ringtoneList.get(info.position).getUrl());

		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_filter:{
			// popup the spin dialog
			filterSpinner.setVisibility(View.INVISIBLE);
			filterSpinner.performClick();
			return true;
		}
		case R.id.menu_search:{
			// show the search box
			displaySearchBox(true);
			return true;
		}
		case R.id.menu_help:{
			// popup the about window
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(MobileRingtoneActivity.this, AboutActivity.class);
			startActivity(intent);
			return true;
		}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	public void toastMsg(int resId) {
		final String msg = this.getString(resId);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG)
						.show();
			}
		});
	}

	public void toastMsg(final String msg) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG)
						.show();
			}
		});
	}

	public String getUid() {
		if (this.uid == null) {
			this.uid = Settings.Secure.getString(getContentResolver(),
					Settings.Secure.ANDROID_ID);
		}
		return this.uid;
	}

	public void enableBtn() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Button playBtn = (Button) findViewById(R.id.playBtn);
				playBtn.setEnabled(true);
				playBtn.setBackgroundResource(R.drawable.media_playback_pause2);
				Button stopBtn = (Button) findViewById(R.id.stopBtn);
				stopBtn.setEnabled(true);
				TextView displayLbl = (TextView) findViewById(R.id.displayLbl);
				displayLbl.setText(R.string.playing);
				displayToolbar(true);
			}
		});

	}

	private void initBtn() {
		final Button playBtn = (Button) findViewById(R.id.playBtn);
		playBtn.setEnabled(false);

		final Button stopBtn = (Button) findViewById(R.id.stopBtn);
		stopBtn.setEnabled(false);
		
		final Button searchBtn = (Button) findViewById(R.id.searchBtn);
//		searchBtn.setEnabled(false);
		
		final EditText searchTxt = (EditText) findViewById(R.id.searchTxt);
//		searchTxt.setText(R.string.noMusic);

		final TextView displayLbl = (TextView) findViewById(R.id.displayLbl);
		displayLbl.setText(R.string.noMusic);

		final LinearLayout btnLayout = (LinearLayout) findViewById(R.id.btn_layout);
		btnLayout.setVisibility(View.GONE);

		playBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// play or pause
				if (DownloadUtil.player != null
						&& DownloadUtil.player.isPlayed()
						&& !DownloadUtil.player.isPaused()) {
					// player is playing, so pause it and change the icon
					DownloadUtil.player.pause();
					v.setBackgroundResource(R.drawable.media_playback_start2);
					displayLbl.setText(R.string.paused);
				} else if (DownloadUtil.player != null
						&& DownloadUtil.player.isPlayed()
						&& DownloadUtil.player.isPaused()) {
					// player is paused, so resume the music and change the icon
					DownloadUtil.player.playatpause();
					v.setBackgroundResource(R.drawable.media_playback_pause2);
					displayLbl.setText(R.string.playing);
				} else {
					v.setEnabled(false);
					stopBtn.setEnabled(false);
					displayLbl.setText(R.string.noMusic);
				}
			}
		});

		stopBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				displayLbl.setText(R.string.noMusic);
				if (DownloadUtil.player != null) {
					// stop to play the music
					DownloadUtil.player.end();
				}
				playBtn.setBackgroundResource(R.drawable.media_playback_start2);
				playBtn.setEnabled(false);
				v.setEnabled(false);
				displayToolbar(false);
			}
		});
		
		searchBtn.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				//set search
				search = 1;
				
				//set keyword
				keyword = searchTxt.getText().toString();
				
				//change the title
				String suffix = getString(R.string.search) + ": " + keyword;
				setTitle(getString(R.string.app_name_suffix, suffix));

				// set the page 0
				page = 0;
				
				//clear list
				ringtoneList.clear();
				
				//set selected_item = -1
				selected_item = -1;

				// get more ringtones by categor + order
				addMore();

				// go to the selected category
				RingtoneAdapter adapter = new RingtoneAdapter(
						MobileRingtoneActivity.this);

				setListAdapter(adapter);
				
				displaySearchBox(false);
				
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(searchTxt.getWindowToken(), 0);
			}
		});
	}
	
	public void displayToolbar(final boolean display) {
		runOnUiThread(new Thread() {
			public void run() {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (display) {
					btnPanel.setVisibility(View.VISIBLE);
				} else {
					btnPanel.setVisibility(View.GONE);
				}
			}
		});
	}
	
	public void displaySearchBox(final boolean display) {
		runOnUiThread(new Thread() {
			public void run() {
				if (display) {
					searchPanel.setVisibility(View.VISIBLE);
				} else {
					searchPanel.setVisibility(View.GONE);
				}
			}
		});
	}

	public void showHoldPanel(final boolean show) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (show) {
					downloadPanel.setVisibility(View.VISIBLE);
//					btnPanel.setVisibility(View.GONE);
				} else {
					downloadPanel.setVisibility(View.GONE);
//					btnPanel.setVisibility(View.VISIBLE);
				}
			}
		});

	}

	public void refreshList() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				
				((BaseAdapter) getListAdapter())
						.notifyDataSetChanged();
				
				getListView().setSelection(current_row);
			}
		});

	}

	public void addMore() {

		Runnable saveUrl = new Runnable() {

			public void run() {

				if (ringtoneList == null) {
					ringtoneList = new ArrayList<RingtoneModel>();
				}

				// show hold downloadPanel
				if (source == 0 && ringtoneList.size() == 0) {
					showHoldPanel(true);

				}else{
					//show toast
					toastMsg(R.string.wait);
				}

				switch (source) {

				case 0: {
					// get ringtones by category and orderby
					page++;
					current_row = ringtoneList.size();
					
					if(search == 1){
						ringtoneList.addAll(util.getRingtoneByKeyword(keyword, page));
					}else{
						ringtoneList.addAll(util.getRingtoneByCategoryLanguae(category, language,
								orderby, page));
					}

					break;
				}
				case 1: {
					// get 10 ringtones from local db
					page++;
					current_row = ringtoneList.size();
					
						ringtoneList.addAll(getLocalRingtone(page));

					break;
				}
				default:
					;
				}

				// hide hold downloadPanel
					showHoldPanel(false);

				// refresh the gallery view
				refreshList();
			}

		};
		new Thread(saveUrl).start();

	}

	private class RingtoneAdapter extends BaseAdapter {
		private LayoutInflater mInflater;

		public RingtoneAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return ringtoneList.size() + 1;
		}

		public RingtoneModel getItem(int i) {
			return ringtoneList.get(i);
		}

		public long getItemId(int i) {
			return i;
		}

		public View getView(final int position, View convertView, ViewGroup vg) {
			if (ringtoneList == null || position < 0
					|| position > ringtoneList.size())
				return null;

			final View row;

			// the last row is the more... row
			if (position == ringtoneList.size()) {
				// hide two buttons
//				holder.optBtn.setVisibility(View.GONE);
//				holder.playBtn.setVisibility(View.GONE);
				
					row = mInflater.inflate(R.layout.more_list_item, null);

				ViewHolder holder = (ViewHolder) row.getTag();
				if (holder == null) {
					holder = new ViewHolder(row);
					row.setTag(holder);
				}
				
				// set the title,used
				holder.title.setText(R.string.more);
				
			} else {
					row = mInflater.inflate(R.layout.list_item, null);

				ViewHolder holder = (ViewHolder) row.getTag();
				if (holder == null) {
					holder = new ViewHolder(row);
					row.setTag(holder);
				}
				
				// other normal row
				final RingtoneModel rm = ringtoneList.get(position);
				
				// set ringtones to label
				if (!locale.startsWith("zh")) {
					holder.title.setText(position + 1 + "." + rm.getNameEn());
				} else {
					holder.title.setText(position + 1 + "." + rm.getName());
				}
				
				if(rm.getIsUsed() == 1){
					holder.used.setText(R.string.local);
				}else if(rm.getIsUsed() == 2){
					holder.used.setText(R.string.used);
				}else{
					holder.used.setText("");
				}
				
				//hide the icon
				if(position == selected_item){
					holder.icon.setVisibility(View.VISIBLE);
				}else{
					holder.icon.setVisibility(View.GONE);
				}

				// initialize all of buttons
//				holder.playBtn.setOnClickListener(new OnClickListener() {
//
//					@Override
//					public void onClick(View v) {
//						row.setSelected(true);
//
//						// play the music
//						util.runSaveUrl(position, R.id.menu_play_ringtone,
//								ringtoneList.get(position).getUrl());
//					}
//				});
//
//				holder.optBtn.setOnClickListener(new OnClickListener() {
//
//					@Override
//					public void onClick(View v) {
//						// pop context menu
//						row.showContextMenu();
//
//					}
//				});
			}

			return (row);
		}

	}
	
	public void updateRingtoneLocation(final int position, final String location){
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if(position < ringtoneList.size()){
				
				//1.update list
				ringtoneList.get(position).setLocation(location);
				
				//2.refresh the list
				((BaseAdapter) MobileRingtoneActivity.this.getListAdapter())
						.notifyDataSetChanged();
				
				//3.update db
				dba.open();
				dba.updateLocation(ringtoneList.get(position).getUrl(), location);
				
				
				dba.close();
				}
			}
		});
	}
	
	public void updateRingtoneUsed(final int position, final int used){
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				//1.update list
				ringtoneList.get(position).setIsUsed(used);
				
				//2.refresh the list
				((BaseAdapter) MobileRingtoneActivity.this.getListAdapter())
						.notifyDataSetChanged();
				
				//3.update db
				dba.open();
				dba.updateUsed(ringtoneList.get(position).getUrl(), used);
				
				
				dba.close();
			}
		});
	}
	
	public RingtoneModel updateOrInsertRingtone(final RingtoneModel rm){
				//update or insert db
				dba.open();
				dba.updateOrInsert(rm);
				
				dba.close();
				
				return getRingtoneByUrl(rm.getUrl());
	}

	public String translate(String src) {
		Translate.setHttpReferrer("http://www.omdasoft.com");

		String translatedText;
		try {
			translatedText = Translate.execute(src, Language.CHINESE,
					Language.ENGLISH);
		} catch (Exception e) {
			translatedText = src;
		}

		return translatedText;
	}

	public String getSelected_ringtone() {
		return selected_ringtone;
	}

	public void setSelected_ringtone(String selected_ringtone) {
		this.selected_ringtone = selected_ringtone;
	}

	public ArrayList<RingtoneModel> getRingtoneList() {
		return ringtoneList;
	}
	
	public RingtoneModel getRingtoneByUrl(String url){
		RingtoneModel rm = new RingtoneModel(); 
		
				//update or insert db
				dba.open();
				Cursor c = dba.getRingtoneByUrl(url);
				startManagingCursor(c);
				
				if(c.moveToFirst()){
					long id = c.getLong(0);
					String name = c.getString(c
							.getColumnIndex(RingtoneDBOpenHelper.NAME));
					String nameEn = c.getString(c
							.getColumnIndex(RingtoneDBOpenHelper.NAME_EN));
					String location = c.getString(c
							.getColumnIndex(RingtoneDBOpenHelper.LOCATION));
					int used = c.getInt(c
							.getColumnIndex(RingtoneDBOpenHelper.IS_USED));
					rm.setId(id);
					rm.setName(name);
					rm.setNameEn(nameEn);
					rm.setUrl(url);
					rm.setLocation(location);
					rm.setIsUsed(used);
				}else{
					rm = null;
				}
				c.close();
				dba.close();
				
				return rm;
	}
	
	private ArrayList<RingtoneModel> getLocalRingtone(
			int page) {
		ArrayList<RingtoneModel> list = new ArrayList<RingtoneModel>();
		
		//update or insert db
		dba.open();
		Cursor c = dba.getLocalRingtone(page);
		startManagingCursor(c);
		
		if (c.moveToFirst()) {
			do {
			RingtoneModel rm = new RingtoneModel(); 
			
			long id = c.getLong(0);
			String name = c.getString(c
					.getColumnIndex(RingtoneDBOpenHelper.NAME));
			String nameEn = c.getString(c
					.getColumnIndex(RingtoneDBOpenHelper.NAME_EN));
			String location = c.getString(c
					.getColumnIndex(RingtoneDBOpenHelper.LOCATION));
			String url = c.getString(c
					.getColumnIndex(RingtoneDBOpenHelper.URL));
			int used = c.getInt(c
					.getColumnIndex(RingtoneDBOpenHelper.IS_USED));
			rm.setId(id);
			rm.setName(name);
			rm.setNameEn(nameEn);
			rm.setUrl(url);
			rm.setLocation(location);
			rm.setIsUsed(used);
			
			list.add(rm);
		}while(c.moveToNext());
		}
		c.close();
		dba.close();
		
		return list;
	}

	class ViewHolder {
		TextView title = null;
		TextView used = null;
		ImageView icon = null;

		ViewHolder(View base) {
			this.title = (TextView) base.findViewById(R.id.row_title);
			this.used = (TextView) base.findViewById(R.id.row_used);
			this.icon = (ImageView) base.findViewById(R.id.icon);
		}
	}

}