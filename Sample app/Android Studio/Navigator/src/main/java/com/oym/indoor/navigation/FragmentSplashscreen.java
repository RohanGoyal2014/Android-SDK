package com.oym.indoor.navigation;


import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.oym.indoor.Indoor.ConnectCallback;
import com.oym.indoor.Notification;
import com.oym.indoor.location.IndoorLocation;
import com.oym.indoor.location.IndoorLocationBroadcast;
import com.oym.indoor.location.IndoorLocationLib;
import com.oym.indoor.location.IndoorLocationLib.StartLocateCallback;
import com.oym.indoor.location.NotificationResult;
import com.oym.indoor.routing.IndoorRouting;

public class FragmentSplashscreen extends Fragment {

	public static final String TAG = "FragmentSplashscreen";
	
	public static final String KEY_CANCELLOGIN = "SPLASHSCREEN_CANCELLOGIN";
	
	private static final String KEY_PREF_USER = "OYM_KEY_USER";
	private static final String KEY_PREF_PASSWORD = "OYM_KEY_PASSWORD";
	public static final String KEY_PREF_AUTOLOGIN = "OYM_KEY_AUTOLOGIN";
	
	private static final String USER_PREFIX = "user@indoor.";
	private static final String URL = "https://indoor.onyourmap.com:8443/links";	
	
	
	// View
	private ProgressBar bar;
	private TextView textview;
	private LinearLayout loginLayout;
	private LinearLayout progressLayout;
	private EditText accountEditText;
	private EditText passwordEditText;
	private Button button;
	private LinearLayout helpLayout;
	
	// Flags
	private boolean avoidDestroy = false;
		
	// Data	
	private GlobalState gs;
	private String packageName;
	private String user;
	private String password;


	// Connect to Links
	private ConnectCallback callbackConnect = new ConnectCallback() {

		@Override
		public void onSucceed() {
			Log.i(TAG, "Links: Connected");
			
			SharedPreferences.Editor sharedPrefsEditor = gs.getSharedPrefs().edit();
			sharedPrefsEditor.putBoolean(KEY_PREF_AUTOLOGIN, true);
			sharedPrefsEditor.commit();

			// Start Indoor Service
			gs.setIndoorLib(new IndoorLocationLib(getActivity(), URL, user, password));
			gs.getIndoorLib().startLocate(indoorLocation, callbackStartLocate, 2000);
		}

		@Override
		public void onFailure(Exception exc) {
			showError(R.string.FSConnectError);
			Log.e(TAG, "Links: Error connecting: "+exc.getMessage());
			SharedPreferences.Editor sharedPrefsEditor = gs.getSharedPrefs().edit();
			sharedPrefsEditor.putBoolean(KEY_PREF_AUTOLOGIN, false);
			sharedPrefsEditor.commit();
		}
	};
	
	private StartLocateCallback callbackStartLocate = new StartLocateCallback() {

		@Override
		public void onSucceed() {
			onReady();
		}

		@Override
		public void onFailure(Exception exc) {
			showError(R.string.FSLibError);
		}
	};
	
	
	private IndoorLocationBroadcast indoorLocation = new IndoorLocationBroadcast() {

		@Override
		public void onReceive(IndoorLocation location) {
			ArrayList<IndoorLocationListener> list = gs.getLocationCallback();
			for (IndoorLocationListener listener: list) {
				listener.onLocationUpdate(location);
			}
		}

		@Override
		public void onReceive(NotificationResult nr) {
			ArrayList<IndoorLocationListener> list = gs.getLocationCallback();
			for (IndoorLocationListener listener: list) {
				listener.onNotification(nr);
			}
		}
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_splashscreen, container, false);
		
		gs = (GlobalState) getActivity().getApplication();
		
