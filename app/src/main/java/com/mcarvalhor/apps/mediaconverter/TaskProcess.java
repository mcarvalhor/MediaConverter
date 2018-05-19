package com.mcarvalhor.apps.mediaconverter;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

public class TaskProcess extends Service {
	public TaskProcess() {
	}

	public class MessengerHandler extends Handler {
		public static final int
				MSG_FORCEUPDATE = 1,
				MSG_UPDATELIST = 2,
				MSG_ADDTASK = 4,
				MSG_SWAPTASKUP = 8,
				MSG_SWAPTASKDOWN = 16,
				MSG_REMOVETASK = 32,
				MSG_REMOVEALLDONE = 64,
				MSG_START = 128,
				MSG_PROGRESS = 256,
				MSG_FAILURE = 512,
				MSG_SUCCESS = 1024,
				MSG_FINISH = 2048;

		private Messenger ActivityMessenger = null;
		private static final String TAG = "TaskProcess/MHandler";

		@Override
		public void handleMessage(Message msg) {
			Bundle MsgBundle, AuxData = msg.getData();
			double AuxCompleted, AuxTotal;
			int AuxIndex, AuxCount, AuxN;
			Utils.MediaTask AuxTask;
			String AuxId;
			Message Msg;
			if(msg.replyTo != null)
				ActivityMessenger = msg.replyTo;
			switch (msg.what) {
				case MSG_FORCEUPDATE:
					break;
				case MSG_UPDATELIST:
					try {
						Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_LISTUPDATE);
						MsgBundle = new Bundle();
						MsgBundle.putByteArray("new_list", Utils.MediaTask.BytesFrom(DoneList, ProcessingList, QueuedList));
						Msg.setData(MsgBundle);
						ActivityMessenger.send(Msg);
					} catch (Exception e){
						Log.i(TAG, "Could not reply UpdateList message.");
					}
					break;
				case MSG_ADDTASK:
					AuxTask = new Utils.MediaTask(AuxData.getByteArray("task"));
					if(AuxTask.Type != Utils.MediaTask.TYPE_INVALID){
						QueuedList.add(AuxTask);
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_ITEMADDED);
							MsgBundle = new Bundle();
							MsgBundle.putByteArray("new_task", AuxTask.GetBytes());
							Msg.setData(MsgBundle);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(TAG, "Could not reply Add message.");
						}
						UpdateTasks();
						SaveList();
					}else{
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_UNLOCKUI);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(TAG, "Could not reply UnlockUI message.");
						}
					}
					break;
				case MSG_SWAPTASKUP:
					AuxIndex = AuxData.getInt("task_index");
					AuxId = AuxData.getString("task_id");
					if(SwapItemUp(AuxIndex, AuxId)) {
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_ITEMSWAPUP);
							MsgBundle = new Bundle();
							MsgBundle.putInt("task_index", AuxIndex);
							MsgBundle.putString("task_id", AuxId);
							Msg.setData(MsgBundle);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(TAG, "Could not reply SwapUp message.");
						}
					}else{
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_UNLOCKUI);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(TAG, "Could not reply UnlockUI message.");
						}
					}
					break;
				case MSG_SWAPTASKDOWN:
					AuxIndex = AuxData.getInt("task_index");
					AuxId = AuxData.getString("task_id");
					if(SwapItemDown(AuxIndex, AuxId)) {
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_ITEMSWAPDOWN);
							MsgBundle = new Bundle();
							MsgBundle.putInt("task_index", AuxIndex);
							MsgBundle.putString("task_id", AuxId);
							Msg.setData(MsgBundle);
							ActivityMessenger.send(Msg);
						} catch (Exception e) {
							Log.i(TAG, "Could not reply SwapDown message.");
						}
					}else{
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_UNLOCKUI);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(TAG, "Could not reply UnlockUI message.");
						}
					}
					break;
				case MSG_REMOVETASK:
					AuxIndex = AuxData.getInt("task_index");
					AuxId = AuxData.getString("task_id");
					if(RemoveItem(AuxIndex, AuxId)) {
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_ITEMREMOVED);
							MsgBundle = new Bundle();
							MsgBundle.putInt("task_index", AuxIndex);
							MsgBundle.putString("task_id", AuxId);
							Msg.setData(MsgBundle);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(ContentValues.TAG, "Could not reply Remove message.");
						}
					}else{
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_UNLOCKUI);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(TAG, "Could not reply UnlockUI message.");
						}
					}
					break;
				case MSG_REMOVEALLDONE:
					AuxN = DoneList.size();
					DoneList.clear();
					SaveList();
					try {
						Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_DONEREMOVED);
						MsgBundle = new Bundle();
						MsgBundle.putInt("total_items", AuxN);
						Msg.setData(MsgBundle);
						ActivityMessenger.send(Msg);
					} catch (Exception e){
						Log.i(TAG, "Could not reply DoneRemoved message.");
					}
					break;
				case MSG_START:
					AuxIndex = AuxData.getInt("service_index");
					AuxId = AuxData.getString("task_id");
					for(AuxCount = 0, AuxN = ProcessingList.size(); AuxCount < AuxN; AuxCount++){
						if(ProcessingList.get(AuxCount).Id.equals(AuxId))	break;
					}
					if(AuxCount < AuxN && AuxIndex >= 0 && AuxIndex < ServiceList.length
							&& (AuxTask=ServiceList[AuxIndex].TRef).Id.equals(AuxId)) {
						AuxTask.Status = Utils.MediaTask.STATUS_PROCESSING;
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_STARTED);
							MsgBundle = new Bundle();
							MsgBundle.putString("task_id", AuxId);
							MsgBundle.putInt("task_index", AuxCount + DoneList.size());
							Msg.setData(MsgBundle);
							ActivityMessenger.send(Msg);
						} catch (Exception e) {
							Log.i(TAG, "Could not reply Start message.");
						}
					}else{
						Log.w(TAG, "Start not ok: loading list again...");
						LoadList();
					}
					break;
				case MSG_PROGRESS:
					AuxIndex = AuxData.getInt("service_index");
					AuxId = AuxData.getString("task_id");
					for(AuxCount = 0, AuxN = ProcessingList.size(); AuxCount < AuxN; AuxCount++){
						if(ProcessingList.get(AuxCount).Id.equals(AuxId))	break;
					}
					if(AuxCount < AuxN && AuxIndex >= 0 && AuxIndex < ServiceList.length
							&& (AuxTask=ServiceList[AuxIndex].TRef).Id.equals(AuxId)){
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_PROGRESS);
							MsgBundle = new Bundle();
							MsgBundle.putString("task_id", AuxId);
							MsgBundle.putInt("task_index", AuxCount + DoneList.size());
							MsgBundle.putDouble("task_completed", AuxData.getDouble("task_completed"));
							MsgBundle.putDouble("task_total", AuxData.getDouble("task_total"));
							Msg.setData(MsgBundle);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(TAG, "Could not reply Progress message.");
						}
					}else{
						Log.w(TAG, "Progress not ok: loading list again...");
						LoadList();
					}
					break;
				case MSG_FAILURE:
					/*AuxIndex = AuxData.getInt("service_index");
					AuxId = AuxData.getString("task_id");
					for(AuxCount = 0, AuxN = ProcessingList.size(); AuxCount < AuxN; AuxCount++){
						if(ProcessingList.get(AuxCount).Id.equals(AuxId))	break;
					}
					if(AuxCount < AuxN && AuxIndex >= 0 && AuxIndex < ServiceList.length
							&& (AuxTask=ServiceList[AuxIndex].TRef).Id.equals(AuxId)){
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_FINISHED);
							MsgBundle = new Bundle();
							MsgBundle.putString("task_id", AuxId);
							MsgBundle.putInt("task_index", AuxCount + DoneList.size());
							MsgBundle.putInt("task_results", AuxTask.Results=AuxData.getInt("task_results"));
							Msg.setData(MsgBundle);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(TAG, "Could not reply Failure message.");
						}
					}else{
						LoadList();
					}*/
					break;
				case MSG_SUCCESS:
					/*AuxIndex = AuxData.getInt("service_index");
					AuxId = AuxData.getString("task_id");
					for(AuxCount = 0, AuxN = ProcessingList.size(); AuxCount < AuxN; AuxCount++){
						if(ProcessingList.get(AuxCount).Id.equals(AuxId))	break;
					}
					if(AuxCount < AuxN && AuxIndex >= 0 && AuxIndex < ServiceList.length
							&& (AuxTask=ServiceList[AuxIndex].TRef).Id.equals(AuxId)){
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_FINISHED);
							MsgBundle = new Bundle();
							MsgBundle.putString("task_id", AuxId);
							MsgBundle.putInt("task_index", AuxCount + DoneList.size());
							MsgBundle.putInt("task_results", AuxTask.Results=Utils.MediaTask.RESULTS_OK);
							Msg.setData(MsgBundle);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(TAG, "Could not reply Success message.");
						}
					}else{
						LoadList();
					}*/
					break;
				case MSG_FINISH:
					AuxIndex = AuxData.getInt("service_index");
					AuxId = AuxData.getString("task_id");
					for(AuxCount = 0, AuxN = ProcessingList.size(); AuxCount < AuxN; AuxCount++){
						if(ProcessingList.get(AuxCount).Id.equals(AuxId))	break;
					}
					if(AuxCount < AuxN && AuxIndex >= 0 && AuxIndex < ServiceList.length
							&& (AuxTask=ServiceList[AuxIndex].TRef).Id.equals(AuxId)){
						try {
							Msg = Message.obtain(null, MainActivity.MessengerHandler.MSG_FINISHED);
							MsgBundle = new Bundle();
							MsgBundle.putString("task_id", AuxId);
							MsgBundle.putInt("task_index", AuxCount + DoneList.size());
							MsgBundle.putInt("task_results", AuxTask.Results = AuxData.getInt("task_results"));
							Msg.setData(MsgBundle);
							ActivityMessenger.send(Msg);
						} catch (Exception e){
							Log.i(TAG, "Could not reply Finish message.");
						}
						AuxTask.Status = Utils.MediaTask.STATUS_DONE;
						DoneList.add(ProcessingList.remove(AuxCount));
						ServiceList[AuxIndex].TRef = null;
						RunningServices--;
						UpdateTasks();
						SaveList();
					}else{
						Log.w(TAG, "Finish not ok: re-loading list...");
						LoadList();
					}
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	public class LocalBinder extends Binder {
		TaskProcess getService() {
			return TaskProcess.this;
		}
	}

	public final Messenger ServiceMessenger = new Messenger(new MessengerHandler());
	public final IBinder ServiceBinder = new LocalBinder();
	private static final String TAG = "TaskProcess";

	@Override
	public IBinder onBind(Intent intent) {
		if(intent != null && intent.getBooleanExtra("local", false))	return ServiceBinder;
		return ServiceMessenger.getBinder();
	}

	public class ServiceLink {
		public Class<? extends Service> SClass;
		public Intent SIntent;
		public Utils.MediaTask TRef = null;
		public ServiceLink(Class<? extends Service> SvCl){
			this.SClass = SvCl;
			this.SIntent = new Intent(TaskProcess.this, SvCl);
		}
	}

	private boolean Started = false;
	private ServiceLink[] ServiceList = {
			new ServiceLink(TaskService_0.class),
			new ServiceLink(TaskService_1.class),
			new ServiceLink(TaskService_2.class),
			new ServiceLink(TaskService_3.class),
			new ServiceLink(TaskService_4.class),
			new ServiceLink(TaskService_5.class),
			new ServiceLink(TaskService_6.class),
			new ServiceLink(TaskService_7.class),
			new ServiceLink(TaskService_8.class),
			new ServiceLink(TaskService_9.class),
			new ServiceLink(TaskService_10.class),
			new ServiceLink(TaskService_11.class),
			new ServiceLink(TaskService_12.class),
			new ServiceLink(TaskService_13.class),
			new ServiceLink(TaskService_14.class),
			new ServiceLink(TaskService_15.class)
	};
	private int ParallelServices = 15; // Quantos serviços podem ser executados simultaneamente?
	private int RunningServices = 0;
	private ArrayList<Utils.MediaTask> DoneList = new ArrayList<>();
	private ArrayList<Utils.MediaTask> ProcessingList = new ArrayList<>();
	private ArrayList<Utils.MediaTask> QueuedList = new ArrayList<>();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(Started)	return START_STICKY;
		Started = true;
		LoadList();
		return START_STICKY;
	}

	/**
	 * Salva a lista de tarefas em seu arquivo.
	 */
	public void SaveList() {
		Utils.MediaTask.SetList(DoneList, ProcessingList, QueuedList, TaskProcess.this);
	}

	/**
	 * Faz o recarregamento da lista de tarefas a partir de seu arquivo.
	 */
	public void LoadList(){
		Utils.MediaTask[] Tasks = Utils.MediaTask.GetArray(TaskProcess.this);
		boolean reSave, clearProcessingList;
		int i, n, aux;
		DoneList.clear();
		ProcessingList.clear();
		QueuedList.clear();
		for(i = 0; i < ServiceList.length; i++) {
			ServiceList[i].TRef = null;
		}
		for(i = 0; i < Tasks.length; i++){
			if(Tasks[i].Status == Utils.MediaTask.STATUS_DONE)	DoneList.add(Tasks[i]);
			else if(Tasks[i].Status == Utils.MediaTask.STATUS_PROCESSING)	ProcessingList.add(Tasks[i]);
			else	QueuedList.add(Tasks[i]);
		}
		for(reSave = false, clearProcessingList = false, i = ProcessingList.size() - 1; i >= 0; i--){
			aux = ProcessingList.get(i).ServiceIndex;
			if(aux < 0 || aux >= ServiceList.length){
				QueuedList.add(0, ProcessingList.remove(i));
				QueuedList.get(0).ServiceIndex = -1;
				reSave = true;
			}else if(ServiceList[aux].TRef == null) {
				ServiceList[aux].TRef = ProcessingList.get(i);
			}else{
				clearProcessingList = true;
				break;
			}
		}
		if(clearProcessingList){
			for(i = 0, n = ProcessingList.size(); i < n; i++){
				ProcessingList.get(i).ServiceIndex = -1;
			}
			QueuedList.addAll(0, ProcessingList);
			ProcessingList.clear();
			reSave = true;
		}
		if(reSave)	SaveList();
		RunningServices = 0;
		for(i = 0; i < ServiceList.length; i++){
			ServiceList[i].SIntent = new Intent(TaskProcess.this, ServiceList[i].SClass);
			if(ServiceList[i].TRef != null){
				ServiceList[i].SIntent.putExtra("init_action", Utils.TaskService.INIT_START);
				ServiceList[i].SIntent.putExtra("init_task", ServiceList[i].TRef.GetBytes());
				startService(ServiceList[i].SIntent);
				RunningServices++;
			}else{
				stopService(ServiceList[i].SIntent);
			}
		}
	}

	public void UpdateTasks(){
		Utils.MediaTask Aux;
		int i;
		if(RunningServices >= ParallelServices)	return;
		for(i = 0; i < ServiceList.length; i++)
			if(ServiceList[i].TRef == null)	break;
		if(i >= ServiceList.length)	return;
		if(QueuedList.size() < 1)	return;
		Aux = QueuedList.remove(0);
		Aux.ServiceIndex = i;
		ProcessingList.add(Aux);
		ServiceList[i].TRef = Aux;
		ServiceList[i].SIntent = new Intent(TaskProcess.this, ServiceList[i].SClass);
		ServiceList[i].SIntent.putExtra("init_action", Utils.TaskService.INIT_START);
		ServiceList[i].SIntent.putExtra("init_task", ServiceList[i].TRef.GetBytes());
		if(startService(ServiceList[i].SIntent) == null)	Log.e(TAG, "startService return is 'null'");
		RunningServices++;
		UpdateTasks();
	}

	public boolean SwapItemUp(int ListIndex, String ItemId){
		int nDone, nProcessing, nQueued;
		Utils.MediaTask Aux;
		nDone = DoneList.size();
		nProcessing = nDone + ProcessingList.size();
		nQueued = nProcessing + QueuedList.size();
		if(ListIndex < nDone && ListIndex > 0) { // Está na lista de tarefas prontas.
			Aux = DoneList.get(ListIndex);
			if(!Aux.Id.equals(ItemId))	return false;
			DoneList.set(ListIndex, DoneList.get(ListIndex - 1));
			DoneList.set(ListIndex - 1, Aux);
		}else if(ListIndex > nProcessing && ListIndex < nQueued){ // Está na lista de tarefas aguardando em fila.
			ListIndex -= nProcessing;
			Aux = QueuedList.get(ListIndex);
			if(!Aux.Id.equals(ItemId))	return false;
			QueuedList.set(ListIndex, QueuedList.get(ListIndex - 1));
			QueuedList.set(ListIndex - 1, Aux);
		}else	return false;
		SaveList();
		return true;
	}

	public boolean SwapItemDown(int ListIndex, String ItemId){
		int nDone, nProcessing, nQueued;
		Utils.MediaTask Aux;
		nDone = DoneList.size();
		nProcessing = nDone + ProcessingList.size();
		nQueued = nProcessing + QueuedList.size();
		if(ListIndex < nDone - 1 && ListIndex >= 0) { // Está na lista de tarefas prontas.
			Aux = DoneList.get(ListIndex);
			if(!Aux.Id.equals(ItemId))	return false;
			DoneList.set(ListIndex, DoneList.get(ListIndex + 1));
			DoneList.set(ListIndex + 1, Aux);
		}else if(ListIndex >= nProcessing && ListIndex < nQueued - 1){ // Está na lista de tarefas aguardando em fila.
			ListIndex -= nProcessing;
			Aux = QueuedList.get(ListIndex);
			if(!Aux.Id.equals(ItemId))	return false;
			QueuedList.set(ListIndex, QueuedList.get(ListIndex + 1));
			QueuedList.set(ListIndex + 1, Aux);
		}else	return false;
		SaveList();
		return true;
	}

	public boolean RemoveItem(int ListIndex, String ItemId){
		int nDone, nProcessing, nQueued;
		Utils.MediaTask Aux;
		nDone = DoneList.size();
		nProcessing = nDone + ProcessingList.size();
		nQueued = nProcessing + QueuedList.size();
		if(ListIndex < nDone && ListIndex >= 0) { // Está na lista de tarefas prontas.
			Aux = DoneList.get(ListIndex);
			if(!Aux.Id.equals(ItemId)) return false;
			DoneList.remove(ListIndex);
		}else if(ListIndex >= nDone && ListIndex < nProcessing){
			ListIndex -= nDone;
			Aux = ProcessingList.get(ListIndex);
			if(!Aux.Id.equals(ItemId) || Aux.ServiceIndex < 0 || Aux.ServiceIndex >= ServiceList.length) return false;
			ServiceList[Aux.ServiceIndex].SIntent.putExtra("init_action", Utils.TaskService.INIT_STOP);
			ServiceList[Aux.ServiceIndex].SIntent.putExtra("init_taskid", ItemId);
			startService(ServiceList[Aux.ServiceIndex].SIntent);
		}else if(ListIndex >= nProcessing && ListIndex < nQueued){ // Está na lista de tarefas aguardando em fila.
			ListIndex -= nProcessing;
			Aux = QueuedList.get(ListIndex);
			if(!Aux.Id.equals(ItemId))	return false;
			DoneList.remove(ListIndex);
		}else	return false;
		SaveList();
		return true;
	}


}
