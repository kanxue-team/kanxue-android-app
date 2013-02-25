package com.pediy.bbs.kanxue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.http.cookie.Cookie;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pediy.bbs.kanxue.net.HttpClientUtil;
import com.pediy.bbs.kanxue.net.HttpClientUtil.NetClientCallback;
import com.pediy.bbs.kanxue.net.Api;
import com.pediy.bbs.kanxue.widget.ImageViewWithCache;
import com.pediy.bbs.kanxue.widget.RefreshActionBtn;
import com.pediy.bbs.kanxue.widget.ThreadItemFooter;
import com.pediy.bbs.kanxue.widget.XListView;
import com.pediy.bbs.kanxue.widget.XListView.IXListViewListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.format.Time;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ShowThreadPage extends Activity implements IXListViewListener, OnItemClickListener {
	private XListView m_listView;
	private ShowthreadAdapter m_adapter;
	private int m_currentPage = 1;
	private int m_totalPage = 0;
	private int m_id = 0;
	private ProgressBar m_pBar;
	private JSONArray m_model = null;
	private ProgressDialog m_pd = null;
	private RefreshActionBtn m_refreshBtn;
	private TextView m_titleView;
	private long m_lastUpdateTime = 0;
	
	private ImageGetter m_imgGetter = new ImageGetter() {
		@Override  
        public Drawable getDrawable(String source) {
			URL url = null;
			Bitmap bitmap = null;
			try {
				url = new URL(source);
			} catch (MalformedURLException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
				return null;
			}
			
			File file = ImageViewWithCache.getCachedImgFromUrl(url);
			if (file != null) {
				bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
				if (bitmap == null)
					return null;
			} else {
				bitmap = ImageViewWithCache.getBitmapFromUrl(url, Api.getInstance().getCookieStorage().getCookies());
				if (bitmap == null)
					return null;
				ImageViewWithCache.cacheBitmapFromUrl(url, bitmap);
			}
			bitmap.setDensity(ImageViewWithCache.getDensityDpi(ShowThreadPage.this));
			Drawable drawable = new BitmapDrawable(bitmap);
			drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
			return drawable;
        }  
	};
	
	private Handler m_handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (m_pd != null) {
				m_pd.dismiss();
			}
			switch (msg.what) {
			case HttpClientUtil.NET_SUCCESS:				
				m_adapter.notifyDataSetChanged();
				//只在listview中有数据之后才启用pull load
				if (m_currentPage == 2 && m_totalPage != 0) {
					m_listView.setPullLoadEnable(true);
				}
				if (m_titleView.getVisibility() == View.GONE) {
					m_titleView.setText(m_model.getJSONObject(0).getString("title"));
					m_titleView.setVisibility(View.VISIBLE);
				}
				break;
			case HttpClientUtil.NET_TIMEOUT:
				Toast.makeText(ShowThreadPage.this, R.string.net_timeout, Toast.LENGTH_SHORT).show();
				break;
			case HttpClientUtil.NET_FAILED:
				Toast.makeText(ShowThreadPage.this, R.string.net_failed, Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}
			
			if (m_refreshBtn.isRefreshing()) {
				m_listView.setSelection(1);
				m_refreshBtn.endRefresh();
			}
			
			//停止上拉下拉时的processBar
			if (m_listView.getPullLoading()) {
				//加载到最后一页时禁用pull load
				if (m_currentPage > m_totalPage) {
					m_listView.setPullLoadEnable(false);
				}else {
					m_listView.stopLoadMore();
				}
			}
			
			if (m_listView.getPullRefreshing()) {
				m_listView.stopRefresh();
			}
			
			m_pBar.setVisibility(View.GONE);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.show_thread_page);
					
		m_listView = (XListView)this.findViewById(R.id.showthreadListview);	
		m_listView.setPullLoadEnable(false);
		m_listView.setPullRefreshEnable(true);
		m_listView.setXListViewListener(this);
		m_listView.setOnItemClickListener(this);
		View titleHeaderView = LayoutInflater.from(this).inflate(R.layout.show_thread_title_header, null);
		m_listView.addHeaderView(titleHeaderView);
		m_titleView = (TextView)titleHeaderView.findViewById(R.id.showThreadTitle);
		m_titleView.setVisibility(View.GONE);
		m_adapter = new ShowthreadAdapter();
		m_listView.setAdapter(m_adapter);
		m_listView.requestFocus();
		
		m_pBar = (ProgressBar)this.findViewById(R.id.showThreadProgressBar);
		m_refreshBtn = (RefreshActionBtn)this.findViewById(R.id.showThreadRefreshBtn);
		
		Bundle data = this.getIntent().getExtras();
		m_id = data.getInt("id");
		if (data.getInt("open") == 0) {
			this.findViewById(R.id.showThreadReplyBar).setVisibility(View.GONE);
		}
		loadModel(m_currentPage++);
	}
	
	@Override
	public void onRefresh() {
		m_refreshBtn.startRefresh();
		safeRefresh();
	}

	@Override
	public void onLoadMore() {
		loadModel(m_currentPage++);
	}
	
	public void onBackBtnClick(View v) {
		this.finish();
	}
	
	public void onPageTitleClick(View v) {
		//滑动listView到顶端
		m_listView.setSelection(0);
	}
	
	private void refresh() {
		//刷新之前判断有无新贴
		Api.getInstance().checkNewPostInShowThreadPage(m_id, m_lastUpdateTime, new NetClientCallback() {

			@Override
			public void execute(int status, String response,
					List<Cookie> cookies) {
				if (status != HttpClientUtil.NET_SUCCESS) {
					m_handler.sendEmptyMessage(status);
					return;
				}
				final JSONObject obj = JSON.parseObject(response);
				if (obj.getInteger("result") == 0) {
					m_handler.post(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(ShowThreadPage.this, "无新贴", Toast.LENGTH_SHORT).show();
						}
						
					});
					m_handler.sendEmptyMessage(0);
					return;
				}
				m_currentPage = 1;
				loadModel(m_currentPage++);
			}
			
		});
	}
	
	public void onReplyBtnClick(View v) {
		if (!Api.getInstance().isLogin()) {
			this.startActivity(new Intent(this, LoginPage.class));
			return;
		}
		final EditText replyTextView = (EditText)this.findViewById(R.id.showThreadReplyText);
		if (replyTextView.length() == 0) {
			Toast.makeText(ShowThreadPage.this, "回帖内容不能为空", Toast.LENGTH_SHORT).show();
			replyTextView.requestFocus();
			return;
		} else if (replyTextView.length() < Api.POST_CONTENT_SIZE_MIN) {
			Toast.makeText(ShowThreadPage.this, "回帖内容长度不能小于"+Api.POST_CONTENT_SIZE_MIN+"个字符", Toast.LENGTH_SHORT).show();
			replyTextView.requestFocus();
			return;
		}
		
		this.m_pd = ProgressDialog.show(this, "提示", "回帖中，请稍后……", true, true);
		Api.getInstance().quickReply(this.m_id, replyTextView.getText().toString(), new NetClientCallback() {

			@Override
			public void execute(int status, final String response,
					List<Cookie> cookies) {
				System.out.println("reply: "+response);
				m_handler.sendEmptyMessage(status);
				if (status != HttpClientUtil.NET_SUCCESS)
					return;
				m_handler.post(new Runnable() {

					@Override
					public void run() {
						JSONObject jsonObj = JSON.parseObject(response);
						int result = jsonObj.getInteger("result");
						switch(result) {
						case Api.NEW_POST_SUCCESS:
							Toast.makeText(ShowThreadPage.this, R.string.new_post_success, Toast.LENGTH_SHORT).show();
							break;
						case Api.NEW_POST_FAIL_WITHIN_THIRTY_SECONDS:
							Toast.makeText(ShowThreadPage.this, R.string.new_post_fail_within_thirty_seconds, Toast.LENGTH_SHORT).show();
							return;
						case Api.NEW_POST_FAIL_WITHIN_FIVE_MINUTES:
							Toast.makeText(ShowThreadPage.this, R.string.new_post_fail_within_five_minutes, Toast.LENGTH_SHORT).show();
							return;
						default:
							break;
						}
						
						JSONObject item = new JSONObject();
						item.put("postid", -1);
						item.put("thumbnail", 0);
						item.put("username", Html.fromHtml(Api.getInstance().getLoginUserName()));
						item.put("userid", Api.getInstance().getLoginUserId());
						item.put("avatar", Api.getInstance().getIsAvatar());
						item.put("avatardateline", "");
						
						Time time = new Time();
						time.setToNow();
						
						int hour12 = time.hour;
						String hour = null;
						if (time.hour > 12) {
							hour12 = time.hour - 12;
						} 
						hour = "" + ((hour12 > 10)?hour12:("0"+hour12));
						item.put("posttime", hour + ":" + time.minute + " " + ((time.hour > 12)?"PM":"AM"));
						
						item.put("postdate", "今天");
						item.put("message", replyTextView.getText().toString());
						m_model.add(item);
						m_adapter.notifyDataSetChanged();
						m_listView.setSelection(m_listView.getCount());
						
						replyTextView.setText("");
						replyTextView.clearFocus();
						//关闭软键盘
						InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(replyTextView.getWindowToken(), 0);
					}
					
				});
			}
			
		});
	}
	
	private void safeRefresh() {
		//如果开始网络传输失败了，强制重新刷新
		if (this.m_model == null) {
			m_currentPage = 1;
			loadModel(m_currentPage++);
		} else {
			refresh();
		}
	}
	
	public void onRefreshBtnClick(View v) {
		if (m_refreshBtn.isRefreshing())
			return;
		m_refreshBtn.startRefresh();
		m_listView.setSelection(0);
		m_listView.pullRefreshing();
		safeRefresh();
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
		//对header进行点击也会触发该事件
		if (position < 2) 
			return;
		
		final JSONObject item = m_model.getJSONObject(position-2);	//listView 中有两个header，所以此处减去2
		if (item.getInteger("thumbnail") != 1) 
			return;
		
		final ThreadItemFooter itemFooter = (ThreadItemFooter)v.findViewById(R.id.showthreadLoadTip);
		if (itemFooter.isLoading()) {
			return;
		}
		
		final TextView msg = (TextView)v.findViewById(R.id.showthreadMsg);
		final int postId = Integer.parseInt(item.get("postid").toString());
		NetClientCallback ncc = new NetClientCallback() {
			@Override
			public void execute(int status,
					String response, List<Cookie> cookies) {
				if (status == HttpClientUtil.NET_SUCCESS) {
					final Spanned spanned = Html.fromHtml(response, m_imgGetter, null);
					item.put("isExpanded", 1);
					item.put("expandSpanned", spanned);
					m_handler.post(new Runnable(){

						@Override
						public void run() {
							msg.setText(spanned);
							itemFooter.setLoadFinish();
							itemFooter.setExpanded();
						}
						
					});
					return;
				}
				m_handler.post(new Runnable() {

					@Override
					public void run() {
						itemFooter.setLoadFinish();
					}
					
				});
				m_handler.sendEmptyMessage(status);
			}
		};
		
		if (item.containsKey("isExpanded") && item.getInteger("isExpanded") == 1) {
			item.put("isExpanded", 0);
			msg.setText((Spanned)item.get("thumbnailSpanned"));
			itemFooter.setCollapsed();
			m_listView.setSelection(position);
		}else {
			if (item.containsKey("expandSpanned")) {
				item.put("isExpanded", 1);
				msg.setText((Spanned)item.get("expandSpanned"));
				itemFooter.setExpanded();
			}else {
				itemFooter.setLoading();
				Api.getInstance().getForumFullThread(postId, ncc);
			}
		}
	}
	
	private void loadModel(final int page) {
		Api.getInstance().getForumShowthreadPage(m_id, page, new NetClientCallback() {

			@Override
			public void execute(int status, String response, List<Cookie> cookies) {
				if (status == HttpClientUtil.NET_SUCCESS) {
					JSONObject jsonObj = JSON.parseObject(response);
					JSONArray jsonArr = jsonObj.getJSONArray("postbits");
					if (page == 1) {
						m_totalPage = jsonObj.getInteger("pagenav");
						m_lastUpdateTime = jsonObj.getLong("time");
						m_model = jsonArr;
					} else {
						//加载更多页时,删除在第一页列表末尾插入的回复贴
						if (m_model.size() != 0 && m_model.getJSONObject(m_model.size() - 1).getInteger("postid") == -1) {
							m_model.remove(m_model.size() - 1);
						}
						for (int i = 0; i < jsonArr.size(); i++) {
							m_model.add(jsonArr.getJSONObject(i));
						}		
					}
				}						
				m_handler.sendEmptyMessage(status);
			}
		});
	}
	
	private class ShowthreadAdapter extends BaseAdapter {		

		public ShowthreadAdapter() {
			super();
		}

		@Override
		public int getCount() {
			return (m_model == null)?0:m_model.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.show_thread_item, null);
			}
			
			TextView username = (TextView)convertView.findViewById(R.id.showthreadUsername);
			TextView floorNum = (TextView)convertView.findViewById(R.id.showThreadFloorNum);
			floorNum.setText((position + 1) + "#");
			TextView posttime = (TextView)convertView.findViewById(R.id.showthreadPosttime);
			final TextView msg = (TextView)convertView.findViewById(R.id.showthreadMsg);
			ImageViewWithCache img = (ImageViewWithCache)convertView.findViewById(R.id.showthreadHeadImg);
			ThreadItemFooter itemFooter = (ThreadItemFooter)convertView.findViewById(R.id.showthreadLoadTip);
			
			final JSONObject item = m_model.getJSONObject(position);
			username.setText(Html.fromHtml(item.get("username").toString()));
			posttime.setText(item.get("postdate")+" "+item.get("posttime"));
			if (item.getInteger("avatar") == 1) {
				try {
					img.setImageUrl(new URL(Api.getInstance().getUserHeadImageUrl(item.getInteger("userid"))));
				} catch (MalformedURLException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}
			}else {
				img.setImageResource(R.drawable.default_user_head_img);
			}
			//格式化缩略帖子缩略信息的Runnable
			Runnable runFormatMessage = new Runnable() {

				@Override
				public void run() {
					final Spanned spanned = Html.fromHtml(item.get("message").toString(), m_imgGetter, null);
					item.put("thumbnailSpanned", spanned);
					m_handler.post(new Runnable(){

						@Override
						public void run() {
							msg.setText(spanned);
						}
						
					});
				}
				
			};
			
			//长文章的点击扩展
			itemFooter.setVisibility((item.getInteger("thumbnail") == 1)?View.VISIBLE:View.GONE);
			if (item.containsKey("isExpanded") && item.getInteger("isExpanded") == 1) {
				//onItemClick中扩展文章，缓存格式化后的对象
				msg.setText((Spanned)item.get("expandSpanned"));
				itemFooter.setExpanded();
			}else {
				if (item.containsKey("thumbnailSpanned")) {
					msg.setText((Spanned)item.get("thumbnailSpanned"));
				} else {
					new Thread(runFormatMessage).start();
				}
				itemFooter.setCollapsed();
			}
			
			//图片中带附件处理
			View attachmentView = convertView.findViewById(R.id.showthreadAttachment);
			if (!item.containsKey("thumbnailattachments") && !item.containsKey("otherattachments")) {
				attachmentView.setVisibility(View.GONE);
			}
			
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			lp.setMargins(0, 5, 0, 0);
			LinearLayout attachmentList = (LinearLayout)convertView.findViewById(R.id.showThreadImgAttachmentList);
			if (item.containsKey("thumbnailattachments")) {
				JSONArray arr = item.getJSONArray("thumbnailattachments");
				attachmentView.setVisibility(View.VISIBLE);
				attachmentList.removeAllViews();
				for (int i = 0; i < arr.size(); i++) {
					
					ImageViewWithCache imgWithCache = new ImageViewWithCache(ShowThreadPage.this);
					imgWithCache.setLayoutParams(lp);
					int attachmentId = arr.getJSONObject(i).getInteger("attachmentid");
					try {
						URL url = new URL(Api.getInstance().getAttachmentImgUrl(attachmentId));
						imgWithCache.setImageUrl(url, Api.getInstance().getCookieStorage().getCookies());
					} catch (MalformedURLException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
					attachmentList.addView(imgWithCache);
				}
			}
			
			//其他类型附件处理
			attachmentList = (LinearLayout)convertView.findViewById(R.id.showThreadOtherAttachmentList);
			if (item.containsKey("otherattachments")) {
				JSONArray arr = item.getJSONArray("otherattachments");
				attachmentView.setVisibility(View.VISIBLE);
				attachmentList.removeAllViews();
				
				for (int i = 0; i < arr.size(); i++) {
					TextView filename = new TextView(ShowThreadPage.this);
					filename.setLayoutParams(lp);
					filename.setText(arr.getJSONObject(i).getString("filename"));
					attachmentList.addView(filename);
				}
			}
			
			return convertView;
		}
	}
}
