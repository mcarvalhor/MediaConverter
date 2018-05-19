package com.mcarvalhor.apps.mediaconverter;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.FormatFlagsConversionMismatchException;

/**
 * Utils disponibiliza diversos métodos e classes para serem usados no aplicativo.
 * Created by mcarvalhor on 12/31/17.
 */


public class Utils {

	/**
	 * Esta classe fornece métodos sob tarefas e lista de tarefas.
	 */
	public static class MediaTask {

		private static final int FileVersion = 1;
		private static final String TAG = "Utils/MediaTask";

		public static final int
				TYPE_INVALID = 1,
				TYPE_CONVERSION = 2,
				TYPE_AUDIOEXTRACTION = 4;

		public static final int
				STATUS_DONE = 1,
				STATUS_PROCESSING = 2,
				STATUS_QUEUED = 4;

		public static final int
				RESULTS_UNKNOWN = 0,
				RESULTS_OK = 1,
				RESULTS_CANCELLED = 2,
				RESULTS_INPUTNOTFOUND = 4,
				RESULTS_EXTERNALWRITEERROR = 8,
				RESULTS_LIBRARYERROR = 16;

		public String Id;
		public int Type;
		public int Status;
		public int Results;
		public String Path;
		public String[] Cmd;
		public String[] InputFiles;
		public String[][] OutputFiles;
		public int ServiceIndex;
		public LinearLayout ActivityLayout;
		public TextView ActivityInformation;
		public ProgressBar ActivityProgressBar;

		public MediaTask(String TaskId, int TaskType, String[] TaskCmd, String[] TaskInput, String[][] TaskOutput, Context JobContext) {
			this.Id = TaskId;
			this.Type = TaskType;
			this.Status = STATUS_QUEUED;
			this.Results = RESULTS_UNKNOWN;
			this.Path = JobContext.getFilesDir() + "/Inputs/" + TaskId;
			this.Cmd = TaskCmd;
			this.InputFiles = TaskInput;
			this.OutputFiles = TaskOutput;
		}

