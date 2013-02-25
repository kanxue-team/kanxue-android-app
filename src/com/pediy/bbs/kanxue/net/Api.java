package com.pediy.bbs.kanxue.net;

import java.util.List;

import org.apache.http.cookie.Cookie;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pediy.bbs.kanxue.net.HttpClientUtil.NetClientCallback;
import com.pediy.bbs.kanxue.util.CookieStorage;
import com.pediy.bbs.kanxue.util.ObjStorage;
import com.pediy.bbs.kanxue.util.SimpleHASH;

/**
 * 看雪看全论坛开放api类
 * @author fanhexin
 *
 */
public class Api {
	public static final String DOMAIN = "http://bbs.pediy.com";
	public static final String PATH = "/";
	/**
	 * 看雪安卓客户端专用的模板id，可通过在看雪任一url后加入模板参数查看各接口返回数据
	 */
	public static final String STYLE = "styleid=12";
	
	//登录返回状态码
	public static final int LOGIN_SUCCESS = 0;
	public static final int LOGIN_FAIL_LESS_THAN_FIVE = 1; //用户名或密码错误尝试5次以内
	public static final int LOGIN_FAIL_MORE_THAN_FIVE = 2; //登录失败次数超过5次,15分钟后才可继续登录
	
	//发新贴返回状态码
	public static final int NEW_POST_SUCCESS = 0;
	public static final int NEW_POST_FAIL_WITHIN_THIRTY_SECONDS = 1;	//三十秒内发两个贴
	public static final int NEW_POST_FAIL_WITHIN_FIVE_MINUTES = 2;	//5分钟内发相同内容
	public static final int NEW_POST_FAIL_NOT_ENOUGH_KX = 3;		//看雪币不足
	
	//一些板块的id号
	public static final int HELP_FORUM_ID = 20;				//看雪求助问答版块的id
	//public static final int SOFTWARE_DEBUG_FORUM_ID = 4;	//软件调试版块的id
	public static final int GET_JOB_FORUM_ID = 47;			//考聘版块id
	public static final int POST_CONTENT_SIZE_MIN = 6;	//发帖或回帖的最小长度
	public static final int NEW_FORUM_ID = 153;				//新贴集合版块id
	public static final int LIFE_FORUM_ID = 45;			//生活放心情版块id
	
	//几种置顶类型
	public static final int GLOBAL_TOP_FORUM = -1;
	public static final int AREA_TOP_FORUM = 116;
	public static final int TOP_FORUM = 1;
	
	public static final int ALLOW_LOGIN_USERNAME_OR_PASSWD_ERROR_NUM = 5;
	
	private static Api mInstance = null;
	private String mToken = "guest";
	//TODO 使访问cookie时变得线程安全
	private CookieStorage mCookieStorage = null;
	private SharedPreferences mPreferences = null;
	
	public static Api getInstance() {
		if (mInstance == null) {
			mInstance = new Api();
		}
		return mInstance;
	}
	
	/**
	 * 设置context，初始化上下文相关的属性
	 * @param con 要设置的 context
	 */
	public void setmCon(Context con) {
		if (con == null)
			return;
		this.mPreferences = PreferenceManager.getDefaultSharedPreferences(con);
		this.mCookieStorage = new CookieStorage(new ObjStorage(this.mPreferences));
		this.getForumToken();
	}
	
	/**
	 * @return 看雪安全论坛的cookie存储管理类
	 */
	public CookieStorage getCookieStorage() {
		return this.mCookieStorage;
	}
	
