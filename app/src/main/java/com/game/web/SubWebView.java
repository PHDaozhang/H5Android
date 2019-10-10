package com.game.web;

import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class SubWebView {
	private static final String TAG = "SubWebView";
	private static final int CHOOSE_REQUEST_CODE = 0x9011;
	 
	private ValueCallback<Uri> mUploadFile = null;
	private ValueCallback<Uri[]> mUploadFiles = null;

	private MainActivity mMainActivity = null;

	private ViewGroup mParentView = null;
	private TextView mTitleView = null;
	private WebView mWebView = null;
	private FrameLayout mRootLayout = null;
	private LinearLayout mBackLayout = null;

	private boolean mNeedAttach = false;
	private String mUrl = "";
	private boolean mUseSdk = false;
	private AndroidBug5497Workaround mWorkaround = null;

	public SubWebView(MainActivity activity, FrameLayout layout) {
		Log.d(TAG, "SubWebView init");

		mMainActivity = activity;
		mParentView = layout;

		mRootLayout = new FrameLayout(mMainActivity);
		mRootLayout.setBackgroundColor(Color.rgb(0, 0, 0));
		mRootLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		mParentView.addView(mRootLayout);

		mBackLayout = new LinearLayout(mMainActivity);
		mBackLayout.setOrientation(LinearLayout.VERTICAL);
		mBackLayout.setBackgroundColor(Color.rgb(62, 62, 64));
		mBackLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		mRootLayout.addView(mBackLayout);

		mWorkaround = new AndroidBug5497Workaround(mMainActivity, mBackLayout);
		
		if (mWebView != null)
		{
			Log.d(TAG, "WebView destroy");
			try {
				mWebView.stopLoading();
				mWebView.destroy();
			}
			catch (Exception e) {
				Log.i(TAG, e.getMessage());
			}

			mWebView = null;
		}
	}
	
	public void open(String jsonString)
	{
		NavigationBarUtil.showNavigationBar(mMainActivity.getWindow());
		Log.d(TAG, "SubWebView open " + jsonString);
		final JSONObject object = JSON.parseObject(jsonString);
		boolean showBar = false;
		if (object.containsKey("showbar"))
		{
			showBar = object.getBoolean("showbar");
		}

		if (mWebView == null) {
			createLayout(showBar);
		}

		if (object.containsKey("title"))
		{
			String title = object.getString("title");
			Log.d(TAG, String.format("title", title));
			if (mTitleView != null)
				mTitleView.setText(title);
		}

		if (object.containsKey("orientation"))
		{
			String orientation = object.getString("orientation");
			if (orientation.equals("portait")) {
				mMainActivity.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
			else if (orientation.equals("landscape"))
			{
				mMainActivity.setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			}
			else if (orientation.equals("sensorlandscape"))
			{
				mMainActivity.setOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
			}
			else if (orientation.equals("sensor"))
			{
				mMainActivity.setOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			}
		}
		if (object.containsKey("usesdk"))
		{
			mUseSdk = object.getBoolean("usesdk");
		}
		mNeedAttach = true;
		mUrl = object.getString("url");

		loadUrl();
	}
	
	public void close()
	{
		NavigationBarUtil.hideNavigationBar(mMainActivity.getWindow());
		Log.d(TAG, "close");
		if (mWebView != null) {
			mWorkaround.detach();

			mParentView.removeView(mRootLayout);
			mRootLayout = null;

			mNeedAttach = false;
			mWebView.stopLoading();
			mWebView.destroy();
			mWebView = null;
			mTitleView = null;

			mMainActivity.resetOrientation();
		}
	}
	
	private void loadUrl()
	{
		Log.d(TAG, String.format("url=%s", mUrl));

		String url = mUrl;
		if (mUseSdk) {
			url = MainLoigc.getSingleton().convertURL(url);
		}
		Log.d(TAG, url);
		mWebView.loadUrl(url);
	}

	public void checkAttach()
	{
		if (mNeedAttach){
			mNeedAttach = false;
			mWorkaround.attach();
		}
	}

	private void onAlertError()
	{
		AlertDialog.Builder builder=new AlertDialog.Builder(mMainActivity);
		builder.setTitle("提示");
		builder.setMessage("页面加载失败,是否重新加载?");
		builder.setPositiveButton("是",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						loadUrl();
					}
				});
		builder.setNegativeButton("否",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						MainLoigc.getSingleton().closeSubWebView();
					}
				});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	public int sp2px(float spValue) {
		final float fontScale = mMainActivity.getResources().getDisplayMetrics().scaledDensity;
		return (int)(spValue * fontScale + 0.5f);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, String.format("requestCode=%d, resultCode=%d", requestCode, resultCode));
		
	    if (resultCode == Activity.RESULT_OK) {
	        switch (requestCode) {
	            case CHOOSE_REQUEST_CODE:
	                if (null != mUploadFile) {
	                    Uri result = (data == null || resultCode != Activity.RESULT_OK) ? null : data.getData();
						mUploadFile.onReceiveValue(result);
						mUploadFile = null;
	                }
					if (null != mUploadFiles) {
						Uri result = data == null || resultCode != Activity.RESULT_OK ? null
								: data.getData();
						mUploadFiles.onReceiveValue(new Uri[]{result});
						mUploadFiles = null;
					}
	                break;
	            default:
	                break;
	        }
	    } else if (resultCode == Activity.RESULT_CANCELED) {
	        if (null != mUploadFile) {
				mUploadFile.onReceiveValue(null);
				mUploadFile = null;
	        }
			if (null != mUploadFiles) {
				mUploadFiles.onReceiveValue(null);
				mUploadFiles = null;
			}
	    }
	} 

	public WebView createWebView(Context context) {
		WebView webView = new WebView(context);
		webView.setWebContentsDebuggingEnabled(true);
		
        WebSettings localWebSettings = webView.getSettings();
        localWebSettings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new JSModel(), "nativeInterface");
        
        localWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT); 
        localWebSettings.setDomStorageEnabled(true);
        
        webView.setWebViewClient(new SubWebView.SubWebViewClient());
        webView.setWebChromeClient(new SubWebView.SubWebChromeClient());
		
		return webView;
	}
	
	public void createLayout(boolean showBar)
	{
		if (showBar) 
		{
			FrameLayout titleLayout = new FrameLayout(mMainActivity);
			titleLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			mBackLayout.addView(titleLayout);
			
			mTitleView = new TextView(mMainActivity);
			FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, sp2px(50));
			titleParams.gravity = Gravity.CENTER;
			mTitleView.setLayoutParams(titleParams);
			mTitleView.setGravity(Gravity.CENTER);
			mTitleView.setTextSize(24);
			mTitleView.setTextColor(Color.rgb(255, 255, 255));
			titleLayout.addView(mTitleView);

			ImageButton leftButton = new ImageButton (mMainActivity);
			leftButton.setScaleType(ScaleType.CENTER_INSIDE);
			leftButton.setBackgroundColor(Color.rgb(62, 62, 64));
			leftButton.setImageResource(R.drawable.left_arrow);
			FrameLayout.LayoutParams rightParams = new FrameLayout.LayoutParams(sp2px(50), sp2px(50));
			rightParams.gravity = Gravity.LEFT;
			leftButton.setLayoutParams(rightParams);
			leftButton.setOnClickListener(new OnClickListener() {
	            @Override
	            public void onClick(View v) {
	            	Log.d(TAG, "exit");
					MainLoigc.getSingleton().closeSubWebView();
	            }
	        });
			titleLayout.addView(leftButton);
		}

		mWebView = createWebView(mMainActivity);
		
        LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(
        		LinearLayout.LayoutParams.MATCH_PARENT,
        		LinearLayout.LayoutParams.MATCH_PARENT);
		mBackLayout.addView(mWebView, webViewParams);
	}
	
	class SubWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
			
			Log.d(TAG, "onProgressChanged " + newProgress);
			if (newProgress == 100) {
				SubWebView.this.checkAttach();
			}
        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message, final JsResult result) {
            return super.onJsBeforeUnload(view, url, message, result);               
        }
        
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
			Log.d(TAG, "onJsAlert");
        	return super.onJsAlert(view, url, message, result);
        }

		@Override
		public void onPermissionRequest(PermissionRequest request) {
			//                super.onPermissionRequest(request);//必须要注视掉
			request.grant(request.getResources());
		}

		// For Android 3.0+
		public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType) {
			mUploadFile = uploadFile;
			openFileChooseProcess();
		}

		// For Android < 3.0
		public void openFileChooser(ValueCallback<Uri> uploadFile) {
			mUploadFile = uploadFile;
			openFileChooseProcess();
		}

		// For Android  > 4.1.1
//    @Override
		public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
			mUploadFile = uploadFile;
			openFileChooseProcess();
		}

		// For Android  >= 5.0
		@Override
		public boolean onShowFileChooser(WebView webView,
										 ValueCallback<Uri[]> uploadFiles,
										 WebChromeClient.FileChooserParams fileChooserParams) {
			mUploadFiles = uploadFiles;
			openFileChooseProcess();
			return true;
		}

		private void openFileChooseProcess() {
			Intent i = new Intent(Intent.ACTION_GET_CONTENT);
			i.addCategory(Intent.CATEGORY_OPENABLE);
			i.setType("image/*");
			mMainActivity.startActivityForResult(Intent.createChooser(i, "Choose"), CHOOSE_REQUEST_CODE);
		}
	}
	
    class SubWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.d(TAG, "shouldOverrideUrlLoading " + url);
			return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
        	Log.d(TAG, "onPageFinished " + url);
			super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String url) {
        	Log.d(TAG, String.format("onReceivedError %d %s", errorCode, url));
			super.onReceivedError(view, errorCode, description, url);
			SubWebView.this.onAlertError();
        }
    }
}
