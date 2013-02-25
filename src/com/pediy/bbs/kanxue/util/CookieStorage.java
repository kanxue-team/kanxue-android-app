package com.pediy.bbs.kanxue.util;

import java.io.IOException;
import java.util.HashMap;

public class CookieStorage {
	private ObjStorage m_objStorage = null;
	private HashMap<String, String> m_cookies = null;
	
	public CookieStorage(ObjStorage objStorage) {
		this.m_objStorage = objStorage;
		Object obj = m_objStorage.load("cookie");
		this.m_cookies = (obj == null)?new HashMap<String, String>():(HashMap<String, String>)obj;
	}
	
	public void addCookie(String name, String value) {
		if (name == null || value == null)
			return;
		
		if (m_cookies.containsKey(name) && m_cookies.get(name) == value)
			return;
		m_cookies.put(name, value);
		m_objStorage.save("cookie", m_cookies);
	}
	
	public String getCookies() {
		if (this.m_cookies.size() == 0)
			return null;
		
		String cookies = "";
		Object[] keys = this.m_cookies.keySet().toArray();
		cookies += (keys[0]+"="+this.m_cookies.get(keys[0]));
		for (int i = 1; i < m_cookies.size(); i++) {
			cookies += ("; "+keys[i]+"="+this.m_cookies.get(keys[i]));
		}
		return cookies;
	}
	
	public boolean hasCookie(String name) {
		return this.m_cookies.containsKey(name);
	}
	
	public void clearAll() {
		this.m_cookies.clear();
		m_objStorage.remove("cookie");
	}
	
	public void remove(String name) throws IOException {
		if (null == name)
			return;
		
		m_cookies.remove(name);
		m_objStorage.save("cookie", m_cookies);
	}
}