	/**
	 * 获取securitytoken，securitytoken在做post操作时需要提交。处于登录状态得到正常token，非登录状态得到字符串guest
	 */
	private void getForumToken() {
		String url = DOMAIN + PATH + "getsecuritytoken.php?" + STYLE;
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_GET, new NetClientCallback() {

			@Override
			public void execute(int status, String response,
					List<Cookie> cookies) {
				
				if (status != HttpClientUtil.NET_SUCCESS)
					return;
				
				JSONObject obj = JSON.parseObject(response);
				mToken = obj.getString("securitytoken");
			}
			
		});
		if (this.isLogin()) {
			hcu.addCookie(this.mCookieStorage.getCookies());
		}
		hcu.asyncConnect();
	}
	
	
	/**
	 * securitytoken的setter接口
	 * @param token
	 */
	public void setToken(String token) {
		if (token == null)
			return;
		this.mToken = token;
	}
	
	/**
	 * @return 处于登录状态返回true，反之返回false
	 */
	public boolean isLogin() {
		return this.mCookieStorage.hasCookie("bbsessionhash");
	}

	/**
	 * 获取看雪的版块列表
	 * @param callback
	 */
	public void getForumHomePage(final NetClientCallback callback) {
		String url = DOMAIN + PATH + "index.php?" + STYLE;
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_GET, callback);
		if (this.isLogin()) {
			hcu.addCookie(this.mCookieStorage.getCookies());
		}
		hcu.asyncConnect();
	}
	
	/**
	 * 获取指定版块的主题列表
	 * @param id		版块id
	 * @param page		版块主题列表的页码
	 * @param callback
	 */
	public void getForumDisplayPage(int id, int page, final NetClientCallback callback) {
		String url = DOMAIN + PATH + "forumdisplay.php?" + STYLE + "&f=" + id + "&page=" + page + "&order=desc";
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_GET, callback);
		if (this.isLogin()) {
			hcu.addCookie(this.mCookieStorage.getCookies());
		}
		hcu.asyncConnect();
	}
	
	/**
	 * 获取指定主题中的帖子列表
	 * @param id		主题id
	 * @param page		主题帖子列表页码
	 * @param callback
	 */
	public void getForumShowthreadPage(int id, int page, final NetClientCallback callback) {
		String url = DOMAIN + PATH + "showthread.php?" + STYLE + "&t=" + id + "&page=" + page;
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_GET, callback);
		if (this.isLogin()) {
			hcu.addCookie(this.mCookieStorage.getCookies());
		}
		hcu.asyncConnect();
	}
	
	/**
	 * 获取完整的帖子内容。帖子内容较长时，看雪默认只传输缩略内容，可通过该接口获取完整内容。
	 * @param id		帖子id
	 * @param callback
	 */
	public void getForumFullThread(int id, final NetClientCallback callback) {
		String url = DOMAIN + PATH + "showpost.php?" + STYLE +"&p=" + id;
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_GET, callback);
		if (this.isLogin()) {
			hcu.addCookie(this.mCookieStorage.getCookies());
		}
		hcu.asyncConnect();
	}
	
	/**
	 * 登录看雪
	 * @param uname
	 * @param passwd
	 * @param callback
	 */
	public void login(String uname, String passwd, final NetClientCallback callback) {
		String url = DOMAIN + PATH + "login.php?do=login" + "&" + STYLE;
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_POST, callback);
		hcu.addParam("vb_login_username", uname);
		hcu.addParam("do", "login");
		hcu.addParam("cookieuser", "1");
		hcu.addParam("securitytoken", "guest");
		hcu.addParam("vb_login_md5password", SimpleHASH.md5(this.strToEnt(passwd.trim())));
		hcu.addParam("vb_login_md5password_utf", SimpleHASH.md5(passwd.trim()));
		hcu.asyncConnect();
	}
	
	/**
	 * 设置登录用户的个人信息
	 * @param username	登录用户用户名
	 * @param id		登录用户id
	 * @param isavatar	登录用户是否有头像
	 * @param email		登录用户email地址
	 */
	public void setLoginUserInfo(String username, int id, int isavatar, String email) {
		if (username == null)
			return;
		Editor editor = this.mPreferences.edit();
		editor.putString("username", username);
		editor.putInt("userid", id);
		editor.putInt("isavatar", isavatar);
		editor.putString("email", email);
		editor.commit();
	}
	
	/**
	 * @return	用户名
	 */
	public String getLoginUserName() {
		return this.mPreferences.getString("username", null);
	}
	
	/**
	 * @return 用户id
	 */
	public int getLoginUserId() {
		return this.mPreferences.getInt("userid", -1);
	}
	
	/**
	 * @return	用户有头像返回true
	 */
	public int getIsAvatar() {
		return this.mPreferences.getInt("isavatar", 0);
	}
	
	/**
	 * @return 用户email地址
	 */
	public String getEmail() {
		return this.mPreferences.getString("email", null);
	}
	
	/**
	 * 清除登录用户个人信息
	 */
	public void clearLoginData() {
		this.mCookieStorage.clearAll();
		Editor editor = this.mPreferences.edit();
		editor.remove("username");
		editor.remove("userid");
		editor.remove("isavatar");
		editor.commit();
	}

	/**
	 * 登出
	 * @param callback
	 */
	public void logout(final NetClientCallback callback) {
		String url = DOMAIN + PATH + "login.php?do=logout&logouthash=" + mToken + "&" + STYLE;
		new HttpClientUtil(url, HttpClientUtil.METHOD_GET, callback).asyncConnect();
	}
	
	/**
	 * 回复主题
	 * @param id		主题id
	 * @param msg		回复内容
	 * @param callback
	 */
	public void quickReply(int id, String msg, final NetClientCallback callback) {
		String url = DOMAIN + PATH + "newreply.php?" + STYLE;
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_POST, callback);
		hcu.addParam("message", msg);
		hcu.addParam("t", id + "");
		hcu.addParam("fromquickreply", "1");
		hcu.addParam("do", "postreply");
		hcu.addParam("securitytoken", mToken);
		hcu.addCookie(this.mCookieStorage.getCookies());
		hcu.asyncConnect();
	}
	
	/**
	 * 发布新主题
	 * @param id 		版块id
	 * @param subject	主题标题
	 * @param msg		主题内容
	 * @param callback
	 */
	public void newThread(int id, String subject, String msg, final NetClientCallback callback) {
		getNormalNewThread(id, subject, msg, callback).asyncConnect();
	}
	
	/**
	 * 发布带有悬赏kx币的新主题
	 * @param id
	 * @param subject
	 * @param kxReward		悬赏kx币数值
	 * @param msg
	 * @param callback
	 */
	public void newThread(int id, String subject, String kxReward, String msg, final NetClientCallback callback) {
		HttpClientUtil hcu = getNormalNewThread(id, subject, msg, callback);
		hcu.addParam("offer_Price", kxReward);
		hcu.asyncConnect();
	}
	
	private HttpClientUtil getNormalNewThread(int id, String subject, String msg, final NetClientCallback callback) {
		String url = DOMAIN + PATH + "newthread.php?do=postthread" + "&f=" + id + "&" + STYLE;
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_POST, callback);
		hcu.addParam("subject", subject);
		hcu.addParam("message", msg);
		hcu.addParam("securitytoken", mToken);
		hcu.addParam("f", "" + id);
		hcu.addParam("do", "postthread");
		hcu.addCookie(this.mCookieStorage.getCookies());
		return hcu;
	}
	
	/**
	 * 看雪的意见反馈接口
	 * @param name
	 * @param email
	 * @param msg
	 * @param callback
	 */
	public void feedback(String name, String email, String msg, final NetClientCallback callback) {
		String url = DOMAIN + PATH + "sendmessage.php?do=docontactus&" + STYLE;
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_POST, callback);
		hcu.addParam("name", name);
		hcu.addParam("email", email);
		hcu.addParam("message", msg);
		hcu.addParam("securitytoken", mToken);
		hcu.addParam("subject", "0");
		hcu.addParam("do", "docontactus");
		hcu.addCookie(this.mCookieStorage.getCookies());
		hcu.asyncConnect();
	}
	
	/**
	 * 检测新版本
	 * @param callback
	 */
	public void checkUpdate(NetClientCallback callback) {
		String url = DOMAIN + PATH + "mobile/android/appupdate.html";
		new HttpClientUtil(url, HttpClientUtil.METHOD_GET, callback).asyncConnect();
	}
	
	/**
	 * 检测指定版块下的主题列表是否有更新
	 * @param id		版块id
	 * @param time		上次刷新的时间戳
	 * @param callback
	 */
	public void checkNewPostInForumDisplayPage(int id, long time, final NetClientCallback callback) {
		String url = DOMAIN + PATH + "forumdisplay.php?f=" + id + "&getnewpost=" + time +"&" + STYLE;
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_GET, callback);
		if (this.isLogin()) {
			hcu.addCookie(this.mCookieStorage.getCookies());
		}
		hcu.asyncConnect();
	}
	
	/**
	 * 检测指定主题下的帖子列表是否有更新
	 * @param id		主题id
	 * @param time		上次刷新的时间戳
	 * @param callback
	 */
	public void checkNewPostInShowThreadPage(int id, long time, final NetClientCallback callback) {
		String url = DOMAIN + PATH + "showthread.php?t=" + id + "&getnewpost=" + time +"&" + STYLE;
		HttpClientUtil hcu = new HttpClientUtil(url, HttpClientUtil.METHOD_GET, callback);
		if (this.isLogin()) {
			hcu.addCookie(this.mCookieStorage.getCookies());
		}
		hcu.asyncConnect();
	}
	
	/**
	 * 获取看雪用户头像的url
	 * @param userId	用户id
	 * @return
	 */
	public String getUserHeadImageUrl(int userId) {
		return DOMAIN + PATH + "image.php?u=" + userId;
	}
	
	/**
	 * 获取看雪帖子中附件图片的url
	 * @param id		附件id
	 * @return
	 */
	public String getAttachmentImgUrl(int id) {
		return DOMAIN + PATH + "attachment.php?attachmentid=" + id + "&thumb=1&" + STYLE;
	}
	
	/**
	 * 登录前用户密码预处理
	 * @param input		去掉首位空格的用户密码
	 * @return
	 */
	private String strToEnt(String input) {
		String output = "";
		
		for (int i = 0; i < input.length(); i++) {
			int ucode = input.codePointAt(i);
			String tmp = "";
			
			if (ucode > 255) {
				while (ucode >= 1) {
					tmp = "0123456789".charAt(ucode%10) + tmp;
					ucode /= 10;
				}
				
				if (tmp == "") {
					tmp = "0";
				}
				
				tmp = "#" + tmp;
				tmp = "&" + tmp;
				tmp = tmp + ";";
				output += tmp;
			}else {
				output += input.charAt(i);
			}
		}
		return output;
	}
}
