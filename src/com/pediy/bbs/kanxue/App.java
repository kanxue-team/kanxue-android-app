package com.pediy.bbs.kanxue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.http.cookie.Cookie;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pediy.bbs.kanxue.net.Api;
import com.pediy.bbs.kanxue.net.HttpClientUtil;
import com.pediy.bbs.kanxue.net.HttpClientUtil.NetClientCallback;
import com.pediy.bbs.kanxue.util.CookieStorage;
import com.pediy.bbs.kanxue.util.ObjStorage;

import android.app.Application;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.widget.Toast;

public class App extends Application {
	private static String LOG_FILE_NAME = "kanxue.log";
	public static final String LOGIN_STATE_CHANGE_ACTION = "com.pediy.bbs.kanxue.LOGIN_STATE_CHANGE_ACTION";
	private int m_versionCode = 0;
	private ProgressDialog m_updatePd = null;
	private String m_appSavePath = null;
	private boolean m_bCancel = false;
	private Handler m_handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//关闭ProgressDialog
			if (m_updatePd == null) {
				return;
			}
			
			if (msg.what >= 0 && msg.what != m_updatePd.getMax()) {
				m_updatePd.setProgress(msg.what);
				return;
			}
			
			if (m_appSavePath == null)
				return;
			m_updatePd.dismiss();
			
			if (msg.what < 0)
				return;
			installApk(m_appSavePath);
		}
	};
	
	private Handler m_networkErrHandler = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HttpClientUtil.NET_TIMEOUT:
				Toast.makeText(App.this, R.string.net_timeout, Toast.LENGTH_SHORT).show();
				break;
			case HttpClientUtil.NET_FAILED:
				Toast.makeText(App.this, R.string.net_failed, Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}
		}
		
	};
	
	@Override
	public void onCreate() {
		super.onCreate();
		Api.getInstance().setmCon(this);
		
		PackageInfo pinfo = null;
		try {
			pinfo = getPackageManager().getPackageInfo(App.this.getPackageName(), PackageManager.GET_CONFIGURATIONS);
			m_versionCode = pinfo.versionCode;
		} catch (NameNotFoundException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		
		enableRecordLog();
	}
	
	public Handler getNetworkHandler() {
		return m_networkErrHandler;
	}
	
	
	public void installApk(String path) {
		Uri uri = Uri.fromFile(new File(path)); 
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(uri, "application/vnd.android.package-archive");
		startActivity(intent);
	}
	
	private void enableRecordLog() {
		String path = LOG_FILE_NAME;
		FileOutputStream fos = null;
		try {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				path = Environment.getExternalStorageDirectory() + "/kanxue/" + path;
				fos = new FileOutputStream(path, true);
			} else {
				fos = openFileOutput(path, MODE_APPEND | MODE_PRIVATE);
			}
			System.setErr(new PrintStream(fos));
			fos.close();
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
	}
	
	public void checkUpdate(final Context context) {
		this.checkUpdate(context, null);
	}
	
	public void checkUpdate(final Context context, final onCheckUpdate callback) {
		Api.getInstance().checkUpdate(new NetClientCallback() {

			@Override
			public void execute(final int status, final String response,
					List<Cookie> cookies) {
				
				if (callback != null) {
					callback.networkComplete();
				}
				if (status != HttpClientUtil.NET_SUCCESS) {
					m_networkErrHandler.sendEmptyMessage(status);
					return;
				}
				
				final JSONObject obj = JSON.parseObject(response);
				if (obj.getInteger("version") == m_versionCode) {
					if (callback != null) {
						callback.noUpdate();
					}
					return;
				}
				
				m_handler.post(new Runnable() {

					@Override
					public void run() {
						Builder builder = new Builder(context);
						builder.setTitle("检测到新版本");
						final String versionName = versionCodeToName(obj.getInteger("version"));
						builder.setMessage("版本: " + versionName + "\n大小: " + converToSuitableSize(obj.getInteger("size")) + "\n是否更新?");
						builder.setPositiveButton("确定", new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								m_updatePd = createUpdateDialog(context);
								Thread updateThread = new Thread(new Runnable() {

									@Override
									public void run() {
										URL url = null;
										HttpURLConnection con = null;
										String appSaveName = "kanxue_" + versionName + ".apk";
										FileOutputStream fos = null;
										
										try {
											url = new URL(obj.getString("url"));
											con = (HttpURLConnection)url.openConnection();
										
											int filelength = con.getContentLength();
											
											
											if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
												String Path = Environment.getExternalStorageDirectory() + "/kanxue/tmp";
												File file = new File(Path);
												if (!file.exists()) {
													file.mkdirs();
												}
												m_appSavePath = Path + "/" + appSaveName;
												file = new File(m_appSavePath);
												if (file.exists()) {
													file.delete();
												}
												
												file.createNewFile();
												fos = new FileOutputStream(file);
											} else {
												fos = openFileOutput(appSaveName, MODE_PRIVATE);
												m_appSavePath = App.this.getFilesDir() + "/" + appSaveName;
											}
											
											if(filelength>0){
												m_updatePd.setMax(filelength);
												InputStream input=con.getInputStream();  
												//读取大文件  
												byte[] buffer=new byte[4*1024];
												int len = input.read(buffer);
												int downlength = 0;
												while(len != -1 ){
													fos.write(buffer,0,len);
													downlength += len;
													len = input.read(buffer);
													if (m_bCancel) {
														m_handler.sendEmptyMessage(-1);
														break;
													}else {
														m_handler.sendEmptyMessage(downlength);
													}
												}
												fos.flush();
											}
										} catch (MalformedURLException e) {
											// TODO 自动生成的 catch 块
											e.printStackTrace();
										} catch (FileNotFoundException e) {
											// TODO 自动生成的 catch 块
											e.printStackTrace();
										} catch (IOException e) {
											// TODO 自动生成的 catch 块
											e.printStackTrace();
										} finally {
											if (fos != null) {
												try {
													fos.close();
												} catch (IOException e) {
													// TODO 自动生成的 catch 块
													e.printStackTrace();
												}
											}
											
											if (con != null) {
												con.disconnect();
											}
										}
									}
									
								});
								m_updatePd.show();
								updateThread.start();
							}
							
						}).setNegativeButton("取消", null);
						builder.create().show();
						
					}
					
				});
			}
		});
	}
	
	private ProgressDialog createUpdateDialog(Context context) {
		ProgressDialog updatePd = new ProgressDialog(context);	
		updatePd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		updatePd.setTitle("提示");
		updatePd.setMessage("正在更新……");
		updatePd.setIndeterminate(false);
		updatePd.setCancelable(true);
		updatePd.setButton("取消", new OnClickListener() {

			@Override
			public void onClick(
					DialogInterface dialog,
					int which) {
				m_bCancel = false;			//结束下载线程
			}
			
		});
		return updatePd;
	}
	
	private String versionCodeToName(int code) {
		return code/100 + "." + code/10%10 + "." + code%100;
	}
	
	private String converToSuitableSize(int size) {
		if (size >= 1024)
		{
			return (size/1024.0 + "").substring(0, 3) + "MB";
		}
		return size + "KB";
	}

	public interface onCheckUpdate {
		void networkComplete();
		void noUpdate();
	}
}
