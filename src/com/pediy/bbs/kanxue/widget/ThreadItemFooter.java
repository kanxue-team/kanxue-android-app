package com.pediy.bbs.kanxue.widget;

import com.pediy.bbs.kanxue.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ThreadItemFooter extends LinearLayout {
	private ImageView m_TipImg;
	private TextView m_TipText;
	private View m_processBar;

	public ThreadItemFooter(Context context) {
		super(context);
	}

	public ThreadItemFooter(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.thread_item_footer, this);
		m_TipImg = (ImageView)this.findViewById(R.id.threadItemFooterTipImg);
		m_TipText = (TextView)this.findViewById(R.id.threadItemFooterTipText);
		m_processBar = this.findViewById(R.id.threadItemProgressBar);
	}

	public void setExpanded() {
		m_TipImg.setImageResource(R.drawable.navigation_collapse);
		m_TipText.setText(R.string.tap_load_less);
	}
	
	public void setCollapsed() {
		m_TipImg.setImageResource(R.drawable.navigation_expand);
		m_TipText.setText(R.string.tap_load_more);
	}
	
	public void setLoading() {
		m_TipImg.setVisibility(View.INVISIBLE);
		m_processBar.setVisibility(View.VISIBLE);
	}
	
	public void setLoadFinish() {
		m_processBar.setVisibility(View.GONE);
		m_TipImg.setVisibility(View.VISIBLE);
	}
	
	public boolean isLoading() {
		return (m_TipImg.getVisibility() == View.INVISIBLE);
	}
}
