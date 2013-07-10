package com.pediy.bbs.kanxue;

import java.util.List;

import org.apache.http.cookie.Cookie;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pediy.bbs.kanxue.net.Api;
import com.pediy.bbs.kanxue.net.HttpClientUtil;
import com.pediy.bbs.kanxue.net.HttpClientUtil.NetClientCallback;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class UserInfoPage extends Activity {

	private TextView m_UserNameContentView;
	private TextView m_IDView;
	private TextView m_IDContentView;
	private TextView m_UserTitleView;
	private TextView m_UserTitleContentView;
	private TextView m_PostsView;
	private TextView m_PostsContentView;
	private TextView m_moneyView;
	private TextView m_MoneyContentView;
	private TextView m_GoodnessView;
	private TextView m_GoodnessContentView;
	private String m_UserNameContent = null;
	private String m_UserTitleContent = null;
	private int m_PostsContent = 0;
	private int m_MoneyContent = 0;
	private int m_GoodnessContent = 0;
	private Integer m_IDContent = 0;
	private JSONArray m_person = null;

	
	private  int m_id = 0;
	
	private Handler m_handler = new Handler(){


		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HttpClientUtil.NET_SUCCESS:

	            for(int i = 0; i <  m_person.size(); i++){
                    JSONObject jsonObj = m_person.getJSONObject(i);
					m_UserNameContent = jsonObj.getString("username");
					m_UserTitleContent = jsonObj.getString("usertitle");
					m_IDContent = jsonObj.getInteger("userid");
					m_PostsContent = jsonObj.getInteger("posts");
					m_MoneyContent = jsonObj.getInteger("money");
					m_GoodnessContent = jsonObj.getInteger("goodnees");
					m_UserNameContentView.setText(Html.fromHtml(m_UserNameContent));
					m_UserTitleContentView.setText(Html.fromHtml(m_UserTitleContent));
					m_IDContentView.setText(""+m_IDContent);
					m_PostsContentView.setText(""+m_PostsContent);
					m_MoneyContentView.setText(""+m_MoneyContent+"   Kx");
					m_GoodnessContentView.setText(""+m_GoodnessContent);
	
	            }
	            break;
			case HttpClientUtil.NET_TIMEOUT:
				Toast.makeText(UserInfoPage.this, R.string.net_timeout, Toast.LENGTH_SHORT).show();
				break;
			case HttpClientUtil.NET_FAILED:
				Toast.makeText(UserInfoPage.this, R.string.net_failed, Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}
		}
		
	};
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.show_user_info);
		
		m_IDView = (TextView)this.findViewById(R.id.peID);
		m_UserNameContentView = (TextView)this.findViewById(R.id.peNameConent);
		m_IDContentView =(TextView)this.findViewById(R.id.peIDConent);
		m_UserTitleView = (TextView)this.findViewById(R.id.peUserTitle);
		m_UserTitleContentView = (TextView)this.findViewById(R.id.peUserTitleConent);
		m_PostsView = (TextView)this.findViewById(R.id.pePosts);
		m_PostsContentView = (TextView)this.findViewById(R.id.pePostsConent);
		m_moneyView = (TextView)this.findViewById(R.id.peMoneny);
		m_MoneyContentView = (TextView)this.findViewById(R.id.peMoneyConent);
		m_GoodnessView = (TextView)this.findViewById(R.id.peGoodness);
		m_GoodnessContentView = (TextView)this.findViewById(R.id.peGoodnessConent);
        Bundle bundle = this.getIntent().getExtras();
        
        /*从Bundle中获得所需查看用戶的ID*/
        m_id = bundle.getInt("user_id");
		if (Api.getInstance().isLogin()) {
			onLoginState();
		}


	}


	
	private void onLoginState() {
		// 获取用户信息列表
		//m_id = Api.getInstance().getLoginUserId();

		if (m_id > 0) {
			Api.getInstance().getUserInfoPage(m_id, new NetClientCallback() {

				@Override
				public void execute(int status, String response,
						List<Cookie> cookies) {
					Looper.prepare();
					if (status == HttpClientUtil.NET_SUCCESS) {
						JSONArray jsonArray = JSON.parseArray(response);
						m_person = jsonArray;

					} else {
						Toast.makeText(UserInfoPage.this, "提示：目前网络状况不佳。",
								Toast.LENGTH_SHORT).show();
					}
					m_handler.sendEmptyMessage(status);
					Looper.loop();
				}
			});
		}
		else {
			Toast.makeText(UserInfoPage.this, "用户ID"+m_id+"不合法",
					Toast.LENGTH_SHORT).show();
		}
		
	}
	
	
	public void onBackBtnClick(View v) {
		finish();
	}

}
