package com.pediy.bbs.kanxue;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class AboutPage extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.about_page);
		
		PackageInfo pinfo;
		try {
			pinfo = getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_CONFIGURATIONS);
			TextView tv = (TextView)this.findViewById(R.id.aboutPageVerText);
			tv.setText("v" + pinfo.versionName + " Beta");
		} catch (NameNotFoundException e1) {
			// TODO 自动生成的 catch 块
			e1.printStackTrace();
		}
	}
	
	public void onBackBtnClick(View v) {
		finish();
	}
	
}