		public MediaTask(byte[] Obj) {
			/*
			Estrutura: Id, Type, Status, CmdN, Cmd, InputN, [InputStrN, InputStr,] OutputN, [OutputPrivStrN, OutputPrivStr, OutputExtStrN, OutputExtStr,] [ProcessingService, Duration, Progress]
			 */
			ByteArrayInputStream BStream = null;
			byte[] Aux, AuxLen;
			int i;
			try {
				BStream = new ByteArrayInputStream(Obj);
				AuxLen = new byte[Integer.SIZE / 8];
				if (BStream.read(AuxLen) != AuxLen.length) throw new IOException();
				Aux = new byte[ByteBuffer.wrap(AuxLen).getInt()];
				if (BStream.read(Aux) != Aux.length) throw new IOException();
				this.Id = new String(Aux);
				Aux = new byte[Integer.SIZE / 8];
				if (BStream.read(Aux) != Aux.length) throw new IOException();
				this.Type = ByteBuffer.wrap(Aux).getInt();
				Aux = new byte[Integer.SIZE / 8];
				if (BStream.read(Aux) != Aux.length) throw new IOException();
				this.Status = ByteBuffer.wrap(Aux).getInt();
				Aux = new byte[Integer.SIZE / 8];
				if (BStream.read(Aux) != Aux.length) throw new IOException();
				this.Results = ByteBuffer.wrap(Aux).getInt();
				AuxLen = new byte[Integer.SIZE / 8];
				if (BStream.read(AuxLen) != AuxLen.length) throw new IOException();
				Aux = new byte[ByteBuffer.wrap(AuxLen).getInt()];
				if (BStream.read(Aux) != Aux.length) throw new IOException();
				this.Path = new String(Aux);
				Aux = new byte[Integer.SIZE / 8];
				if (BStream.read(Aux) != AuxLen.length) throw new IOException();
				this.Cmd = new String[ByteBuffer.wrap(Aux).getInt()];
				for (i = 0; i < this.Cmd.length; i++) {
					AuxLen = new byte[Integer.SIZE / 8];
					if (BStream.read(AuxLen) != AuxLen.length) throw new IOException();
					Aux = new byte[ByteBuffer.wrap(AuxLen).getInt()];
					if (BStream.read(Aux) != Aux.length) throw new IOException();
					this.Cmd[i] = new String(Aux);
				}
				AuxLen = new byte[Integer.SIZE / 8];
				if (BStream.read(AuxLen) != AuxLen.length) throw new IOException();
				this.InputFiles = new String[ByteBuffer.wrap(AuxLen).getInt()];
				for (i = 0; i < this.InputFiles.length; i++) {
					AuxLen = new byte[Integer.SIZE / 8];
					if (BStream.read(AuxLen) != AuxLen.length) throw new IOException();
					Aux = new byte[ByteBuffer.wrap(AuxLen).getInt()];
					if (BStream.read(Aux) != Aux.length) throw new IOException();
					this.InputFiles[i] = new String(Aux);
				}
				AuxLen = new byte[Integer.SIZE / 8];
				if (BStream.read(AuxLen) != AuxLen.length) throw new IOException();
				this.OutputFiles = new String[ByteBuffer.wrap(AuxLen).getInt()][2];
				for (i = 0; i < this.OutputFiles.length; i++) {
					AuxLen = new byte[Integer.SIZE / 8];
					if (BStream.read(AuxLen) != AuxLen.length) throw new IOException();
					Aux = new byte[ByteBuffer.wrap(AuxLen).getInt()];
					if (BStream.read(Aux) != Aux.length) throw new IOException();
					this.OutputFiles[i][0] = new String(Aux);
					AuxLen = new byte[Integer.SIZE / 8];
					if (BStream.read(AuxLen) != AuxLen.length) throw new IOException();
					Aux = new byte[ByteBuffer.wrap(AuxLen).getInt()];
					if (BStream.read(Aux) != Aux.length) throw new IOException();
					this.OutputFiles[i][1] = new String(Aux);
				}
				Aux = new byte[Integer.SIZE / 8];
				if (BStream.read(Aux) != Aux.length) throw new IOException();
				this.ServiceIndex = ByteBuffer.wrap(Aux).getInt();
				BStream.close();
			} catch (Exception e) {
				try {
					if (BStream != null) BStream.close();
				} catch (Exception ex) {
					Log.e(TAG, "Constructor error: couldn't build MediaTask from byte[]");
				}
				this.Type = TYPE_INVALID;
			}
		}

