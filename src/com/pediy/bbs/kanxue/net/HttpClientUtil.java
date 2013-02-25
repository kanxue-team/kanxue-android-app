package com.pediy.bbs.kanxue.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;


import android.util.Log;

public class HttpClientUtil {
	public static int METHOD_GET = 1;
	public static int METHOD_POST = 2;
	
	public static final int NET_SUCCESS = 1;
	public static final int NET_FAILED = 2;
	public static final int NET_TIMEOUT = 3;
	
	private String url;
	private int method;
	private NetClientCallback callback;
	private Map<String, String> params = new HashMap<String,String>();
	private String m_cookie = null;
	
	public HttpClientUtil(String url, int method, NetClientCallback callback){
		this.url = url;
		this.method = method;
		this.callback = callback;
	}
	
	public HttpClientUtil addParam(String name, String value){
		params.put(name, value);
		return this;
	}
	
	public void addCookie(String cookie) {
		this.m_cookie = cookie;
	}
	
	public void asyncDownloadTo(final String path,final boolean bForceRefresh){
		new Thread(new Runnable() {
			@Override
			public void run() {
				syncDownloadTo(path,bForceRefresh);
			}
		}).start();
	}
	
	public void syncDownloadTo(String path,boolean bForceRefresh){
		
		int status = NET_FAILED;

		OutputStream output=null;
		InputStream input = null;
		List<Cookie> cookies = null;
		try {
			BasicHttpParams localBasicHttpParams = new BasicHttpParams();
	        HttpConnectionParams.setConnectionTimeout(localBasicHttpParams, 6000);
	        HttpConnectionParams.setSoTimeout(localBasicHttpParams, 6000);
	        DefaultHttpClient client = new DefaultHttpClient(localBasicHttpParams);
	        client.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_1);
	        client.getParams().setParameter("http.useragent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)");
	        client.getParams().setParameter("http.protocol.expect-continue", Boolean.FALSE);
	        client.getParams().setParameter("http.protocol.content-charset", "UTF-8");
	        
			HttpUriRequest request = getRequest();
			HttpResponse response = client.execute(request);
			
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				long filelength = response.getEntity().getContentLength();
				if(filelength>0){
					input=response.getEntity().getContent();  
					File file=new File(path);
					if(!bForceRefresh && file.exists() && new FileInputStream(file).available()==filelength){
						if(callback!=null){
							cookies = client.getCookieStore().getCookies();
							callback.execute(NET_SUCCESS,"", cookies);
						}
						return;
					}
					output=new FileOutputStream(file);  
					
					long downlength = 0;
					
					byte[] buffer=new byte[4*1024];
					int len = input.read(buffer);
	
					while(len!=-1 ){
						output.write(buffer,0,len);
						downlength += len;
						len = input.read(buffer);
					}
					output.flush();
					if(downlength == filelength ){
						status = NET_SUCCESS;
						cookies = client.getCookieStore().getCookies();
					}
				}
			}
		} catch (SocketTimeoutException  e){
			status = NET_TIMEOUT;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch(IllegalStateException e){
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
    		try {
				if(output!=null){
					output.close();
				}
			} catch (IOException e) {  
			e.printStackTrace();  
			}
    		try {
				if(input!=null){
					input.close();
				}
			} catch (IOException e) {  
			e.printStackTrace();  
			}
		}
		if(callback!=null){
			callback.execute(status,"", cookies);
		}
	}
	
	public void asyncConnect(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				syncConnect();
			}
		}).start();
	}
	
	public void syncConnect(){
		String responseString = null;
		BufferedReader reader = null;
		int status = NET_FAILED;
		List<Cookie> cookies = null;
		try {
			BasicHttpParams localBasicHttpParams = new BasicHttpParams();
	        HttpConnectionParams.setConnectionTimeout(localBasicHttpParams, 6000);
	        HttpConnectionParams.setSoTimeout(localBasicHttpParams, 6000);
	        DefaultHttpClient client = new DefaultHttpClient(localBasicHttpParams);
	        client.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_1);
	        client.getParams().setParameter("http.useragent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)");
	        client.getParams().setParameter("http.protocol.expect-continue", Boolean.FALSE);
	        client.getParams().setParameter("http.protocol.content-charset", "UTF-8");
	        
			HttpUriRequest request = getRequest();
			
			HttpResponse response = client.execute(request);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuilder sb = new StringBuilder();
				for (String s = reader.readLine(); s != null; s = reader.readLine()) {
					sb.append(s);
				}
				responseString = sb.toString();
				status = NET_SUCCESS;
				cookies = client.getCookieStore().getCookies();
			}
		} catch (SocketTimeoutException  e){
			status = NET_TIMEOUT;
		} catch (ClientProtocolException e) {
			Log.e("HttpClientUtil", e.getMessage(), e);
		} catch (IOException e) {
			Log.e("HttpClientUtil", e.getMessage(), e);
		}  finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				// ignore me
			}
		}
		if(callback!=null){
			callback.execute(status,responseString, cookies);
		}
	}

	private HttpUriRequest getRequest() {
		if (method==METHOD_POST) {
			List<NameValuePair> listParams = new ArrayList<NameValuePair>();
			for (String name : params.keySet()) {
				listParams.add(new BasicNameValuePair(name, params.get(name)));
			}
			try {
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(listParams,"UTF-8");
				HttpPost request = new HttpPost(url);
				request.setEntity(entity);
				if (this.m_cookie != null) {
					request.addHeader("Cookie", this.m_cookie);
				}
				return request;
			} catch (UnsupportedEncodingException e) {
				throw new java.lang.RuntimeException(e.getMessage(), e);
			}
		} else {
			if (url.indexOf("?") < 0) {
				url += "?";
			}
			for (String name : params.keySet()) {
				try {
					url += "&" + name + "=" + URLEncoder.encode(params.get(name),"UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			HttpGet request = new HttpGet(url);
			if (this.m_cookie != null) {
				request.addHeader("Cookie", this.m_cookie);
			}
			return request;
		}
	}
	
	public interface NetClientCallback {
		public void execute(int status, String response, List<Cookie> cookies); 
	}
}
