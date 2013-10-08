package com.wsf_lp.oritsubushi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

@SuppressLint("SetJavaScriptEnabled") public class InformationActivity extends Activity {
	public static final int ID = R.id.info;

	private ActivityChanger activityChanger;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        activityChanger = new ActivityChanger(this, ID);

		setContentView(R.layout.info);
		final Button reload = (Button)findViewById(R.id.button_reload);
		final String myUrl = getString(R.string.information_url);
		final WebView webView = (WebView)findViewById(R.id.web_view);
		webView.getSettings().setJavaScriptEnabled(true);
		reload.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				webView.reload();
			}
		});
		webView.loadUrl(myUrl);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView webView, String url) {
				reload.setEnabled(true);
			}
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				reload.setEnabled(false);
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
		});
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        activityChanger.createMenu(menu);
        return true;
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		activityChanger.prepareMenu(menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        return activityChanger.onSelectActivityMenu(item.getItemId());
    }

	
}
