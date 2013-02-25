package com.pediy.bbs.kanxue.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Base64;

public class ObjStorage {
	private SharedPreferences m_sharedPreferences;
	
	public ObjStorage(SharedPreferences sharedPreferences) {
		m_sharedPreferences = sharedPreferences;
	}
	
	public void save(String key, Object obj) {
		if (obj == null || key == null)
			return;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			return;
		}
		
		SharedPreferences.Editor editor = m_sharedPreferences.edit();
		editor.putString(key, Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT));
		editor.commit();
	}
	
	public Object load(String key) {
		String wordBase64 = m_sharedPreferences.getString(key, null);
		if (wordBase64 == null)
			return null;
		byte[] base64Bytes = Base64.decode(wordBase64.getBytes(), Base64.DEFAULT);
		ByteArrayInputStream bais = new ByteArrayInputStream(base64Bytes);
		ObjectInputStream ois = null;
		Object ret = null;
		try {
			ois = new ObjectInputStream(bais);
			ret = ois.readObject();
		} catch (StreamCorruptedException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			return null;
		}
		return ret;
	}
	
	public void remove(String key) {
		Editor editor = m_sharedPreferences.edit();
		editor.remove(key);
		editor.commit();
	}
}
