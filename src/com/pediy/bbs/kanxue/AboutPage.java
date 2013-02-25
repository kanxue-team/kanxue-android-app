package com.pediy.bbs.kanxue;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class AboutPage extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.about_page);
	}
	
	public void onBackBtnClick(View v) {
		finish();
	}
	
}
