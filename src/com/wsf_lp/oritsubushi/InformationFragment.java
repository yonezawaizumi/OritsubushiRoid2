package com.wsf_lp.oritsubushi;

import java.lang.ref.WeakReference;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class InformationFragment extends Fragment {

	private static class MyWebViewClient extends WebViewClient {
		private final WeakReference<Fragment> mFragment;
		private final String mMyUrl;

		private MyWebViewClient(Fragment fragment, String myUrl) {
			mFragment = new WeakReference<Fragment>(fragment);
			mMyUrl = myUrl;
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if(mMyUrl.equals(url)) {
				return false;
			} else if (mFragment.get() != null){
				mFragment.get().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				return true;
			} else {
				return false;
			}
		}
	}

	private static class HttpResponseHandler extends AsyncHttpResponseHandler {
		private WeakReference<InformationFragment> mFragment;
		public HttpResponseHandler(InformationFragment fragment) {
			mFragment = new WeakReference<InformationFragment>(fragment);
		}
		@Override
		public void onSuccess(String response) {
			InformationFragment fragment = mFragment.get();
			if(fragment != null) {
				fragment.onLoadSuccess(response);
			}
		}
		@Override
		public void onFailure(Throwable error, String response) {
			InformationFragment fragment = mFragment.get();
			if(fragment != null) {
				fragment.onLoadFailure(response);
			}
		}
	}

	AsyncHttpClient mHttpClient = new AsyncHttpClient();
	Button mReloadButton;
	WebView mWebView;
	String mMyUrl;
	String mContent;
	String mContentType;
	boolean mIsLoading;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mMyUrl = getString(R.string.information_url);
		load();
	}

	private void setContent() {
		if(mContent != null) {
			mWebView.loadDataWithBaseURL(mMyUrl, mContent, mContentType, "UTF-8", null);
		}
	}

	private void onLoadFinish(String content, String contentType) {
		this.mContent = content;
		this.mContentType = contentType;
		if(mWebView != null) {
			mIsLoading = false;
			mReloadButton.setEnabled(true);
			setContent();
		}
	}

	public void onLoadSuccess(String response) {
		onLoadFinish(response, "text/html");
	}

	public void onLoadFailure(String response) {
		onLoadFinish(getString(R.string.information_load_error), "text/plain");
	}

	private void load() {
		if(!mIsLoading) {
			mIsLoading = true;
			mHttpClient.get(mMyUrl, new HttpResponseHandler(this));
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.info, container, false);

		mReloadButton = (Button)view.findViewById(R.id.button_reload);
		mReloadButton.setEnabled(false);
		mWebView = (WebView)view.findViewById(R.id.web_view);
		mReloadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mReloadButton.setEnabled(false);
				load();
			}
		});
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new MyWebViewClient(this, mMyUrl));

		return view;
	}

	@Override
	public void onResume() {
		if(!mIsLoading) {
			mReloadButton.setEnabled(true);
			setContent();
		}
		super.onResume();
	}

	@Override
	public void onDestroyView() {
		mReloadButton = null;
		mWebView = null;
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		if(mIsLoading) {
			mHttpClient.cancelRequests(getActivity(), true);
		}
		super.onDestroy();
	}
}
