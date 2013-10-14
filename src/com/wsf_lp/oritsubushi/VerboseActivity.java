package com.wsf_lp.oritsubushi;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
//import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.wsf_lp.android.Prefs;
import com.wsf_lp.mapapp.data.Database;
import com.wsf_lp.mapapp.data.OritsubushiBroadcastReceiver;
import com.wsf_lp.mapapp.data.DatabaseResultReceiver;
import com.wsf_lp.mapapp.data.DatabaseService;
import com.wsf_lp.mapapp.data.DatabaseServiceConnector;
import com.wsf_lp.mapapp.data.OritsubushiNotificationIntent;
import com.wsf_lp.mapapp.data.Station;

public class VerboseActivity
	extends Activity
	implements DatabaseResultReceiver,
		DatabaseServiceConnector.Listener,
		OritsubushiBroadcastReceiver.UpdateListener {

	private static final String STATION_NAME = "com.wsf_lp.oritsubushi.Station";
	private static final String STATE_TAG = "station";

	private static final int EDIT_DATE_DIALOG = 0;
	private static final int EDIT_MEMO_DIALOG = 1;

	public static class VerboseIntent extends Intent {
		public VerboseIntent(Activity activity, Station station) {
			super(activity, VerboseActivity.class);
			setAction(Intent.ACTION_VIEW);
			putExtra(STATION_NAME, station);
		}
	}

	public static void start(Activity parent, Station station) {
    	parent.startActivity(new VerboseIntent(parent, station));
    	//parent.overridePendingTransition(R.anim.none, R.anim.slide_out_right);
	}

	private boolean isAlive;

	private ActivityChanger activityChanger;

	private DatabaseServiceConnector connector;
	private DatabaseService databaseService;
	private OritsubushiBroadcastReceiver broadcastReceiver;
	private TextView title;
	private TextView yomi;
	private TextView lines;
	private TextView address;
	private TextView completionDate;
	private TextView memo;
	private Button editDate;
	private Button completionOnToday;
	private Button editMemo;

	private Station station;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		isAlive = true;
        activityChanger = new ActivityChanger(this, 0);

		setContentView(R.layout.verbose);
		overridePendingTransition(R.anim.slide_in_right, R.anim.none);
		title = (TextView)findViewById(R.id.verbose_title);
		yomi = (TextView)findViewById(R.id.verbose_yomi);
		lines = (TextView)findViewById(R.id.verbose_lines);
		address = (TextView)findViewById(R.id.verbose_address);
		completionDate = (TextView)findViewById(R.id.verbose_comp_date);
		memo = (TextView)findViewById(R.id.verbose_memo);
		editDate = (Button)findViewById(R.id.verbose_button_edit_date);
		completionOnToday = (Button)findViewById(R.id.verbose_button_comp_today);
		editMemo = (Button)findViewById(R.id.verbose_button_edit_memo);

		editDate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(EDIT_DATE_DIALOG);
			}
		});
		completionOnToday.setOnClickListener(compTodayProc);
		editMemo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(EDIT_MEMO_DIALOG);
			}
		});
		findViewById(R.id.verbose_button_wikipedia).setOnClickListener(wikiProc);
		findViewById(R.id.verbose_button_move_to).setOnClickListener(moveToProc);

		station = getIntent().getParcelableExtra(STATION_NAME);
		if(station == null && savedInstanceState != null) {
			station = (Station)savedInstanceState.getParcelable(STATE_TAG);
		}
        loadStation();

        broadcastReceiver = new OritsubushiBroadcastReceiver(this);
        broadcastReceiver.registerTo(this, OritsubushiNotificationIntent.getIntentFilter());
        connector = new DatabaseServiceConnector();
        connector.connect(this, this);
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		station = savedInstanceState.getParcelable(STATE_TAG);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_TAG, station);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			overridePendingTransition(R.anim.none, R.anim.slide_out_right);
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onDestroy() {
		isAlive = false;
		broadcastReceiver.unregisterFrom(this);
		connector.disconnect();
		super.onDestroy();
	}

	public Station getStation() { return station; }
	public DatabaseService getDatabaseService() { return databaseService; }

	private EditDateDialogHelper editDateDialogHelper;

	private static class EditMemoDialogHelper {
		private WeakReference<VerboseActivity> activity;
		private EditText memo;
		public Dialog createDialog(final VerboseActivity activity_) {
			activity = new WeakReference<VerboseActivity>(activity_);
			memo = new EditText(activity_);
			final AlertDialog dialog = new AlertDialog.Builder(activity_)
				.setTitle(activity_.getString(R.string.verbose_edit_memo_title, activity_.station.getTitle()))
				.setView(memo)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						final VerboseActivity activity_ = activity.get();
						if(activity_ == null) {
							return;
						}
						activity_.station.setMemo(memo.getText().toString());
						activity_.databaseService.callDatabase(activity_, Database.MethodName.UPDATE_MEMO, activity_.station);
						activity_.updateText();
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.create();
			memo.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
					}
				}
			});
			return dialog;
		}
		public void prepareDialog(Dialog dialog) {
			final VerboseActivity activity = this.activity.get();
			if(activity != null) {
				memo.setText(activity.station.getMemo());
			}
		}
	}

	private EditMemoDialogHelper editMemoDialogHelper;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case EDIT_DATE_DIALOG:
			if(editDateDialogHelper == null) {
				editDateDialogHelper = new EditDateDialogHelper();
			}
			return editDateDialogHelper.createDialog(this);
		case EDIT_MEMO_DIALOG:
			if(editMemoDialogHelper == null) {
				editMemoDialogHelper = new EditMemoDialogHelper();
			}
			return editMemoDialogHelper.createDialog(this);
		default:
			return null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		switch(id) {
		case EDIT_DATE_DIALOG:
			editDateDialogHelper.prepareDialog(dialog);
			break;
		case EDIT_MEMO_DIALOG:
			editMemoDialogHelper.prepareDialog(dialog);
			break;
		}
	}

	private final OnClickListener compTodayProc = new OnClickListener() {
		@Override
		public void onClick(View v) {
			station.setCompletionToday();
			databaseService.callDatabase(VerboseActivity.this, Database.MethodName.UPDATE_COMPLETION, station);
			updateText();
		}
	};

	private final OnClickListener wikiProc = new OnClickListener() {
		@Override
		public void onClick(View v) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://ja.m.wikipedia.org/wiki/" + Uri.encode(station.getWiki()))));
		}
	};

	private final OnClickListener moveToProc = new OnClickListener() {
		@Override
		public void onClick(View v) {
			sendBroadcast(new OritsubushiNotificationIntent().setMapMoveTo(station));
		}
	};

	protected void loadStation() {
		if(!station.isReadyToCreateSubtitle() && databaseService != null) {
			databaseService.callDatabase(this, Database.MethodName.LOAD_LINES, station);
		}
		updateText();
	}

	protected void updateText() {
		if(!isAlive) {
			return;
		}
		title.setText(station.getTitle());
		yomi.setText(station.getYomi());
		lines.setText(station.getSubtitle());
		address.setText(Prefs.getValue(station.getPref(), getResources()) + station.getAddress());
		completionDate.setText(station.getCompletionDateString(getResources()));
		memo.setText(station.getMemo());
		completionOnToday.setEnabled(!station.isCompleted());
		final boolean enableEdit = station.isReadyToCreateSubtitle();
		editDate.setEnabled(enableEdit);
		editMemo.setEnabled(enableEdit);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        activityChanger.createMenu(menu);
        return true;
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		activityChanger.prepareMenu(menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        return activityChanger.onSelectActivityMenu(item.getItemId());
    }

	@Override
	public void onDatabaseResult(long sequence, String methodName, Object result) {
		updateText();
	}

	@Override
	public void onDatabaseUpdated(Station station, int sequence) {
		if(station == null) {
			isAlive = false;
			finish();
		} else if(isAlive && this.station.getCode() == station.getCode()) {
			this.station = station;
			loadStation();
		}
	}

	@Override
	public void onDatabaseConnected(DatabaseService service) {
		this.databaseService = service;
		loadStation();
	}

	@Override
	public void onDatabaseDisconnected() {
		editDate.setEnabled(false);
		editMemo.setEnabled(false);
	}

}
