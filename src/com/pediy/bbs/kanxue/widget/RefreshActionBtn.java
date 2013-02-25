package com.pediy.bbs.kanxue.widget;

import com.pediy.bbs.kanxue.R;

import android.content.Context;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

public class RefreshActionBtn extends RelativeLayout {
	private ImageView m_icon;

	public RefreshActionBtn(Context context) {
		super(context);
		this.init(context);
	}
	
	public RefreshActionBtn(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.init(context);
	}

	public RefreshActionBtn(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.init(context);
	}

	private void init(Context context) {
		LayoutInflater.from(context).inflate(R.layout.refresh_action_btn, this);
		m_icon = (ImageView)this.findViewById(R.id.refreshBtn);
		
	}

	public void startRefresh() {
		if (m_icon.getAnimation() != null && !m_icon.getAnimation().hasEnded()) {
			return;
		}
		float centerX = m_icon.getWidth()/2.0f;
		float centerY = m_icon.getHeight()/2.0f;

		RotateAnimation ra = new RotateAnimation(0, 360, centerX, centerY);
		ra.setDuration(1000);
		ra.setRepeatCount(-1);
		ra.setInterpolator(new LinearInterpolator());
		this.m_icon.startAnimation(ra);
	}
	
	public void endRefresh() {
		this.m_icon.clearAnimation();
	}
	
	public boolean isRefreshing() {
		Animation a = this.m_icon.getAnimation();
		return (a != null)?!a.hasEnded():false;
	}
}
