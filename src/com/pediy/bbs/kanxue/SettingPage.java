package com.pediy.bbs.kanxue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.http.cookie.Cookie;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pediy.bbs.kanxue.App.onCheckUpdate;
import com.pediy.bbs.kanxue.net.Api;
import com.pediy.bbs.kanxue.net.HttpClientUtil;
import com.pediy.bbs.kanxue.net.HttpClientUtil.NetClientCallback;
import com.pediy.bbs.kanxue.widget.ImageViewWithCache;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingPage extends Activity {
	private TextView m_version;
	private ImageViewWithCache m_loginUserHeadImg = null;
	private TextView m_loginUserName = null;
	private ImageView m_loginIcon = null;
	private Handler m_handler = null;
	private Recv m_recv = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.setting_page);
		
		m_version = (TextView)this.findViewById(R.id.settingAppVersion);
		PackageInfo pinfo = null;
		try {
			pinfo = getPackageManager().getPackageInfo(SettingPage.this.getPackageName(), PackageManager.GET_CONFIGURATIONS);
			m_version.setText(pinfo.versionName);
		} catch (NameNotFoundException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		
		m_loginUserHeadImg = (ImageViewWithCache)this.findViewById(R.id.settingPageLoginUserHeadImg);
		m_loginUserName = (TextView)this.findViewById(R.id.settingPageLoginUserName);
		m_loginIcon = (ImageView)this.findViewById(R.id.settingPageLoginIcon);
		if (Api.getInstance().isLogin()) {
			onLoginState();
		}
		m_handler = ((App)SettingPage.this.getApplication()).getNetworkHandler();
		
		m_recv = new Recv();
		IntentFilter filter = new IntentFilter();
		filter.addAction(App.LOGIN_STATE_CHANGE_ACTION);
		this.registerReceiver(m_recv, filter);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(m_recv);
	}
	
	private void onLoginState() {
		if (Api.getInstance().getIsAvatar() == 1) {
			String url = Api.getInstance().getUserHeadImageUrl(Api.getInstance().getLoginUserId());
			try {
				m_loginUserHeadImg.setImageUrl(new URL(url));
			} catch (MalformedURLException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
		}
		m_loginIcon.setImageResource(R.drawable.logout_dark);
		//解析昵称中可能出现的html实体字符
		m_loginUserName.setText(Html.fromHtml(Api.getInstance().getLoginUserName()));
	}
	
	private void onLogoutState() {
		m_loginUserHeadImg.setImageResource(R.drawable.default_user_head_img);
		m_loginUserName.setText("游客");
		m_loginIcon.setImageResource(R.drawable.social_add_person_dark);
	}
	
	public void onLoginItemClick(View v) {
		if (!Api.getInstance().isLogin()) {
			this.startActivityForResult(new Intent(this, LoginPage.class), 0);
			return;
		}
		
		Builder builder = new Builder(this);
		builder.setMessage(R.string.logout_alert_msg);
		builder.setPositiveButton("确定", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				final ProgressDialog pd = ProgressDialog.show(SettingPage.this, null, "登出中，请稍后……", true, true);
				Api.getInstance().logout(new NetClientCallback() {

					@Override
					public void execute(int status, String response,
							List<Cookie> cookies) {
						System.out.println("logout renturn:"+response);
						if (status == HttpClientUtil.NET_SUCCESS) {
							m_handler.post(new Runnable() {
	
								@Override
								public void run() { 
									pd.dismiss();
									onLogoutState();
									Api.getInstance().clearLoginData();
									SettingPage.this.sendBroadcast(new Intent(App.LOGIN_STATE_CHANGE_ACTION));
								}
								
							});
							return;
						}
						m_handler.sendEmptyMessage(status);
					}
					
				});
				
			}
			
		}).setNegativeButton("取消", null);
		builder.create().show();
		// TODO 登录和登出两种状态刷新主页缓存
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case 1:
			onLoginState();
			break;
		default:
			break;
		}
	}
	
	public void onCheckUpdateItemClick(View v) {
		final ProgressDialog pd = ProgressDialog.show(this, null, "检测更新中，请稍后……", true, true);
		final Handler handler = new Handler();
		final App app = (App)this.getApplication();
		app.checkUpdate(this, new onCheckUpdate() {

			@Override
			public void networkComplete() {
				handler.post(new Runnable() {

					@Override
					public void run() {
						pd.dismiss();
					}
					
				});
				
			}

			@Override
			public void noUpdate() {
				handler.post(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(SettingPage.this, "已经是最新版本", Toast.LENGTH_SHORT).show();
					}
					
				});
			}

		});
	}
	
	public void onFeedbackItemClick(View v) {
		if (Api.getInstance().isLogin()) {
			this.startActivity(new Intent(this, FeedbackPage.class));
		} else {
			Intent emailIntent=new Intent(Intent.ACTION_SEND);  
			  
	        String subject = "意见反馈";  
	        String[] extra = new String[]{"webmaster@pediy.com"};  
	       
	        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);  
		    emailIntent.putExtra(Intent.EXTRA_EMAIL, extra);  
	        emailIntent.setType("message/rfc822");
	        try {
				startActivity(emailIntent);
			} catch (ActivityNotFoundException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
				Toast.makeText(SettingPage.this, "未安装邮件客户端", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	public void onAboutBtnClick(View v) {
		this.startActivity(new Intent(this, AboutPage.class));
	}
	
	private class Recv extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			//未登陆状态下点击发帖或者回帖时，弹出登陆界面，此时登陆后需通知settingpage更新登陆用户信息
			if (Api.getInstance().isLogin())
				SettingPage.this.onLoginState();
		}
		
	}
}
