package com.pediy.bbs.kanxue;

import java.io.IOException;
import java.util.List;

import org.apache.http.cookie.Cookie;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pediy.bbs.kanxue.net.HttpClientUtil.NetClientCallback;
import com.pediy.bbs.kanxue.net.HttpClientUtil;
import com.pediy.bbs.kanxue.net.Api;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class LoginPage extends Activity {
	private TextView m_userName;
	private TextView m_passwd;
	private ProgressDialog m_pd;
	
	private Handler m_handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//关闭ProgressDialog
			m_pd.dismiss();
			
			switch (msg.what) {
			case HttpClientUtil.NET_SUCCESS:
		        LoginPage.this.setResult(1);
		        finish();
				break;
			case HttpClientUtil.NET_TIMEOUT:
				Toast.makeText(LoginPage.this, R.string.net_timeout, Toast.LENGTH_SHORT).show();
				break;
			case HttpClientUtil.NET_FAILED:
				Toast.makeText(LoginPage.this, R.string.net_failed, Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.login_page);
		
		m_userName = (TextView)this.findViewById(R.id.loginPageUsername);
		m_passwd = (TextView)this.findViewById(R.id.loginPagePasswd);
	}
	
	public void onBackBtnClick(View v) {
        this.finish();
	}
	
	public void onLoginBtnClick(View v) {
		String uname = m_userName.getText().toString();
		String passwd = m_passwd.getText().toString();
		if (uname.length() == 0) {
			Toast.makeText(this, R.string.login_username_alert, Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (passwd.length() == 0) {
			Toast.makeText(this, R.string.login_passwd_alert, Toast.LENGTH_SHORT).show();
			return;
		}
		m_pd = ProgressDialog.show(this, null, "登录中，请稍后……", true, true);
		
		Api.getInstance().login(uname, passwd, new NetClientCallback() {

			@Override
			public void execute(int status, String response,
					List<Cookie> cookies) {
				System.out.println("login:"+response);
				
				if (status == HttpClientUtil.NET_SUCCESS) {
					final JSONObject retObj = JSON.parseObject(response);
					final int ret = retObj.getInteger("result");
					if (ret != Api.LOGIN_SUCCESS) {
						m_handler.post(new Runnable() {

							@Override
							public void run() {
								m_pd.dismiss();
								switch (ret) {
								case Api.LOGIN_FAIL_LESS_THAN_FIVE:
									String alertText = "用户名或者密码错误,还有" + (Api.ALLOW_LOGIN_USERNAME_OR_PASSWD_ERROR_NUM - retObj.getInteger("strikes")) + "尝试机会";
									Toast.makeText(LoginPage.this, alertText, Toast.LENGTH_SHORT).show();
									break;
								case Api.LOGIN_FAIL_MORE_THAN_FIVE:
									Toast.makeText(LoginPage.this, R.string.login_fail_more_than_five, Toast.LENGTH_SHORT).show();
									break;
								}
								
							}
							
						});
						return;
					}
					String token = retObj.getString("securitytoken");
					Api.getInstance().setToken(token);
					Api.getInstance().setLoginUserInfo(retObj.getString("username"), retObj.getInteger("userid"), retObj.getInteger("isavatar"), retObj.getString("email"));
				
					for (int i = 0; i < cookies.size(); i++) {
						Cookie cookie = cookies.get(i);
						Api.getInstance().getCookieStorage().addCookie(cookie.getName(), cookie.getValue());
					}
					LoginPage.this.sendBroadcast(new Intent(App.LOGIN_STATE_CHANGE_ACTION));
				}
				m_handler.sendEmptyMessage(status);
				
			}
			
		});
	}
}
