package com.mgamerzproductions.logthislib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class LogCollector {
	private static String TAG = "logthislib";
	private Context context;
	/**
	 * @return
	 */
	private String buildLogString(Context context) {
		Log.w(TAG, "Building Log Command.");
		String logdata = null; // The value returned and placed into the message
		Process mLogcatProc = null;
		BufferedReader reader = null;
		String command = "logcat -d";

		StringBuilder log = new StringBuilder();
		String line;

		String separator = "\n";
		log.append(separator + separator + separator
				+ "System Environment Information----" + separator
				+ "Device Brand: ");
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
		log.append(separator + "LogCat of the device: ");

		try {
			mLogcatProc = Runtime.getRuntime().exec(command);

			reader = new BufferedReader(new InputStreamReader(
					mLogcatProc.getInputStream()));

			while ((line = reader.readLine()) != null) {
				log.append(line);
				log.append(separator);
				// break;

			}
		} catch (IOException e) {
			Log.e(TAG, "IOEXCEPTION AT RUNTIME LOG GRAB!", e);
			Toast.makeText(context, "IOException getting logs",
					Toast.LENGTH_LONG).show();
		}
		Log.i(TAG, "Log built.");
		logdata = log.toString();

		Log.d(TAG, "Completed buildlogstring, logdata null? "
				+ (logdata == null));
		return logdata;
	}
	
	public void getLogs(Context context){
		this.context = context;
		new BuildLogTask().execute();
	}

	private class BuildLogTask extends AsyncTask<Void, String, String> {
		ProgressDialog progressdialog;
		String logdata;
		Context context;

		@Override
		protected String doInBackground(Void ... params) {
			
			logdata = buildLogString(context);
			if (logdata == null) {
				cancel(true); // abort
			}
			
			String response = postDataToPastebin(context, logdata);
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE); 
		    android.content.ClipData clip = android.content.ClipData.newPlainText("logcat log", response);
		    clipboard.setPrimaryClip(clip);
			return response;
		}

		protected void onProgressUpdate(String... progress) {
			progressdialog.setMessage(progress[0]);
		}

		@Override
		protected void onPreExecute() {
			Log.w(TAG, "Showing Dialog");
			//callingView.setEnabled(false);
			progressdialog = new ProgressDialog(context);
			progressdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressdialog.setMessage("Gathering logs");
			progressdialog.setCancelable(false);
			progressdialog
					.setOnDismissListener(new DialogInterface.OnDismissListener() {

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
			//callingView.setEnabled(true);
			logdata = null;
		}

		@Override
		protected void onPostExecute(String result) {
			progressdialog.dismiss();
			Toast.makeText(context, "Logs posted to pastebin, link copied to clipboard", Toast.LENGTH_LONG).show();
			//callingView.setEnabled(true);
		}
	}

	public String postDataToPastebin(Context context, String pasteData) {
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://pastebin.com/api/api_post.php");
		String responseBody = context.getString(R.string.pasteFailedText);

		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("api_dev_key",
					encodeUTF8("3e06ecc889a1a816bbcb9fed03631b84"))); // my Dev
																		// Key
			nameValuePairs.add(new BasicNameValuePair("api_option",
					encodeUTF8("paste"))); // TODO Add more options. Possibly
											// move to it's own screen.
			nameValuePairs.add(new BasicNameValuePair("api_paste_name",
					encodeUTF8("Untitled"))); // paste name
			nameValuePairs.add(new BasicNameValuePair("api_paste_expire_date",
					encodeUTF8("1M"))); // paste time
			nameValuePairs.add(new BasicNameValuePair("api_paste_code",
					encodeUTF8(pasteData)));
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