		public byte[] GetBytes() {
			/*
			Estrutura: Id, Type, Status, CmdN, Cmd, InputN, [InputStrN, InputStr,] OutputN, [OutputPrivStrN, OutputPrivStr, OutputExtStrN, OutputExtStr,] [ProcessingService, Duration, Progress]
			 */
			ByteArrayOutputStream BStream = null;
			byte[] Aux, AuxStr;
			int i;
			try {
				BStream = new ByteArrayOutputStream();
				Aux = this.Id.getBytes();
				BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(Aux.length).array());
				BStream.write(Aux);
				BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(this.Type).array());
				BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(this.Status).array());
				BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(this.Results).array());
				Aux = this.Path.getBytes();
				BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(Aux.length).array());
				BStream.write(Aux);
				BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(this.Cmd.length).array());
				for (i = 0; i < this.Cmd.length; i++) {
					Aux = this.Cmd[i].getBytes();
					BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(Aux.length).array());
					BStream.write(Aux);
				}
				BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(this.InputFiles.length).array());
				for (i = 0; i < this.InputFiles.length; i++) {
					AuxStr = this.InputFiles[i].getBytes();
					BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(AuxStr.length).array());
					BStream.write(AuxStr);
				}
				BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(this.OutputFiles.length).array());
				for (i = 0; i < this.OutputFiles.length; i++) {
					AuxStr = this.OutputFiles[i][0].getBytes();
					BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(AuxStr.length).array());
					BStream.write(AuxStr);
					AuxStr = this.OutputFiles[i][1].getBytes();
					BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(AuxStr.length).array());
					BStream.write(AuxStr);
				}
				BStream.write(ByteBuffer.allocate(Integer.SIZE / 8).putInt(this.ServiceIndex).array());
				Aux = BStream.toByteArray();
				BStream.close();
			} catch (Exception e) {
				try {
					if (BStream != null) BStream.close();
				} catch (Exception ex) {
					Log.e(TAG, "Constructor error: couldn't build MediaTask from ByteArray.");
				}
				Aux = new byte[0];
			}
			return Aux;
		}

		/**
		 * Obter uma lista de tarefas.
		 *
		 * @param Obj O conjunto de bytes que representa essa lista de tarefas.
		 * @return Retorna um array com as tarefas em sua ordenação natural.
		 */
		public static MediaTask[] GetArray(byte[] Obj) {
			ByteArrayInputStream BStream = null;
			byte[] AuxByt, AuxLen;
			MediaTask[] Aux;
			int i;
			try {
				BStream = new ByteArrayInputStream(Obj);
				AuxByt = new byte[Integer.SIZE / 8];
				if (BStream.read(AuxByt) != AuxByt.length) throw new IOException();
				Aux = new MediaTask[ByteBuffer.wrap(AuxByt).getInt()];
				for (i = 0; i < Aux.length; i++) {
					AuxLen = new byte[Integer.SIZE / 8];
					if (BStream.read(AuxLen) != AuxLen.length) throw new IOException();
					AuxByt = new byte[ByteBuffer.wrap(AuxLen).getInt()];
					if (BStream.read(AuxByt) != AuxByt.length) throw new IOException();
					Aux[i] = new MediaTask(AuxByt);
				}
				BStream.close();
			} catch (Exception e) {
				try {
					if (BStream != null) BStream.close();
				} catch (Exception eBS) {
					Log.e(TAG, "GetArray: couldn't build MediaTask[] from ByteArray.");
				}
				Aux = new MediaTask[0];
			}
			return Aux;
		}

		/**
		 * Obter uma lista de tarefas a partir dos dados do usuário.
		 *
		 * @param AppContext O "Android Context", usado para ler o arquivo corretamente.
		 * @return Retorna um array com as tarefas em sua ordenação natural.
		 */
		public static MediaTask[] GetArray(Context AppContext) {
			return GetArray(ReadList(AppContext));
		}

		public static ArrayList<MediaTask> GetArrayList(byte[] Obj) {
			ArrayList<MediaTask> AuxList = new ArrayList<>();
			AuxList.addAll(Arrays.asList(GetArray(Obj)));
			return AuxList;
		}

		public ArrayList<MediaTask> GetArrayList(Context AppContext) {
			return GetArrayList(ReadList(AppContext));
		}

		/**
		 * Obter um conjunto de bytes que representa determinada lista de tarefas.
		 *
		 * @param List A lista desejada.
		 * @return Retorna um ByteArray que representa a lista. Pode ser salvo ou transferido entre serviços.
		 */
		public static byte[] BytesFrom(MediaTask[] List) {
			ByteArrayOutputStream BStream = null;
			byte[] Aux, AuxByt, AuxLen;
			int i;
			try {
				BStream = new ByteArrayOutputStream();
				AuxByt = ByteBuffer.allocate(Integer.SIZE / 8).putInt(List.length).array();
				BStream.write(AuxByt);
				for (i = 0; i < List.length; i++) {
					AuxByt = List[i].GetBytes();
					AuxLen = ByteBuffer.allocate(Integer.SIZE / 8).putInt(AuxByt.length).array();
					BStream.write(AuxLen);
					BStream.write(AuxByt);
				}
				Aux = BStream.toByteArray();
				BStream.close();
			} catch (Exception e) {
				try {
					if (BStream != null) BStream.close();
				} catch (Exception eBS) {
					Log.e(TAG, "BytesFrom: couldn't build ByteArray from MediaTask[].");
				}
				Aux = new byte[0];
			}
			return Aux;
		}

		public static byte[] BytesFrom(ArrayList<MediaTask> List) {
			MediaTask[] Aux = new MediaTask[List.size()];
			int i;
			for (i = 0; i < Aux.length; i++) {
				Aux[i] = List.get(i);
			}
			return BytesFrom(Aux);
		}

		public static byte[] BytesFrom(ArrayList<MediaTask> DoneList, ArrayList<MediaTask> ProcessingList, ArrayList<MediaTask> QueuedList) {
			MediaTask[] Aux = new MediaTask[DoneList.size() + ProcessingList.size() + QueuedList.size()];
			int i, n, counter;
			for (counter = 0, i = 0, n = DoneList.size(); i < n; i++) {
				Aux[counter++] = DoneList.get(i);
			}
			for (i = 0, n = ProcessingList.size(); i < n; i++) {
				Aux[counter++] = ProcessingList.get(i);
			}
			for (i = 0, n = QueuedList.size(); i < n; i++) {
				Aux[counter++] = QueuedList.get(i);
			}
			return BytesFrom(Aux);
		}

		/**
		 * Salva nos dados do usuário uma lista de tarefas descrita a partir de bytes.
		 *
		 * @param Obj        O conjunto de bytes que representa a lista.
		 * @param AppContext O "Android Context", usado para escrever o arquivo corretamente.
		 * @return Retorna 'true' para sucesso na escrita, ou 'false' caso algum erro tenha ocorrido.
		 */
		public static boolean SetList(byte[] Obj, Context AppContext) {
			return WriteList(Obj, AppContext);
		}

		/**
		 * Salva nos dados do usuário uma lista de tarefas.
		 *
		 * @param NewList    A lista que será salva.
		 * @param AppContext O "Android Context", usado para escrever o arquivo corretamente.
		 * @return Retorna 'true' para sucesso na escrita, ou 'false' caso algum erro tenha ocorrido.
		 */
		public static boolean SetList(MediaTask[] NewList, Context AppContext) {
			return WriteList(BytesFrom(NewList), AppContext);
		}

		public static boolean SetList(ArrayList<MediaTask> NewList, Context AppContext) {
			return SetList(BytesFrom(NewList), AppContext);
		}

		public static boolean SetList(ArrayList<MediaTask> NewDoneList, ArrayList<MediaTask> NewProcessingList, ArrayList<MediaTask> NewQueuedList, Context AppContext) {
			return SetList(BytesFrom(NewDoneList, NewProcessingList, NewQueuedList), AppContext);
		}

		/**
		 * Este função faz a leitura de uma lista nos dados do usuário.
		 *
		 * @param AppContext O "Android Context", usado para ler o arquivo corretamente.
		 * @return Retorna um ByteArray que representa a lista.
		 */
		private static byte[] ReadList(Context AppContext) {
			File TaskListFile = new File(AppContext.getFilesDir() + "/joblist");
			byte[] Aux = new byte[0];
			if (!TaskListFile.exists()) return Aux;
			if (!TaskListFile.isFile() || TaskListFile.length() > 1024 * 1024 * 10) { // 10MiB
				DeleteFile(TaskListFile);
				return Aux;
			}
			FileInputStream FStream = null;
			try {
				FStream = new FileInputStream(TaskListFile);
				byte[] AuxByt;
				int AuxLen;
				AuxByt = new byte[Integer.SIZE / 8];
				if (FStream.read(AuxByt) != AuxByt.length) throw new IOException();
				if (ByteBuffer.wrap(AuxByt).getInt() != FileVersion)
					throw new IOException(); // Versões de arquivo diferentes.
				AuxByt = new byte[1024 * 1024 * 10]; // 10MiB
				AuxLen = FStream.read(AuxByt);
				Aux = Arrays.copyOf(AuxByt, AuxLen);
				FStream.close();
			} catch (Exception e) {
				try {
					if (FStream != null) FStream.close();
				} catch (Exception eFS) {
					Log.e(TAG, "ReadList: file reading error.");
				}
				DeleteFile(TaskListFile);
				Aux = new byte[0];
			}
			return Aux;
		}

		/**
		 * Este função faz a escrita de uma lista nos dados do usuário.
		 *
		 * @param NewList    A lista, representada em bytes.
		 * @param AppContext O "Android Context", usado para escrever o arquivo corretamente.
		 * @return Retorna 'true' para sucesso na escrita, ou 'false' caso algum erro tenha ocorrido.
		 */
		private static boolean WriteList(byte[] NewList, Context AppContext) {
			File TaskListFile = new File(AppContext.getFilesDir() + "/joblist");
			if (NewList == null) return false;
			if (NewList.length < 1) {
				if (TaskListFile.exists()) DeleteFile(TaskListFile);
				return true;
			}
			if (TaskListFile.exists() && !TaskListFile.isFile()) DeleteFile(TaskListFile);
			FileOutputStream FStream = null;
			try {
				FStream = new FileOutputStream(TaskListFile, false);
				byte[] AuxByt;
				AuxByt = ByteBuffer.allocate(Integer.SIZE / 8).putInt(FileVersion).array();
				FStream.write(AuxByt);
				AuxByt = ByteBuffer.allocate(Integer.SIZE / 8).putInt(NewList.length).array();
				FStream.write(AuxByt);
				FStream.write(NewList);
				FStream.close();
			} catch (Exception e) {
				try {
					if (FStream != null) FStream.close();
				} catch (Exception eFS) {
					Log.e(TAG, "WriteList: file writing error.");
				}
				DeleteFile(TaskListFile);
				return false;
			}
			return true;
		}

	}



	/*public static class Settings {

		public static class Pair {
			String Key;
			Object Value, Default;
		}

		static Pair[] S = new Pair[0];

		public static Object Get(String Key){
			return null;
		}

		public static String GetString(String Key){
			return Get(Key).toString();
		}

		public static boolean GetBool(String Key){
			return (boolean) Get(Key);
		}

		public static int GetInt(String Key){
			return (int) Get(Key);
		}

		public static long GetLong(String Key){
			return (long) Get(Key);
		}

		public static float GetFloat(String Key){
			return (float) Get(Key);
		}

		public static double GetDouble(String Key){
			return (double) Get(Key);
		}

		public static void Set(String Key, Object Value){
			;
		}

		public static void Load(){
			;
		}

		public static void Save(){
			;
		}

		public static void Reset(){
			;
		}

	}*/


	public static abstract class TaskService extends Service {
		public Messenger MessengerService = null;
		public ServiceConnection MessengerConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
				MessengerService = new Messenger(iBinder);
			}

			@Override
			public void onServiceDisconnected(ComponentName componentName) {
				MessengerService = null;
				Log.e(TAG, "ProcessService: Service unbound.");
				stopSelf();
			}
		};

		@Override
		public IBinder onBind(Intent intent) {
			throw new UnsupportedOperationException("Not implemented");
		}

		private boolean Started = false;
		public FFmpeg MediaLib = null;
		private MediaTask Task = null;
		private int TaskIndex = 0;
		private double Duration = -1, Completed = -1;
		public int ServiceIndex = -1;
		private final String
				DurationPattern = "\\s*Duration\\s*:\\s*([0-9]+)\\s*:\\s*([0-9]+)\\s*:\\s*([0-9]+\\.[0-9]+|[0-9]+).*",
				CompletedPattern = ".*time\\s*=\\s*([0-9]+)\\s*:\\s*([0-9]+)\\s*:\\s*([0-9]+\\.[0-9]+|[0-9]+).*";
		public static final int
				INIT_START = 1,
				INIT_STOP = 2,
				INIT_NONE = 0;

		private static final String TAG = "TaskService";

		public void Next(){
			if(TaskIndex < 1){
				try {
					MediaLib.execute(Task.Cmd, new ExecuteBinaryResponseHandler() {
						@Override
						public void onStart() {
							//Log.i(TAG, "FFmpeg: Start");
							if (MessengerService == null) return;
							try {
								Message Msg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_START);
								Bundle MsgBundle = new Bundle();
								MsgBundle.putString("task_id", Task.Id);
								MsgBundle.putInt("service_index", ServiceIndex);
								Msg.setData(MsgBundle);
								MessengerService.send(Msg);
							} catch (Exception e) {
								Log.w(TAG, "FFMPEG: Could not send Start message.");
							}
						}

						@Override
						public void onProgress(String Line) {
							//Log.i(TAG, "FFmpeg: Progress > " + Line);
							double Aux;
							if(Line.matches(DurationPattern)){
								Aux = Double.parseDouble(Line.replaceAll(DurationPattern, "$1")) * 60 * 60;
								Aux += Double.parseDouble(Line.replaceAll(DurationPattern, "$2")) * 60;
								Aux += Double.parseDouble(Line.replaceAll(DurationPattern, "$3"));
								if(Duration < 0 || Duration < Aux) {
									Duration = Aux;
								}
							}else if(Line.matches(CompletedPattern)){
								Aux = Double.parseDouble(Line.replaceAll(CompletedPattern, "$1")) * 60 * 60;
								Aux += Double.parseDouble(Line.replaceAll(CompletedPattern, "$2")) * 60;
								Aux += Double.parseDouble(Line.replaceAll(CompletedPattern, "$3"));
								if(Completed < 0 || Completed < Aux) {
									Completed = Aux;
								}
							}
							if(Duration < 0 || Completed < 0)	return;
							if (MessengerService == null) return;
							try {
								Message Msg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_PROGRESS);
								Bundle MsgBundle = new Bundle();
								MsgBundle.putString("task_id", Task.Id);
								MsgBundle.putInt("service_index", ServiceIndex);
								MsgBundle.putDouble("task_completed", Completed/Duration); // REGEX TIME
								MsgBundle.putDouble("task_total", 1);
								Msg.setData(MsgBundle);
								MessengerService.send(Msg);
							} catch (Exception e) {
								Log.w(TAG, "FFMPEG: Could not send Progress message.");
							}
						}

						@Override
						public void onFailure(String Response) {
							/*Log.w(TAG, "FFmpeg: Processing Failure");
							Log.w(TAG, Response);*/
							if (MessengerService == null) return;
							try {
								Message Msg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_FAILURE);
								Bundle MsgBundle = new Bundle();
								MsgBundle.putString("task_id", Task.Id);
								MsgBundle.putInt("service_index", ServiceIndex);
								MsgBundle.putInt("task_results", Task.Results = MediaTask.RESULTS_LIBRARYERROR);
								Msg.setData(MsgBundle);
								MessengerService.send(Msg);
							} catch (Exception e) {
								Log.w(TAG, "FFMPEG: Could not send Failure message.");
							}
						}

						@Override
						public void onSuccess(String Response) {
							/*Log.i(TAG, "FFmpeg: Processing success");
							Log.i(TAG, Response);*/
							if (MessengerService == null) return;
							try {
								Message Msg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_SUCCESS);
								Bundle MsgBundle = new Bundle();
								MsgBundle.putString("task_id", Task.Id);
								MsgBundle.putInt("service_index", ServiceIndex);
								Msg.setData(MsgBundle);
								MessengerService.send(Msg);
							} catch (Exception e) {
								Log.w(TAG, "FFMPEG: Could not send Success message.");
							}
						}

						@Override
						public void onFinish() {
							//Log.i(TAG, "FFmpeg: Processing Finished");
							if(Task.Results != MediaTask.RESULTS_OK) {
								if (MessengerService != null) {
									try {
										Message Msg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_FINISH);
										Bundle MsgBundle = new Bundle();
										MsgBundle.putString("task_id", Task.Id);
										MsgBundle.putInt("service_index", ServiceIndex);
										MsgBundle.putInt("task_results", Task.Results);
										Msg.setData(MsgBundle);
										MessengerService.send(Msg);
									} catch (Exception e) {
										Log.w(TAG, "FFMPEG: Could not send Finish message.");
									}
								}
								stopSelf();
							}else {
								Next();
							}
						}
					});
				} catch (Exception e) {
					Log.e(TAG, "FFMPEG: May already be running on this process.");
					stopSelf();
				}
				TaskIndex++;
			}else{
				if (MessengerService != null) {
					try {
						Message Msg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_FINISH);
						Bundle MsgBundle = new Bundle();
						MsgBundle.putString("task_id", Task.Id);
						MsgBundle.putInt("service_index", ServiceIndex);
						MsgBundle.putInt("task_results", MediaTask.RESULTS_OK);
						Msg.setData(MsgBundle);
						MessengerService.send(Msg);
					} catch (Exception e) {
						Log.w(TAG, "Could not send FinishOk message.");
					}
				}
				stopSelf();
			}
		}

		public void StopAll(){
			MediaLib.killRunningProcesses();
			if (MessengerService != null) {
				try {
					Message Msg = Message.obtain(null, TaskProcess.MessengerHandler.MSG_FINISH);
					Bundle MsgBundle = new Bundle();
					MsgBundle.putString("task_id", Task.Id);
					MsgBundle.putInt("service_index", ServiceIndex);
					MsgBundle.putInt("task_results", MediaTask.RESULTS_CANCELLED);
					Msg.setData(MsgBundle);
					MessengerService.send(Msg);
				} catch (Exception e) {
					Log.w(TAG, "Could not send FinishCancel Msg.");
				}
			}
			stopSelf();
		}

		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			if (Started){
				if(intent == null)	return START_NOT_STICKY;
				if(intent.getIntExtra("init_action", INIT_NONE) == INIT_STOP)	StopAll();
				return START_NOT_STICKY;
			}
			if (intent == null) {
				Log.e(TAG, "ProcessService startCommand: 'intent' is null.");
				stopSelf();
				return START_NOT_STICKY;
			}
			Task = new MediaTask(intent.getByteArrayExtra("init_task"));
			if (Task.Type == MediaTask.TYPE_INVALID) {
				Log.e(TAG, "ProcessService: 'Task' is not valid.");
				stopSelf();
				return START_NOT_STICKY;
			}
			Started = true;
			try {
				MediaLib = FFmpeg.getInstance(this);
				MediaLib.loadBinary(new LoadBinaryResponseHandler() {
					@Override
					public void onStart() {
						Log.i(TAG, "FFmpeg: Started binary loading.");
					}
					@Override
					public void onFailure() {
						Log.e(TAG, "FFmpeg: Couldn't load binaries.");
						stopSelf();
					}
					@Override
					public void onSuccess() {
						Log.i(TAG, "FFmpeg: Loaded binaries.");
						Task.Results = MediaTask.RESULTS_OK;
						Next();
					}
					@Override
					public void onFinish() {
						Log.i(TAG, "FFmpeg: Binaries loading done.");
					}
				});
			} catch (Exception e) {
				Log.e(TAG, "FFMPEG: May not be supported by the device.");
				stopSelf();
				return START_NOT_STICKY;
			}
			if (!bindService(new Intent(getApplicationContext(), TaskProcess.class), MessengerConnection, Context.BIND_AUTO_CREATE)) {
				Log.e(TAG, "ProcessService: Can't bind service.");
				stopSelf();
				return START_NOT_STICKY;
			}
			return START_NOT_STICKY;
		}

		@Override
		public void onDestroy() { // ATENÇÃO: não é garantido que este método será chamado.
			MediaLib.killRunningProcesses();
			unbindService(MessengerConnection);
			//super.onDestroy();
		}
	}

	/**
	 * Remove um arquivo ou diretório recursivamente.
	 *
	 * @param Obj O arquivo ou diretório que será removido.
	 */
	public static void DeleteFile(File Obj) {
		if (!Obj.exists()) return;
		if (Obj.isDirectory()) {
			File[] Contents = Obj.listFiles();
			for (File Content : Contents) {
				DeleteFile(Content);
			}
		}
		Obj.delete();
	}

	/**
	 * Remove o conteúdo de um diretório.
	 *
	 * @param Obj O diretório que será limpo. Após o término, ele será um diretório vazio.
	 */
	public static void DeleteFileContents(File Obj) {
		if (!Obj.exists()) return;
		if (Obj.isFile()) return;
		File[] Contents = Obj.listFiles();
		for (File Content : Contents) {
			DeleteFile(Content);
		}
	}

	/**
	 * Chame esta função em caso de erros irrecuperáveis.
	 * CUIDADO: Ela remove todos os dados do aplicativo e encerra/reinicia este.
	 *
	 * @param AppContext O "Android Context" que será usado para limpar os dados do aplicativo, ou "null" caso não tenha este.
	 */
	public static void FatalError(String TAG, Context AppContext) {
		Log.e(TAG, " *** FATAL ERROR ***");
		if (AppContext == null) {
			Log.e(TAG, "FATAL ERROR: AppContext is null.");
		} else {
			try {
				DeleteFileContents(AppContext.getFilesDir());
				DeleteFileContents(AppContext.getCacheDir());
			} catch (Exception e) {
				Log.e(TAG, "FATAL ERROR: Could not Delete File Contents.");
			}
		}
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(0);
	}

}
