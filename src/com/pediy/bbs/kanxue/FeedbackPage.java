package com.pediy.bbs.kanxue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.cookie.Cookie;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pediy.bbs.kanxue.net.HttpClientUtil.NetClientCallback;
import com.pediy.bbs.kanxue.net.HttpClientUtil;
import com.pediy.bbs.kanxue.net.Api;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class FeedbackPage extends Activity {
	private EditText m_name;
	private EditText m_email;
	private EditText m_message;
	private Handler m_handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//关闭ProgressDialog
			
			switch (msg.what) {
			case HttpClientUtil.NET_TIMEOUT:
				Toast.makeText(FeedbackPage.this, R.string.net_timeout, Toast.LENGTH_SHORT).show();
				break;
			case HttpClientUtil.NET_FAILED:
				Toast.makeText(FeedbackPage.this, R.string.net_failed, Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.feedback_page);
		this.m_name = (EditText)this.findViewById(R.id.feedbackName);
		this.m_name.setText(Html.fromHtml(Api.getInstance().getLoginUserName()));
		this.m_email = (EditText)this.findViewById(R.id.feedbackEmail);
		this.m_email.setText(Api.getInstance().getEmail());
		this.m_message = (EditText)this.findViewById(R.id.feedbackMessage);
	}
	
	public void onBackBtnClick(View v) {
		this.finish();
	}
	
	public void onSendBtnClick(View v) {
		
		String name = m_name.getText().toString();
		if (name.length() == 0) {
			Toast.makeText(FeedbackPage.this, "姓名不能为空", Toast.LENGTH_SHORT).show();
			this.m_name.requestFocus();
			return;
		}
		
		String email = m_email.getText().toString();
		if (email.length() == 0) {
			Toast.makeText(FeedbackPage.this, "Email不能为空", Toast.LENGTH_SHORT).show();
			this.m_email.requestFocus();
			return;
		} else {
			
			Pattern pattern = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(email);
			if (!matcher.matches()) {
				Toast.makeText(FeedbackPage.this, "不是有效的Email地址", Toast.LENGTH_SHORT).show();
				this.m_email.requestFocus();
				return;
			}
		}
		
		String msg = m_message.getText().toString();
		if (msg.length() == 0) {
			Toast.makeText(FeedbackPage.this, "信息不能为空", Toast.LENGTH_SHORT).show();
			this.m_message.requestFocus();
			return;
		} 
		final ProgressDialog pd = ProgressDialog.show(this, null, "提交反馈中，请稍后……", true, true);
		Api.getInstance().feedback(name, email, msg, new NetClientCallback() {

			@Override
			public void execute(final int status, final String response,
					List<Cookie> cookies) {
				System.out.println("feedback:" + response);
				
				m_handler.sendEmptyMessage(status);
				m_handler.post(new Runnable() {

					@Override
					public void run() {
						pd.dismiss();
						switch(status) {
						case HttpClientUtil.NET_SUCCESS:
							if (response == null) {
								Toast.makeText(FeedbackPage.this, "提交反馈失败", Toast.LENGTH_SHORT).show();
								return;
							}

							JSONObject jsonObj = JSON.parseObject(response);
							if (jsonObj == null) {
								Toast.makeText(FeedbackPage.this, "数据错误", Toast.LENGTH_SHORT).show();
								return;
							}
							int ret = jsonObj.getInteger("result");
							if (ret != 0) {
								Toast.makeText(FeedbackPage.this, "提交反馈失败", Toast.LENGTH_SHORT).show();
								return;
							}
							Toast.makeText(FeedbackPage.this, "提交反馈成功", Toast.LENGTH_SHORT).show();
							FeedbackPage.this.finish();
							break;
						default:
							break;
						}
						
					}
					
				});
				
			}
			
		});
	}
}