		bar = (ProgressBar) view.findViewById(R.id.SProgressbar);
		textview = (TextView) view.findViewById(R.id.STextview);
		loginLayout = (LinearLayout) view.findViewById(R.id.SLoginLayout);
		progressLayout = (LinearLayout) view.findViewById(R.id.SProgressLayout);
		accountEditText = (EditText) view.findViewById(R.id.SAccountEditText);
		passwordEditText = (EditText) view.findViewById(R.id.SPasswordEditText);
		button = (Button) view.findViewById(R.id.SButton);
		helpLayout = (LinearLayout) view.findViewById(R.id.SInfoLayout);
		
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onClickLogin();
			}
		});
		passwordEditText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
				            Context.INPUT_METHOD_SERVICE);
				        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
					button.performClick();
					return true;
				}
				return false;
			}
		});
		helpLayout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
//				getActivity().getSupportFragmentManager().beginTransaction()
//				.replace(R.id.container, new FragmentInfo()).addToBackStack("help").commit();
////				actionBar.show();
////				actionBar.setTitle("Instructions");
////				actionBar.setDisplayHomeAsUpEnabled(true);
////				drawerToggle.setDrawerIndicatorEnabled(false);
////				drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.HText);
				builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
				builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_BACK) {	                    
		                    dialog.dismiss();
		                    return true;
		                }
						return false;
					}
				});
				builder.setCancelable(false);
				builder.create().show();
			}
		});

		return view;
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		packageName = getClass().getPackage().getName();
		
		// Login
		gs.setSharedPrefs(PreferenceManager.getDefaultSharedPreferences(getActivity()));
		boolean a = gs.getSharedPrefs().getBoolean(KEY_PREF_AUTOLOGIN, false);
		String u = gs.getSharedPrefs().getString(KEY_PREF_USER, "");
		String p = gs.getSharedPrefs().getString(KEY_PREF_PASSWORD, "");
		if (a && !u.equals("") && !p.equals("")) {
			startApp(u, p);
		} else {
			progressLayout.setVisibility(View.GONE);
			loginLayout.setVisibility(View.VISIBLE);
			helpLayout.setVisibility(View.VISIBLE);
		}
	}
	
	private void startApp(String us, String pwd) {
		user = us;
		password = pwd;
		progressLayout.setVisibility(View.VISIBLE);
        bar.setVisibility(View.VISIBLE);
		loginLayout.setVisibility(View.GONE);
		helpLayout.setVisibility(View.GONE);
		// Start App
		if (!getArguments().getBoolean(KEY_CANCELLOGIN, false)) {
			gs.setLinks(new IndoorRouting(URL, user, password, callbackConnect));
		}
	}
	
	private void onClickLogin() {
		String u = USER_PREFIX + accountEditText.getText().toString().trim();
		String p = passwordEditText.getText().toString().trim();		
		SharedPreferences.Editor sharedPrefsEditor = gs.getSharedPrefs().edit();
		sharedPrefsEditor.putString(KEY_PREF_USER, u);
		sharedPrefsEditor.putString(KEY_PREF_PASSWORD, p);
		sharedPrefsEditor.commit();
		showText(R.string.FSConnecting);
		startApp(u, p);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (!avoidDestroy) {
			if (gs.getLinks() != null) {
				gs.getLinks().disconnect();
			}
			if (gs.getIndoorLib() != null) {
				gs.getIndoorLib().stopLocate();
			}			
		}
	}


	private void showText(int resource) {
        textview.setTextColor(getResources().getColor(R.color.textPrimaryColor));
		textview.setText(resource);
	}
	
	private void showError(int resource) {
		bar.setVisibility(View.GONE);
		showText(resource);
		textview.setTextColor(getResources().getColor(R.color.accentColor));
		loginLayout.setVisibility(View.VISIBLE);
		helpLayout.setVisibility(View.VISIBLE);
	}

	private void onReady() {
		Intent intent = new Intent(getActivity(), ActivityMap.class);
		startActivity(intent);
		avoidDestroy = true;
		getActivity().finish();
	}
}
