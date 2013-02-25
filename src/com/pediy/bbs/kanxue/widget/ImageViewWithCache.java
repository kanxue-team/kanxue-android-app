package com.pediy.bbs.kanxue.widget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pediy.bbs.kanxue.util.SimpleHASH;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ImageView;

public class ImageViewWithCache extends ImageView {
	private static ExecutorService s_threadPool = Executors.newFixedThreadPool(4);
	private static HashMap<URL, ArrayList<ImageViewWithCache>> s_updateQueue = new HashMap<URL, ArrayList<ImageViewWithCache>>();
	private static Handler s_handler = new Handler();
	private static final String s_cachePath = Environment.getExternalStorageDirectory() + "/kanxue/img";
	private URL m_currentUrl = null;
	private int m_ageingTime = 0;		//老化时间,单位小时
	
	static {
		createCachePath();
	}

	public ImageViewWithCache(Context context) {
		super(context);
	}

	public ImageViewWithCache(Context context, AttributeSet attrs) {
		super(context, attrs);
		initWithAttrs(context, attrs);
	}

	public ImageViewWithCache(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initWithAttrs(context, attrs);
	}
	
	private void initWithAttrs(Context context, AttributeSet attrs) {
		//从xml文件中读取cache_path字段的值
 		int resouceId = attrs.getAttributeResourceValue(null, "ageing_time", 0);  
        if(resouceId <= 0)
        	return;
        m_ageingTime = Integer.parseInt(context.getResources().getString(resouceId));  
	}
	
	public void setAgeingTime(int hour) {
		this.m_ageingTime = hour;
	}
	
	public URL getCurrentUrl() {
		return this.m_currentUrl;
	}
	
	private static void createCachePath() {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return;
		}
		File file = new File(s_cachePath);
		if (file.exists())
			return;
		file.mkdirs();
	}
	
	private boolean isOld(File file) {
		if (this.m_ageingTime == 0)
			return false;
		
		long now = System.currentTimeMillis();
		long fileModeTime = file.lastModified();
		return (now - fileModeTime > this.m_ageingTime*60*60*1000);
	}
	
	public static Bitmap getBitmapFromUrl(URL url, String cookies) {
		if (url == null)
			return null;
		
		URLConnection connection;
		InputStream is = null;
		try {
			connection = url.openConnection();
			connection.setUseCaches(true);
			if (cookies != null) {
				connection.addRequestProperty("Cookie", cookies);
			}
			is = (InputStream)connection.getContent();
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		return BitmapFactory.decodeStream(is);
	}
	
	public static void loadImage(final URL url, final String cookies) {
		if (url == null)
			return;
		
		Runnable runGetImg = new Runnable() {
			@Override
			public void run() {
				final Bitmap bitmap = getBitmapFromUrl(url, cookies);
				if (bitmap == null)
					return;
				
				s_handler.post(new Runnable() {
					@Override
					public void run() {
						ArrayList<ImageViewWithCache> imgList = s_updateQueue.get(url);
						for (int i = 0; i < imgList.size(); i++) {
							ImageViewWithCache img = imgList.get(i);
							if (img.getCurrentUrl().equals(url)) {
								bitmap.setDensity(getDensityDpi(img.getContext()));
								img.setImageBitmap(bitmap);
							}
						}
						s_updateQueue.remove(url);
					}
					
				});
				cacheBitmapFromUrl(url, bitmap);
			}
		};
		s_threadPool.submit(runGetImg);
	}
	
	public static int getDensityDpi(Context con) {
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager mWm = (WindowManager)con.getSystemService(Context.WINDOW_SERVICE);
		mWm.getDefaultDisplay().getMetrics(metrics);
		return metrics.densityDpi;
	}
	
	public static void cacheBitmapFromUrl(URL url, Bitmap bitmap) {
		if (url == null || bitmap == null)
			return;
		
		File tFile = new File(getImgPathFromUrl(url));
		try {
			if (tFile != null) {
				if (!tFile.exists()) {
					tFile.createNewFile();
				}
				FileOutputStream fos = new FileOutputStream(tFile);
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
				fos.flush();
				fos.close();
			}
		} catch (FileNotFoundException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		} 
	}
	
	public static File getCachedImgFromUrl(URL url) {
		if (url == null)
			return null;
		
		File targetFile = null;
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			targetFile = new File(getImgPathFromUrl(url));
			if (targetFile.exists()) {
				return targetFile;
			} 
		}
		return null;
	}
	
	private static String getImgPathFromUrl(URL url) {
		if (url == null)
			return null;
		return s_cachePath + "/" + SimpleHASH.sha1(url.toString())  + ".png";
	}
	
	public void setImageUrl(URL url, String cookies) {
		if (url == null) 
			return;
		
		this.m_currentUrl = url;
		
		File targetFile = getCachedImgFromUrl(url);
		if (targetFile != null && !this.isOld(targetFile)) {
			Bitmap bitmap = BitmapFactory.decodeFile(targetFile.getAbsolutePath());
			if (bitmap == null) {
				System.err.println("ImageViewWithCache decodeFile failed。 url:" + url + " path:" + targetFile.getAbsolutePath());
				return;
			}
			bitmap.setDensity(getDensityDpi(this.getContext()));
			this.setImageBitmap(bitmap);
			return;
		}
		
		if (s_updateQueue.containsKey(url)) {
			s_updateQueue.get(url).add(this);
			return;
		} 
		ArrayList<ImageViewWithCache> imgList = new ArrayList<ImageViewWithCache>();
		imgList.add(this);
		s_updateQueue.put(url, imgList);
		loadImage(url, cookies);
	}
	
	public void setImageUrl(URL url) {
		this.setImageUrl(url, null);
	}
}
