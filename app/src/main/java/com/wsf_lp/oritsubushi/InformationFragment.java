package com.wsf_lp.oritsubushi;

import java.lang.ref.WeakReference;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class InformationFragment extends MenuableFragmentBase {

	private static class MyWebViewClient extends WebViewClient {
		private final WeakReference<InformationFragment> mFragment;
		private final String mUrl;
		private final String mResourceUrlBase;
		private boolean mLoading;

		private MyWebViewClient(InformationFragment fragment, String url, String resourceUrlBase) {
			mFragment = new WeakReference<InformationFragment>(fragment);
			mUrl = url;
			mResourceUrlBase = resourceUrlBase;
		}

		private boolean handleUri(Uri uri)
		{
			if(mUrl.equals(uri.toString())) {
				return false;
			} else if (mFragment.get() != null){
				mFragment.get().startActivity(new Intent(Intent.ACTION_VIEW, uri));
				return true;
			} else {
				return false;
			}
		}

		@SuppressWarnings("deprecation")
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			return handleUri(Uri.parse(url));
		}

		@TargetApi(Build.VERSION_CODES.N)
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			return handleUri(request.getUrl());
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			mLoading = true;
			super.onPageStarted(view, url, favicon);
		}
		
		@Override
		public void onLoadResource(WebView view, String url) {
			super.onLoadResource(view, url);
			if(mLoading && !url.startsWith(mResourceUrlBase)) {
				mFragment.get().onPageReady();
				mLoading = false;
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
	View mWrapper;
	String mUrl;
	String mContent;
	String mContentType;
	int mLoadingState;
	private static final int NOT_INITIALIZED = 0;
	private static final int LOADING = 1;
	private static final int LOADED_BG = 2;
	private static final int LOADED = 3;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUrl = getString(R.string.information_url);
		mLoadingState = NOT_INITIALIZED;
		load();
	}

	private void setContent() {
		if(mContent != null) {
			mWebView.loadDataWithBaseURL(mUrl, mContent, mContentType, "UTF-8", null);
			mLoadingState = LOADED;
		}
	}
	
	private void onPageReady() {
		if(mWrapper != null && mWrapper.getVisibility() != View.GONE){ 
			mWrapper.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.wrapper_fade_out));
			mWrapper.setVisibility(View.GONE);
		}
	}

	private void onLoadFinish(String content, String contentType) {
		mContent = content;
		mContentType = contentType;
		if(mWebView != null) {
			mLoadingState = LOADED;
			mReloadButton.setEnabled(true);
			setContent();
		} else {
			mLoadingState = LOADED_BG;
		}
	}

	public void onLoadSuccess(String response) {
		onLoadFinish(response, "text/html");
	}

	public void onLoadFailure(String response) {
		onLoadFinish(getString(R.string.information_load_error), "text/plain");
	}

	private void load() {
		if(mLoadingState != LOADING) {
			mLoadingState = LOADING;
			mHttpClient.get(mUrl, new HttpResponseHandler(this));
			if(mWrapper != null) {
				mWrapper.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.wrapper_fade_in));
				mWrapper.setVisibility(View.VISIBLE);
			}
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.info, container, false);

		mReloadButton = (Button)view.findViewById(R.id.button_reload);
		mReloadButton.setEnabled(false);
		mWebView = (WebView)view.findViewById(R.id.web_view);
		mWrapper = view.findViewById(R.id.wrapper);
		mWrapper.setVisibility(mLoadingState == LOADING ? View.VISIBLE : View.GONE);
		mReloadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mReloadButton.setEnabled(false);
				load();
			}
		});
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new MyWebViewClient(this, mUrl, getString(R.string.information_resource_url_base)));

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(mLoadingState == LOADED) {
			mWebView.restoreState(savedInstanceState);
		}
	}
	
	@Override
	public void onResume() {
		setContent();
		mReloadButton.setEnabled(mLoadingState == LOADED);
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mWebView != null) {
			mWebView.saveState(outState);
		}
	}

	@Override
	public void onDestroyView() {
		mReloadButton = null;
		mWebView = null;
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		if(mLoadingState == LOADING) {
			mHttpClient.cancelRequests(getActivity(), true);
		}
		super.onDestroy();
	}
}
