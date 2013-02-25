package com.pediy.bbs.kanxue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.cookie.Cookie;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pediy.bbs.kanxue.net.HttpClientUtil;
import com.pediy.bbs.kanxue.net.HttpClientUtil.NetClientCallback;
import com.pediy.bbs.kanxue.net.Api;
import com.pediy.bbs.kanxue.widget.AmazingAdapter;
import com.pediy.bbs.kanxue.widget.AmazingListView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class ForumHomePage extends Activity implements OnItemClickListener {
	private static int CACHE_AGEING_TIME = 7*24;  //缓存的老化时间单位小时
	private ArrayList<HashMap<String, String>> m_groupModel = new ArrayList<HashMap<String, String>>();
	private ArrayList<HashMap<String, String>> m_forumModel = new ArrayList<HashMap<String, String>>();
	private AmazingListView m_listView;
	
	private ProgressBar m_pBar;
	private Button m_retryBtn;
	private ForumHomeAdapter m_adapter = new ForumHomeAdapter();;
	private SharedPreferences m_sp = null;
	private Recv m_recv = null;
	
	private Handler m_handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			m_pBar.setVisibility(View.GONE);
			switch (msg.what) {
			case HttpClientUtil.NET_SUCCESS:
				m_adapter.notifyDataSetChanged();
				break;
			case HttpClientUtil.NET_TIMEOUT:
				Toast.makeText(ForumHomePage.this, R.string.net_timeout, Toast.LENGTH_SHORT).show();
				m_retryBtn.setVisibility(View.VISIBLE);
				break;
			case HttpClientUtil.NET_FAILED:
				Toast.makeText(ForumHomePage.this, R.string.net_failed, Toast.LENGTH_SHORT).show();
				m_retryBtn.setVisibility(View.VISIBLE);
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.forum_home_page);
					
		m_listView = (AmazingListView)this.findViewById(R.id.forumHomeListView);
		m_listView.setPinnedHeaderView(LayoutInflater.from(this).inflate(R.layout.forum_home_item_header, m_listView, false));
		m_listView.setAdapter(m_adapter); 
		m_listView.setOnItemClickListener(this);
		
		m_pBar = (ProgressBar)this.findViewById(R.id.forumHomeProgressBar);
		m_retryBtn = (Button)this.findViewById(R.id.forumHomeRetryBtn);
		m_sp = PreferenceManager.getDefaultSharedPreferences(this);
		//cache处理
		long now = System.currentTimeMillis();
		long cacheTime = m_sp.getLong("homePageCacheTime", 0);
		boolean cacheLoginState = m_sp.getBoolean("homePageCacheWhenLogin", false);
		//当当前的登陆状态与cache时的登陆状态不同需要重新刷新
		if ((now - cacheTime) > CACHE_AGEING_TIME*60*60*1000 || cacheLoginState != Api.getInstance().isLogin()) {
			m_pBar.setVisibility(View.VISIBLE);
			loadModel();
		} else {
			m_pBar.setVisibility(View.GONE);
			String cache = m_sp.getString("homePageCache", null);
			if (cache != null) {
				parseResponse(cache);
			}
		}
		
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
	
	private void loadModel() {
		Api.getInstance().getForumHomePage(new NetClientCallback() {

			@Override
			public void execute(int status, String response,
					List<Cookie> cookies) {
				if (status == HttpClientUtil.NET_SUCCESS) {
					parseResponse(response);
					Editor e = m_sp.edit();
					e.putString("homePageCache", response);
					e.putLong("homePageCacheTime", System.currentTimeMillis());
					e.putBoolean("homePageCacheWhenLogin", Api.getInstance().isLogin());
					e.commit();
				}
				m_handler.sendEmptyMessage(status);
			}
			
		});
	}
	
	private void parseResponse(String response) {
		JSONObject retObj = JSON.parseObject(response);
		
		JSONArray ret = retObj.getJSONArray("forumbits");
		int positionForSection = 0;
		for (int i = 0; i < ret.size(); i++) {
			JSONObject innerObj = ret.getJSONObject(i);
			JSONArray forumArray = innerObj.getJSONArray("forumSubTitle");

			HashMap<String, String> item = new HashMap<String, String>();
			item.put("forumTitle", innerObj.getString("forumTitle"));
			item.put("positionForSection", "" + positionForSection);
			ForumHomePage.this.m_groupModel.add(item);
			positionForSection += forumArray.size();

			for (int j = 0; j < forumArray.size(); j++) {
				item = new HashMap<String, String>();
				JSONObject forumObj = forumArray.getJSONObject(j);

				Iterator<String> it = forumObj.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					item.put(key, forumObj.getString(key));
				}
				item.put("sectionForPosition", "" + i);
				ForumHomePage.this.m_forumModel.add(item);
			}
		}
	}
	
	private void refresh() {
		this.m_forumModel.clear();
		this.m_groupModel.clear();
		this.loadModel();
	}
	
	
	public void onRetryBtnClick(View v) {
		v.setVisibility(View.GONE);
		this.m_pBar.setVisibility(View.VISIBLE);
		this.loadModel();
	}
	
	public void onPageTitleClick(View v) {
		m_listView.setSelection(0);
	}
		
	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
		HashMap<String, String> model = m_forumModel.get(position);
		
		Bundle data = new Bundle();
		data.putString("title", model.get("name"));
		data.putInt("id", Integer.parseInt(model.get("id")));
		data.putBoolean("isHideBackBtn", false);
		Intent intent = new Intent(ForumHomePage.this, ForumDisplayPage.class);
		intent.putExtras(data);
		this.startActivity(intent);
	}
	
	//当登出或者登陆状态发生变化时，收到刷新消息
	private class Recv extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			ForumHomePage.this.refresh();
		}
		
	}
	
	class ForumHomeAdapter extends AmazingAdapter {		

		public ForumHomeAdapter() {
			super();
		}

		@Override
		public int getCount() {
			return (m_forumModel!=null)?m_forumModel.size():0;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getAmazingView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.forum_home_item, null);
			}
			TextView tv = (TextView)convertView.findViewById(R.id.forumHomeItemTitle);
			tv.setText(m_forumModel.get(position).get("name"));
			
			tv = (TextView)convertView.findViewById(R.id.forumHomeItemSubtitle);
			tv.setText("subTitle");
			return convertView;
		}

		@Override
		public int getPositionForSection(int section) {
			return Integer.parseInt(m_groupModel.get(section).get("positionForSection"));
		}

		@Override
		public int getSectionForPosition(int position) {
			return Integer.parseInt(m_forumModel.get(position).get("sectionForPosition"));
		}

		@Override
		public Object[] getSections() {
			return null;
		}

		@Override
		protected void onNextPageRequested(int page) {
			
		}

		@Override
		protected void bindSectionHeader(View view, int position, boolean displaySectionHeader) {
			if (displaySectionHeader) {
				view.findViewById(R.id.forumHomeItemHeader).setVisibility(View.VISIBLE);
				TextView lSectionTitle = (TextView) view.findViewById(R.id.forumHomeItemHeader);
				lSectionTitle.setText(m_groupModel.get(getSectionForPosition(position)).get("forumTitle"));
			} else {
				view.findViewById(R.id.forumHomeItemHeader).setVisibility(View.GONE);
			}
		}

		@Override
		public void configurePinnedHeader(View header, int position, int alpha) {
			TextView lSectionHeader = (TextView)header;
			lSectionHeader.setText(m_groupModel.get(getSectionForPosition(position)).get("forumTitle"));
			//lSectionHeader.setBackgroundColor(alpha << 24 | Color.parseColor(R.color.header_color));
			//lSectionHeader.setTextColor(alpha << 24 | (0xFFFFFF));
		}
	}
}
