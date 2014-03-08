package com.mgamerzproductions.logthislib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

public class LogCollector {
	private static String TAG = "logthislib";
	private Context context;
	private String api_key;
	private String user_key;
	private String packagetag;

	/**
	 * Constructor for getting logs. It takes a package name, an api_key, and
	 * optionally a user identifier key if you want to make private pastes.
	 * 
	 * @param packagetag
	 *            package to read logs for (you must own this package)
	 * @param api_key
	 *            pastebin posting api key
	 * @param user_key
	 *            pastebin api user session key
	 */
	public LogCollector(String packagetag, String api_key, String user_key) {
		this.api_key = api_key;
		this.user_key = user_key;
		this.packagetag = packagetag;
	}

	/**
	 * @return
	 */
	private String buildLogString(Context context) {
		Log.w(TAG, "Building Log Command.");
		String logdata = null; // The value returned and placed into the message
		Process mLogcatProc = null;
		BufferedReader reader = null;
		// String cmd = "logcat -d -v long";
		String[] cmd = { "sh", "-c", "logcat -d -v long" };

		// String command = "logcat -d | grep " + packagetag;

		StringBuilder log = new StringBuilder();
		String line;

		String separator = "\n";
		log.append("Log generated on ");
		log.append((DateFormat.format("dd-MM-yyyy hh:mm:ss", new java.util.Date()).toString()));

		log.append(separator + "System Environment Information----" + separator + "Device Brand: ");
		log.append(Build.BRAND);
		log.append(separator + "Device Name: ");
		log.append(Build.MODEL);
		log.append(separator + "OS Version: ");
		log.append(Build.VERSION.RELEASE);
		log.append(separator + "Product: ");
		log.append(Build.PRODUCT);
		log.append(separator + "Board: ");
		log.append(Build.BOARD);
		log.append(separator);
		
		log.append("Logging package " + packagetag + separator);
		PackageInfo pInfo;
		try {
			pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			log.append("App version "+pInfo.versionName+", code: "+pInfo.versionCode);
		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			log.append("Couldn't read the package info");
			e1.printStackTrace();
		}
		log.append(separator);
		
		log.append(separator + "Logcat of the device: " + separator);
		

		try {
			mLogcatProc = Runtime.getRuntime().exec(cmd);
			// Log.w(TAG, "Logging command was "+mLogcatProc.);
			reader = new BufferedReader(new InputStreamReader(mLogcatProc.getInputStream()));

			while ((line = reader.readLine()) != null) {
				// this check is for ICS - so we don't collect unnecessary logs
				if (Build.VERSION.SDK_INT < 16) {
					if (line.contains(packagetag)) {
						log.append(line);
						log.append(separator);
					}
				} else {
					log.append(line);
					log.append(separator);
				}
			}
		} catch (IOException e) {
			Log.e(TAG, "IOEXCEPTION AT RUNTIME LOG GRAB!", e);
			// Toast.makeText(context, "IOException getting logs",
			// Toast.LENGTH_LONG).show();
		}
		try {
			mLogcatProc.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i(TAG, "Log built.");
		logdata = log.toString();

		Log.d(TAG, "Completed buildlogstring, logdata null? " + (logdata == null));
		return logdata;
	}

	public void getLogs(Context context) {
		this.context = context;
		new BuildLogTask().execute();
	}

	private class BuildLogTask extends AsyncTask<Void, String, String> {
		ProgressDialog progressdialog;
		String logdata;

		@Override
		protected String doInBackground(Void... params) {

			logdata = buildLogString(context);
			if (logdata == null) {
				cancel(true); // abort
			}

			String response = postDataToPastebin(context, logdata);
			return response;
		}

		protected void onProgressUpdate(String... progress) {
			progressdialog.setMessage(progress[0]);
		}

		@Override
		protected void onPreExecute() {
			Log.w(TAG, "Showing Dialog");
			// callingView.setEnabled(false);
			progressdialog = new ProgressDialog(context);
			progressdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressdialog.setMessage("Gathering logs");
			progressdialog.setCancelable(false);
			progressdialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					Log.e(TAG, "Progress Dialog dismissal.");

				}
			});
			progressdialog.show();
		}

		@Override
		protected void onCancelled() {
			Log.e(TAG, "Build Log Operation Aborted.");
			progressdialog.dismiss();
			// callingView.setEnabled(true);
			logdata = null;
		}

		@Override
		protected void onPostExecute(String result) {
			progressdialog.dismiss();
			try {
				URL url = new URL(result);
				// valid url, not an exception
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
				android.content.ClipData clip = android.content.ClipData.newPlainText("logcat log", result);
				clipboard.setPrimaryClip(clip);
				Toast.makeText(context, "Logs posted to pastebin, link copied to clipboard", Toast.LENGTH_LONG).show();
				return;
			} catch (MalformedURLException e) { /* invalid URL */
				Toast.makeText(context, "An error occured: " + result, Toast.LENGTH_LONG).show();
				Log.i(TAG, result);
			}

			// callingView.setEnabled(true);
		}
	}

	public String postDataToPastebin(Context context, String pasteData) {
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://pastebin.com/api/api_post.php");
		// HttpPost httppost = new
		// HttpPost("http://pastebin.com/api/api_login.php");

		String responseBody = context.getString(R.string.pasteFailedText);

		try {
			// Add your data
			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(7);

			nameValuePairs.add(new BasicNameValuePair("api_dev_key", encodeUTF8(api_key)));
			nameValuePairs.add(new BasicNameValuePair("api_user_key", encodeUTF8(user_key)));
			nameValuePairs.add(new BasicNameValuePair("api_option", encodeUTF8("paste")));
			nameValuePairs.add(new BasicNameValuePair("api_paste_name", encodeUTF8("BugReport Log")));
			nameValuePairs.add(new BasicNameValuePair("api_paste_private", encodeUTF8("2")));
			nameValuePairs.add(new BasicNameValuePair("api_paste_expire_date", encodeUTF8("1M")));
			nameValuePairs.add(new BasicNameValuePair("api_paste_code", encodeUTF8(pasteData)));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			responseBody = httpclient.execute(httppost, responseHandler);
			Log.e(TAG, responseBody);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return responseBody;
	}

	private String encodeUTF8(String textToEncode) {
		byte[] utf8Bytes;
		String result = "Encoding Failed.";
		try {
			utf8Bytes = textToEncode.getBytes("UTF8");
			result = new String(utf8Bytes, "UTF8");
		} catch (UnsupportedEncodingException e) {
			// This is a valid encoding scheme
			Log.e(TAG, "Invalid Encoding Scheme...");
			e.printStackTrace();
		}

		return result;
	}
}
