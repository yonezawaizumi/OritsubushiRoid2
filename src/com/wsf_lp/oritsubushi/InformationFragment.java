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

	/*private static class MyWebViewClient extends WebViewClient {
		private WeakReference<Fragment> fragment;

		private MyWebViewClient(Fragment fragment) {
			this.fragment = new WeakReference<Fragment>(fragment);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if(myUrl.equals(url)) {
				return false;
			} else {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				return true;
			}
		}
	}*/

	private static class HttpResponseHandler extends AsyncHttpResponseHandler {
		private WeakReference<InformationFragment> fragment;
		public HttpResponseHandler(InformationFragment fragment) {
			this.fragment = new WeakReference<InformationFragment>(fragment);
		}
		@Override
		public void onSuccess(String response) {
			InformationFragment fragment = this.fragment.get();
			if(fragment != null) {
				fragment.onLoadSuccess(response);
			}
		}
		@Override
		public void onFailure(Throwable error, String response) {
			InformationFragment fragment = this.fragment.get();
			if(fragment != null) {
				fragment.onLoadFailure(response);
			}
		}
	}

	AsyncHttpClient httpClient = new AsyncHttpClient();
	Button reload;
	WebView webView;
	String myUrl;
	String content;
	String contentType;
	boolean loading;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myUrl = getString(R.string.information_url);
		load();
	}

	private void setContent() {
		if(content != null) {
			webView.loadDataWithBaseURL(myUrl, content, contentType, "UTF-8", null);
		}
	}

	private void onLoadFinish(String content, String contentType) {
		this.content = content;
		this.contentType = contentType;
		if(webView != null) {
			loading = false;
			reload.setEnabled(true);
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
		if(!loading) {
			loading = true;
			httpClient.get(myUrl, new HttpResponseHandler(this));
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.info, container, false);

		reload = (Button)view.findViewById(R.id.button_reload);
		reload.setEnabled(false);
		webView = (WebView)view.findViewById(R.id.web_view);
		reload.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				reload.setEnabled(false);
				load();
			}
		});
		webView.getSettings().setJavaScriptEnabled(true);
		//webView.setWebViewClient(new MyWebViewClient(this));

		return view;
	}

	@Override
	public void onResume() {
		if(!loading) {
			reload.setEnabled(true);
			setContent();
		}
		super.onResume();
	}

	@Override
	public void onDestroyView() {
		reload = null;
		webView = null;
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		if(loading) {
			httpClient.cancelRequests(getActivity(), true);
		}
		super.onDestroy();
	}
}
