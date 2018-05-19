package com.mcarvalhor.apps.mediaconverter;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

	// Used to load the 'native-lib' library on application startup.
	static {
		System.loadLibrary("native-lib");
	}

	class MessengerHandler extends Handler {
		public static final int
				MSG_LISTUPDATE = 1,
				MSG_ITEMADDED = 2,
				MSG_ITEMSWAPUP = 4,
				MSG_ITEMSWAPDOWN = 8,
				MSG_ITEMREMOVED = 16,
				MSG_DONEREMOVED = 32,
				MSG_STARTED = 64,
				MSG_PROGRESS = 128,
				MSG_FINISHED = 256,
				MSG_UNLOCKUI = 512;

		@Override
		public void handleMessage(Message msg) {
			LinearLayout AuxLayout, ListOfItems = (LinearLayout) findViewById(R.id.TaskListItems);
			Utils.MediaTask AuxTaskA, AuxTaskB;
			Bundle AuxData = msg.getData();
			double AuxCompleted, AuxTotal;
			int AuxIndex, AuxCount, AuxN;
			String AuxId;
			switch (msg.what) {
				case MSG_LISTUPDATE:
					Update(Utils.MediaTask.GetArray(msg.getData().getByteArray("new_list")));
					break;
				case MSG_ITEMADDED:
					AuxTaskA = new Utils.MediaTask(AuxData.getByteArray("new_task"));
					TaskList.add(AuxTaskA);
					AuxLayout = CreateTaskLayout(AuxTaskA, String.format(Locale.US, "%03d. ", TaskList.size()));
					ListOfItems.addView(AuxLayout);
					ListOfItems.setEnabled(true);
					findViewById(R.id.LoadingBar).setVisibility(View.INVISIBLE);
					break;
				case MSG_ITEMSWAPUP:
					AuxIndex = AuxData.getInt("task_index");
					AuxId = AuxData.getString("task_id");
					if (AuxIndex < TaskList.size() && AuxIndex > 0
							&& (AuxTaskA = TaskList.get(AuxIndex)).Id.equals(AuxId)) {
						AuxTaskB = TaskList.get(AuxIndex - 1);
						ListOfItems.removeView(AuxTaskA.ActivityLayout);
						ListOfItems.removeView(AuxTaskB.ActivityLayout);
						TaskList.set(AuxIndex - 1, AuxTaskA);
						TaskList.set(AuxIndex, AuxTaskB);
						ListOfItems.addView(AuxTaskA.ActivityLayout, AuxIndex - 1);
						ListOfItems.addView(AuxTaskB.ActivityLayout, AuxIndex);
						ListOfItems.setEnabled(true);
						findViewById(R.id.LoadingBar).setVisibility(View.INVISIBLE);
					} else {
						RequestUpdate();
					}
					break;
				case MSG_ITEMSWAPDOWN:
					AuxIndex = AuxData.getInt("task_index");
					AuxId = AuxData.getString("task_id");
					if (AuxIndex < TaskList.size() - 1 && AuxIndex >= 0
							&& (AuxTaskA = TaskList.get(AuxIndex)).Id.equals(AuxId)) {
						AuxTaskB = TaskList.get(AuxIndex + 1);
						ListOfItems.removeView(AuxTaskA.ActivityLayout);
						ListOfItems.removeView(AuxTaskB.ActivityLayout);
						TaskList.set(AuxIndex, AuxTaskB);
						TaskList.set(AuxIndex + 1, AuxTaskA);
						ListOfItems.addView(AuxTaskB.ActivityLayout, AuxIndex);
						ListOfItems.addView(AuxTaskA.ActivityLayout, AuxIndex + 1);
						ListOfItems.setEnabled(true);
						findViewById(R.id.LoadingBar).setVisibility(View.INVISIBLE);
					} else {
						RequestUpdate();
					}
					break;
				case MSG_ITEMREMOVED:
					AuxIndex = AuxData.getInt("task_index");
					AuxId = AuxData.getString("task_id");
					if (AuxIndex < TaskList.size() && AuxIndex >= 0
							&& (AuxTaskA = TaskList.get(AuxIndex)).Id.equals(AuxId)) {
						ListOfItems.removeView(AuxTaskA.ActivityLayout);
						TaskList.remove(AuxIndex);
						ListOfItems.setEnabled(true);
						findViewById(R.id.LoadingBar).setVisibility(View.INVISIBLE);
					} else {
						RequestUpdate();
					}
					break;
				case MSG_DONEREMOVED:
					AuxN = AuxData.getInt("total_items");
					for (AuxIndex = TaskList.size() - 1, AuxCount = 0; AuxIndex >= 0; AuxIndex--) {
						if (TaskList.get(AuxIndex).Status == Utils.MediaTask.STATUS_DONE) {
							ListOfItems.removeView(TaskList.remove(AuxIndex).ActivityLayout);
							AuxCount++;
						}
					}
					ListOfItems.setEnabled(true);
					findViewById(R.id.LoadingBar).setVisibility(View.INVISIBLE);
					if (AuxCount != AuxN) RequestUpdate();
					break;
				case MSG_STARTED:
					AuxIndex = AuxData.getInt("task_index");
					AuxId = AuxData.getString("task_id");
					if (AuxIndex < TaskList.size() && AuxIndex >= 0
							&& (AuxTaskA = TaskList.get(AuxIndex)).Id.equals(AuxId) && AuxTaskA.Status == Utils.MediaTask.STATUS_QUEUED) {
						ListOfItems.removeView(AuxTaskA.ActivityLayout);
						AuxTaskA.Status = Utils.MediaTask.STATUS_PROCESSING;
						AuxLayout = CreateTaskLayout(AuxTaskA, String.format(Locale.US, "%03d. ", AuxIndex));
						ListOfItems.addView(AuxLayout, AuxIndex);
					} else {
						RequestUpdate();
					}
					break;
				case MSG_PROGRESS:
					AuxIndex = AuxData.getInt("task_index");
					AuxId = AuxData.getString("task_id");
					AuxCompleted = AuxData.getDouble("task_completed", -1);
					AuxTotal = AuxData.getDouble("task_total", -1);
					if (AuxIndex < TaskList.size() && AuxIndex >= 0
							&& (AuxTaskA = TaskList.get(AuxIndex)).Id.equals(AuxId) && AuxTaskA.Status == Utils.MediaTask.STATUS_PROCESSING) {
						if(AuxCompleted >= 0 && AuxCompleted <= AuxTotal) {
							AuxTaskA.ActivityProgressBar.setIndeterminate(false);
							AuxTaskA.ActivityProgressBar.setMax((int) (AuxTotal * 100000)); // 100000 => precisão de 3 dígitos w.xyz
							AuxTaskA.ActivityProgressBar.setProgress((int) (AuxCompleted * 100000));
							AuxTaskA.ActivityInformation.setText(String.format(Locale.US, getString(R.string.task_progress), 100 * AuxCompleted / AuxTotal, (int)Math.floor(AuxCompleted), (int)Math.ceil(AuxTotal)));
						}else{
							AuxTaskA.ActivityProgressBar.setIndeterminate(true);
							AuxTaskA.ActivityInformation.setText(getString(R.string.task_indprogress));
						}
					} else {
						RequestUpdate();
					}
					break;
				case MSG_FINISHED:
					AuxIndex = AuxData.getInt("task_index");
					AuxId = AuxData.getString("task_id");
					AuxN = AuxData.getInt("task_results");
					if (AuxIndex < TaskList.size() && AuxIndex >= 0
							&& (AuxTaskA = TaskList.get(AuxIndex)).Id.equals(AuxId) && AuxTaskA.Status == Utils.MediaTask.STATUS_PROCESSING) {
						ListOfItems.removeView(AuxTaskA.ActivityLayout);
						AuxTaskA.Status = Utils.MediaTask.STATUS_DONE;
						AuxTaskA.Results = AuxN;
						AuxTaskA.ActivityProgressBar = null;
						AuxLayout = CreateTaskLayout(AuxTaskA, String.format(Locale.US, "%03d. ", AuxIndex));
						ListOfItems.addView(AuxLayout, AuxIndex);
					} else {
						RequestUpdate();
					}
					break;
				case MSG_UNLOCKUI:
					ListOfItems.setEnabled(true);
					findViewById(R.id.LoadingBar).setVisibility(View.INVISIBLE);
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	private Messenger BoundService = null;
	private final Messenger ActivityMessenger = new Messenger(new MessengerHandler());
	private ServiceConnection ServiceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			BoundService = new Messenger(iBinder);
			RequestUpdate();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			BoundService = null;
		}
	};

	private static final String TAG = "MainActivity";
	private ArrayList<Utils.MediaTask> TaskList = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent GalChos = new Intent();
				GalChos.setAction(Intent.ACTION_GET_CONTENT);
				GalChos.setType("*/*");
				startActivityForResult(Intent.createChooser(GalChos, "Selecione qualquer arquivo de midia."),1);
			}
		});

		RequestUpdate();

	}

	public void onActivityResult(int reqCode, int resCode, Intent data){
		if(resCode != RESULT_OK || reqCode != 1 || data == null) {
			return;
		}
		Uri FilePath = data.getData();
		Utils.MediaTask AuxTask;
		Bundle AuxBundle;
		Message AuxMsg;
		String TaskId;
		TaskId = Double.toString(Math.random());
		try {
			InputStream src = getContentResolver().openInputStream(FilePath);
			OutputStream dest = openFileOutput(TaskId + ".mp4", Context.MODE_PRIVATE);
			byte[] R = new byte[5242880]; // 5 MiB
			int memsize=0;
			while( (memsize=src.read(R, 0, R.length))>0 ){
				dest.write(R, 0, memsize);
			}
			src.close();
			dest.close();
		} catch (Exception ex) {
			Snackbar.make(null, "Erro ao copiar arquivos", Snackbar.LENGTH_LONG)
					.setAction("Action", null).show();
			return;
		}
		try {
			AuxMsg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_ADDTASK);
			AuxMsg.replyTo = ActivityMessenger;
			AuxTask = new Utils.MediaTask(TaskId, Utils.MediaTask.TYPE_CONVERSION, new String[] { "-i", getFilesDir() + "/" + TaskId + ".mp4", getFilesDir() + "/output.mp4"}, new String[0], new String[0][0], MainActivity.this);
			AuxBundle = new Bundle();
			AuxBundle.putByteArray("task", AuxTask.GetBytes());
			AuxMsg.setData(AuxBundle);
			BoundService.send(AuxMsg);
		} catch (Exception e){
			Log.w(TAG, "Could not delivery UpdateList message.");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		switch(id) {
			case R.id.action_settings:	return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void SwapTaskUp(Utils.MediaTask Task){
		LinearLayout ListOfItems = (LinearLayout) findViewById(R.id.TaskListItems);
		Bundle MsgBundle;
		Message Msg;
		int Index;
		try {
			Index = ListOfItems.indexOfChild(Task.ActivityLayout);
			if(Index >= 0 && Index < TaskList.size() && TaskList.get(Index).Id.equals(Task.Id) && BoundService != null) {
				Msg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_SWAPTASKUP);
				MsgBundle = new Bundle();
				MsgBundle.putInt("task_index", Index);
				MsgBundle.putString("task_id", Task.Id);
				Msg.setData(MsgBundle);
				Msg.replyTo = ActivityMessenger;
				BoundService.send(Msg);
				ListOfItems.setEnabled(false);
				findViewById(R.id.LoadingBar).setVisibility(View.VISIBLE);
			}else	RequestUpdate();
		} catch(Exception e) {
			Log.w(TAG, "Could not delivery SwapUp message.");
		}
	}

	private void SwapTaskDown(Utils.MediaTask Task){
		LinearLayout ListOfItems = (LinearLayout) findViewById(R.id.TaskListItems);
		Bundle MsgBundle;
		Message Msg;
		int Index;
		try {
			Index = ListOfItems.indexOfChild(Task.ActivityLayout);
			if(Index >= 0 && Index < TaskList.size() && TaskList.get(Index).Id.equals(Task.Id) && BoundService != null) {
				Msg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_SWAPTASKDOWN);
				MsgBundle = new Bundle();
				MsgBundle.putInt("task_index", Index);
				MsgBundle.putString("task_id", Task.Id);
				Msg.setData(MsgBundle);
				Msg.replyTo = ActivityMessenger;
				BoundService.send(Msg);
				ListOfItems.setEnabled(false);
				findViewById(R.id.LoadingBar).setVisibility(View.VISIBLE);
			}else	RequestUpdate();
		} catch(Exception e) {
			Log.w(TAG, "Could not delivery SwapDown message.");
		}
	}

	private void RemoveTask(Utils.MediaTask Task){
		LinearLayout ListOfItems = (LinearLayout) findViewById(R.id.TaskListItems);
		Bundle MsgBundle;
		Message Msg;
		int Index;
		try {
			Index = ListOfItems.indexOfChild(Task.ActivityLayout);
			if(Index >= 0 && Index < TaskList.size() && TaskList.get(Index).Id.equals(Task.Id) && BoundService != null) {
				Msg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_REMOVETASK);
				MsgBundle = new Bundle();
				MsgBundle.putInt("task_index", Index);
				MsgBundle.putString("task_id", Task.Id);
				Msg.setData(MsgBundle);
				Msg.replyTo = ActivityMessenger;
				BoundService.send(Msg);
				ListOfItems.setEnabled(false);
				findViewById(R.id.LoadingBar).setVisibility(View.VISIBLE);
			}else	RequestUpdate();
		} catch(Exception e) {
			Log.w(TAG, "Could not delivery Remove message.");
		}
	}

	private LinearLayout CreateTaskLayout(Utils.MediaTask Item, String Identifier){
		LinearLayout ItemContainer;
		String ItemShownText = Identifier;
		double DisplayDensity = getResources().getDisplayMetrics().density;
		switch(Item.Type){
			case Utils.MediaTask.TYPE_CONVERSION:	ItemShownText += "Conversão";
				break;
			default:	ItemShownText += "Tarefa de Mídia";
		}
		ItemShownText += "_" + Item.Id;
		ItemContainer = new LinearLayout(this);
		ItemContainer.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams ItemContainerLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
		ItemContainerLayout.setMargins(0, (int) (DisplayDensity*5), 0, (int) (DisplayDensity*5));
		ItemContainer.setLayoutParams(ItemContainerLayout);
		LinearLayout ItemTextContainer = new LinearLayout(this);
		ItemTextContainer.setOrientation(LinearLayout.VERTICAL);
		ItemTextContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
		TextView ItemText = new TextView(this);
		ItemText.setText(ItemShownText);
		ItemText.setTextAppearance(this, R.style.TextAppearance_AppCompat_Body1);
		ItemText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
		TextView ItemCaption = new TextView(this);
		switch(Item.Status){
			case Utils.MediaTask.STATUS_DONE:
				switch(Item.Results) {
					case Utils.MediaTask.RESULTS_OK:
						ItemCaption.setText(getString(R.string.task_ok));
						break;
					case Utils.MediaTask.RESULTS_CANCELLED:
						ItemCaption.setText(getString(R.string.task_cancelled));
						break;
					case Utils.MediaTask.RESULTS_INPUTNOTFOUND:
						ItemCaption.setText(getString(R.string.task_inputnotfound));
						break;
					case Utils.MediaTask.RESULTS_EXTERNALWRITEERROR:
						ItemCaption.setText(getString(R.string.task_externalwriteerror));
						break;
					case Utils.MediaTask.RESULTS_LIBRARYERROR:
						ItemCaption.setText(getString(R.string.task_libraryerror));
						break;
					default:
						ItemCaption.setText(getString(R.string.task_unknownerror));
				}
				break;
			case Utils.MediaTask.STATUS_PROCESSING:
				ItemCaption.setText(getString(R.string.task_indprogress));
				break;
			default:
				ItemCaption.setText(getString(R.string.task_queued));
		}
		ItemCaption.setTextAppearance(this, R.style.TextAppearance_AppCompat_Caption);
		ItemCaption.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
		LinearLayout ItemButtonsContainer = new LinearLayout(this);
		ItemButtonsContainer.setOrientation(LinearLayout.HORIZONTAL);
		ItemButtonsContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
		ItemButtonsContainer.setMinimumHeight((int) (DisplayDensity * 35));
		ItemButtonsContainer.setGravity(Gravity.CENTER);
		ImageButton SwapAboveButton = new ImageButton(this);
		SwapAboveButton.setLayoutParams(new LinearLayout.LayoutParams((int)(DisplayDensity * 325/3), (int)(DisplayDensity * 35), 1.0f));
		SwapAboveButton.setImageDrawable(getDrawable(android.R.drawable.arrow_up_float));
		SwapAboveButton.setScaleType(ImageButton.ScaleType.FIT_CENTER);
		SwapAboveButton.setContentDescription(getString(R.string.button_swapabove));
		SwapAboveButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View view){
				try{
					SwapTaskUp((Utils.MediaTask) ((View)view.getParent().getParent()).getTag());
				} catch (Exception e) {
					Utils.FatalError(TAG, MainActivity.this);
				}
			}
		});
		ImageButton SwapBelowButton = new ImageButton(this);
		SwapBelowButton.setLayoutParams(new LinearLayout.LayoutParams((int)(DisplayDensity * 325/3), (int)(DisplayDensity * 35), 1.0f));
		SwapBelowButton.setImageDrawable(getDrawable(android.R.drawable.arrow_down_float));
		SwapBelowButton.setScaleType(ImageButton.ScaleType.FIT_CENTER);
		SwapBelowButton.setContentDescription(getString(R.string.button_swapbelow));
		SwapBelowButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View view){
				try{
					SwapTaskDown((Utils.MediaTask) ((View)view.getParent().getParent()).getTag());
				} catch (Exception e) {
					Utils.FatalError(TAG, MainActivity.this);
				}
			}
		});
		ImageButton DeleteButton = new ImageButton(this);
		DeleteButton.setLayoutParams(new LinearLayout.LayoutParams((int)(DisplayDensity * 325/3), (int)(DisplayDensity * 35), 1.0f));
		DeleteButton.setImageDrawable(getDrawable(android.R.drawable.ic_delete));
		DeleteButton.setScaleType(ImageButton.ScaleType.FIT_CENTER);
		DeleteButton.setContentDescription(getString(R.string.button_delete));
		DeleteButton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View view){
				try{
					RemoveTask((Utils.MediaTask) ((View)view.getParent().getParent()).getTag());
				} catch (Exception e) {
					Utils.FatalError(TAG, MainActivity.this);
				}
			}
		});
		ItemTextContainer.addView(ItemText);
		if(Item.Status == Utils.MediaTask.STATUS_PROCESSING){
			ProgressBar ItemProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
			ItemProgress.setIndeterminate(true);
			ItemProgress.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
			ItemTextContainer.addView(ItemProgress);
			Item.ActivityProgressBar = ItemProgress;
		}
		ItemTextContainer.addView(ItemCaption);
		ItemButtonsContainer.addView(SwapAboveButton);
		ItemButtonsContainer.addView(SwapBelowButton);
		ItemButtonsContainer.addView(DeleteButton);
		ItemContainer.addView(ItemTextContainer);
		ItemContainer.addView(ItemButtonsContainer);
		ItemContainer.setTag(Item);
		Item.ActivityLayout = ItemContainer;
		Item.ActivityInformation = ItemCaption;
		return ItemContainer;
	}

	public void Update(Utils.MediaTask[] NewList){
		if(NewList == null)	NewList = new Utils.MediaTask[0];
		LinearLayout ListOfItems = (LinearLayout) findViewById(R.id.TaskListItems);
		int i;
		TaskList.clear();
		ListOfItems.removeAllViews();
		TaskList.addAll(Arrays.asList(NewList));
		for(i = 0; i < NewList.length; i++) {
			ListOfItems.addView(CreateTaskLayout(NewList[i], String.format(Locale.US, "%03d. ", i + 1)));
		}
		ListOfItems.setEnabled(true);
		findViewById(R.id.LoadingBar).setVisibility(View.INVISIBLE);
	}

	public void RequestUpdate(){
		LinearLayout ListOfItems = (LinearLayout) findViewById(R.id.TaskListItems);
		Message AuxMsg;
		ListOfItems.setEnabled(false);
		findViewById(R.id.LoadingBar).setVisibility(View.VISIBLE);
		if(BoundService != null){
			try {
				AuxMsg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_UPDATELIST);
				AuxMsg.replyTo = ActivityMessenger;
				BoundService.send(AuxMsg);
			} catch (Exception e){
				Log.w(TAG, "Could not delivery UpdateList message.");
			}
		}else	bindService(new Intent(getApplicationContext(), TaskProcess.class), ServiceConn, BIND_AUTO_CREATE);
	}

	/**
	 * A native method that is implemented by the 'native-lib' native library,
	 * which is packaged with this application.
	 */
	public native String stringFromJNI();
}
