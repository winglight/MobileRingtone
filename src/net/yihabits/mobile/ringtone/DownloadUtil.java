package net.yihabits.mobile.ringtone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.yihabits.mobile.ringtone.R;
import net.yihabits.mobile.ringtone.db.RingtoneModel;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.google.api.translate.Language;
import com.google.api.translate.Translate;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;

public class DownloadUtil {

	private MobileRingtoneActivity activity;
	private String download_url = "http://file.mozhao.net:86/down.php?aid=";
	public static MyAudioPlayer player;

	private static HashMap<String, ArrayList<RingtoneModel>> listPageCache = new HashMap<String, ArrayList<RingtoneModel>>();
	private static HashMap<String, String> redirectUrl = new HashMap<String, String>();

	public static final int PICK_CONTACT = 1;

	public DownloadUtil(MobileRingtoneActivity activity) {
		this.activity = activity;
	}

	public void runSaveUrl(final int position, final int menuId,
			final String ringtoneUrl) {
		final String dir = initBaseDir() + "/";

		Runnable saveUrl = new Runnable() {
			public void run() {
				saveUrl(ringtoneUrl, dir, menuId, position);

				// notify the activity refresh progress and button

			}
		};
		new Thread(saveUrl).start();
	}

	private void saveUrl(String url, String dir, int id, int position) {
		HttpEntity resEntity = null;
		String path = dir;
		int used = 1;

		RingtoneModel rm = this.activity.getRingtoneByUrl(url);
		if (rm != null && rm.getLocation() != null) {
			path = rm.getLocation();
		} else {
			try {
				

				this.activity.toastMsg(R.string.startDownload);

				ApplicationEx app = (ApplicationEx) this.activity
						.getApplication();

				HttpClient httpclient = app.getHttpClient();

				HttpGet httpget = new HttpGet(url);

				HttpResponse response = httpclient.execute(httpget);

				int status = response.getStatusLine().getStatusCode();

				if (status == HttpStatus.SC_OK) {

					resEntity = response.getEntity();
					Header tmp = response.getFirstHeader("Content-Disposition");

					String fileName = "";
					if (tmp != null) {
						String head = tmp.getValue();
						int start = head.indexOf("filename=");
						if (start > 0) {
							head = new String(head.getBytes("iso8859-1"),
									"gb2312");
							fileName = head.substring(start + 10,
									head.lastIndexOf("\""));
							start = fileName.indexOf("-");
							if (start > 0) {
								fileName = fileName.substring(start + 1);
							}
						}
					}

					this.activity.toastMsg(R.string.stopDownload);

					path += fileName;

					// 1. save to sdcard
					boolean flag = save2card(
							EntityUtils.toByteArray(resEntity), path);

					// 2. save new record to db
					if (flag) {
						this.activity.updateRingtoneLocation(position, path);
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {

				if (resEntity != null) {
					try {
						resEntity.consumeContent();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}
		// 3.deal music by menu item
		switch (id) {
		case R.id.menu_play_ringtone: {
			if (player != null) {
				player.end();
			}
			player = new MyAudioPlayer(this.activity, new File(path));
			player.go();
			((MobileRingtoneActivity) this.activity).enableBtn();
			break;
		}
		case R.id.menu_set_ringtone: {
			setRingtone(path, false);
			DownloadUtil.this.activity.toastMsg(R.string.setRingtoneSuccess);
			used = 2;
			break;
		}
		case R.id.menu_set_ringtone_contact: {
			String ringtone = insertRingtone(path);
			this.activity.setSelected_ringtone(ringtone);
			// popup selector
			Intent intent = new Intent(Intent.ACTION_PICK,
					ContactsContract.Contacts.CONTENT_URI);
			this.activity.startActivityForResult(intent, PICK_CONTACT);

			used = 2;
			
			break;
		}
		case R.id.menu_set_message_ringtone: {
			setRingtone(path, true);
			DownloadUtil.this.activity.toastMsg(R.string.setRingtoneSuccess);
			break;
		}
		case R.id.menu_send_ringtone_mms: {
			Intent sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.putExtra("sms_body",
					DownloadUtil.this.activity.getString(R.string.sendMmsBody));
			sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
			sendIntent.setType("audio/mp3");
			DownloadUtil.this.activity.startActivity(sendIntent);
			break;
		}
		default: {

		}
		
		
		
		}
		
		//save used into db
		this.activity.updateRingtoneUsed(position, used);

	}

	
	public ArrayList<RingtoneModel> getRingtoneByCategoryLanguae(int category, int language,
			int order, int page) {
		// compose url
		String url = "http://www.350c.com/ring.php?ff=" + category + "&rank=";
		if (order == 0) {
			url += "date";
		} else {
			url += "hot";
		}
		if (language > 0) {
			url += "date";
		} 
		if (page > 0) {
			url += "&language=" + language;
		}

		// if cached, get the list directly
		if (listPageCache.containsKey(url)) {
			return listPageCache.get(url);
		}

		// get the list page
		HttpEntity resEntity = null;
		ArrayList<RingtoneModel> resList = new ArrayList<RingtoneModel>();

		try {

			ApplicationEx app = (ApplicationEx) this.activity.getApplication();

			HttpClient httpclient = app.getHttpClient();

			HttpGet httpget = new HttpGet(url);

			HttpResponse response = httpclient.execute(httpget);

			int status = response.getStatusLine().getStatusCode();

			if (status == HttpStatus.SC_OK) {

				resEntity = response.getEntity();
				String content = EntityUtils.toString(resEntity);
				Source source = new Source(content);

				// parse list
				Element tele = source.getFirstElement(HTMLElementName.TABLE);
				List<Element> tlist = tele.getAllElements(HTMLElementName.A);
				for (Element ele : tlist) {

					String href = ele.getAttributeValue("href");
					if (href != null && href.startsWith("/file/")) {
						String id = href.substring(href.indexOf("-") + 1,
								href.indexOf(".html"));
						RingtoneModel rm = new RingtoneModel();
						rm.setUrl(download_url + id);
						String name = ele.getTextExtractor().toString().trim();
						rm.setName(new String(name.getBytes("iso8859-1"),
								"gb2312"));
						// rm.setNameEn(translate(name));

						// insert new ringtones into db
						rm = this.activity.updateOrInsertRingtone(rm);

						resList.add(rm);
					}

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (resEntity != null) {
				try {
					resEntity.consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// put the list into cache
		listPageCache.put(url, resList);

		return resList;
	}
	
	public ArrayList<RingtoneModel> getRingtoneByKeyword(String keyword, int page) {
		// compose url
		String url = "";
		if(redirectUrl.containsKey(keyword)){
			String tmp = redirectUrl.get(keyword);
			url = "http://www.350c.com" + tmp.substring(0, tmp.lastIndexOf("_") + 1) + page + ".html";
		}else{
		try {
			url = "http://www.350c.com/search.php?keyword=" + URLEncoder.encode(keyword, "gb2312") + "&radio=1";
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (page > 1) {
			url += "&page=" + page;
		}
		}

		// if cached, get the list directly
		if (listPageCache.containsKey(url)) {
			return listPageCache.get(url);
		}

		// get the list page
		HttpEntity resEntity = null;
		ArrayList<RingtoneModel> resList = new ArrayList<RingtoneModel>();

		try {

			ApplicationEx app = (ApplicationEx) this.activity.getApplication();

			HttpClient httpclient = app.getHttpClient();

			HttpGet httpget = new HttpGet(url);

			HttpContext context = new BasicHttpContext(); 
	        HttpResponse response = httpclient.execute(httpget, context); 

			int status = response.getStatusLine().getStatusCode();

			if (status == HttpStatus.SC_OK) {
				
				//deal with redirect
				HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute( 
		                ExecutionContext.HTTP_REQUEST);
		        HttpHost currentHost = (HttpHost)  context.getAttribute( 
		                ExecutionContext.HTTP_TARGET_HOST);
		        String currentUrl = currentHost.toURI() + currentReq.getURI();
		        if(!currentUrl.equals(url) && !redirectUrl.containsKey(keyword)){
		        	redirectUrl.put(keyword, currentReq.getURI().toString());
		        }

				resEntity = response.getEntity();
				String content = EntityUtils.toString(resEntity);
				
				//truncate the useless content
				int start = content.indexOf("<img src=\"/images/play.gif");
				content = content.substring(start);
				int end = content.indexOf("</table>");
				content = content.substring(0, end);
				
				Source source = new Source(content);

				// parse list
				List<Element> tlist = source.getAllElements(HTMLElementName.A);
				for (Element ele : tlist) {

					String href = ele.getAttributeValue("href");
					if (href != null && href.startsWith("/file/")) {
						String id = href.substring(href.indexOf("-") + 1,
								href.indexOf(".html"));
						RingtoneModel rm = new RingtoneModel();
						rm.setUrl(download_url + id);
						String name = ele.getTextExtractor().toString().trim();
						rm.setName(new String(name.getBytes("iso8859-1"),
								"gb2312"));
						// rm.setNameEn(translate(name));

						// insert new ringtones into db
						rm = this.activity.updateOrInsertRingtone(rm);

						resList.add(rm);
					}

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (resEntity != null) {
				try {
					resEntity.consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// put the list into cache
		listPageCache.put(url, resList);

		return resList;
	}

	private boolean save2card(byte[] bytes, String path) {
		try {
			// save to sdcard
			FileOutputStream fos = new FileOutputStream(new File(path));
			IOUtils.write(bytes, fos);

			// release all instances
			fos.flush();
			fos.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public String initBaseDir() {
		File sdDir = Environment.getExternalStorageDirectory();
		File uadDir = null;
		if (sdDir.exists() && sdDir.canWrite()) {

		} else {
			sdDir = Environment.getDataDirectory();

		}
		uadDir = new File(sdDir.getAbsolutePath() + "/ringtone/");
		if (!uadDir.exists()) {
			uadDir.mkdirs();
		}
		return uadDir.getAbsolutePath();
	}

	public String getDataDir() {
		String dir = Environment.getDataDirectory() + "/ringtone/";
		File iDir = new File(dir);
		if (!iDir.exists()) {
			iDir.mkdirs();
		}
		return dir;
	}

	private void setRingtone(String path, boolean ismsg) {
		ContentValues values = new ContentValues();

		values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
		values.put(MediaStore.Audio.Media.IS_RINGTONE, !ismsg);
		values.put(MediaStore.Audio.Media.IS_NOTIFICATION, ismsg);
		values.put(MediaStore.Audio.Media.IS_ALARM, false);
		values.put(MediaStore.Audio.Media.IS_MUSIC, false);

		Uri uri;

		File k = new File(path);
		String dir = getDataDir();
		File newFile = new File(dir + "/"
				+ activity.getString(R.string.app_name) + ".mp3");
		try {
			IOUtils.copy(new FileReader(k), new FileWriter(newFile));

			values.put(MediaStore.MediaColumns.DATA, newFile.getAbsolutePath());
			values.put(MediaStore.MediaColumns.TITLE, newFile.getName());
			values.put(MediaStore.MediaColumns.SIZE, newFile.length());

			// Insert it into the database
			uri = MediaStore.Audio.Media.getContentUriForPath(k
					.getAbsolutePath());
			// if exists, delete it
			this.activity.getContentResolver().delete(
					uri,
					MediaStore.MediaColumns.DATA + "=\"" + k.getAbsolutePath()
							+ "\"", null);
		} catch (Exception e) {
			values.put(MediaStore.MediaColumns.DATA, k.getAbsolutePath());
			values.put(MediaStore.MediaColumns.TITLE, k.getName());
			values.put(MediaStore.MediaColumns.SIZE, k.length());

			// Insert it into the database
			uri = MediaStore.Audio.Media.getContentUriForPath(k
					.getAbsolutePath());
			// if exists, delete it
			this.activity.getContentResolver().delete(
					uri,
					MediaStore.MediaColumns.DATA + "=\"" + k.getAbsolutePath()
							+ "\"", null);
		}

		// create a new one
		Uri newUri = this.activity.getContentResolver().insert(uri, values);

		if (ismsg) {
			RingtoneManager.setActualDefaultRingtoneUri(this.activity,
					RingtoneManager.TYPE_NOTIFICATION, newUri);
		} else {
			RingtoneManager.setActualDefaultRingtoneUri(this.activity,
					RingtoneManager.TYPE_RINGTONE, newUri);
		}
	}

	private String insertRingtone(String path) {
		File k = new File(path);

		ContentValues values = new ContentValues();

		values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
		values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
		values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
		values.put(MediaStore.Audio.Media.IS_ALARM, false);
		values.put(MediaStore.Audio.Media.IS_MUSIC, false);

		values.put(MediaStore.MediaColumns.DATA, k.getAbsolutePath());
		values.put(MediaStore.MediaColumns.TITLE, k.getName());
		values.put(MediaStore.MediaColumns.SIZE, k.length());

		// Insert it into the database
		Uri uri = MediaStore.Audio.Media.getContentUriForPath(k
				.getAbsolutePath());
		// if exists, delete it
		this.activity.getContentResolver().delete(
				uri,
				MediaStore.MediaColumns.DATA + "=\"" + k.getAbsolutePath()
						+ "\"", null);

		// create a new one
		Uri newUri = this.activity.getContentResolver().insert(uri, values);

		RingtoneManager.setActualDefaultRingtoneUri(this.activity,
				RingtoneManager.TYPE_RINGTONE, newUri);

		return newUri.toString();
	}

	public String translate(String src) {
		Translate.setHttpReferrer("http://www.omdasoft.com");

		String translatedText;
		try {
			translatedText = Translate.execute(src, Language.CHINESE,
					Language.ENGLISH);
		} catch (Exception e) {
			translatedText = src;
		}

		return translatedText;
	}
}
