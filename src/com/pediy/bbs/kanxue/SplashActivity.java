package com.pediy.bbs.kanxue;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

public class SplashActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final View view = View.inflate(this, R.layout.splash, null);
		setContentView(view);
		
		/*AlphaAnimation anim = new AlphaAnimation(0.3f,1.0f);
		anim.setDuration(100);
		view.startAnimation(anim);
		anim.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation arg0) {
				gotoMainPage();
			}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationStart(Animation animation) {}
		});*/
		
		new Thread() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}
				gotoMainPage();
			}
			
		}.start();
	}
	
	private void gotoMainPage() {
		Intent intent = (new Intent(this, MainActivity.class));
        startActivity(intent);
        finish();
	}
}
