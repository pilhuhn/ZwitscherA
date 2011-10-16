package de.bsd.zwitscher.account;

import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TabWidget;
import de.bsd.zwitscher.TwitterHelper;
import twitter4j.http.RequestToken;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
public class TwitterLoginActivity extends Activity {
    WebView myWebView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.twitter_login_webview);
        myWebView = (WebView) findViewById(R.id.WebView);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        myWebView.addJavascriptInterface(new MyJavaScriptClient(),"HTMLOUT");
        myWebView.setWebViewClient(new MyWebViewClient());

        try {
            RequestToken rt = new GetRequestTokenTask().execute().get();
            String token = rt.getToken();

            System.out.println("Token is " + token);

            myWebView.loadUrl("https://api.twitter.com/oauth/authorize?force_login=true&oauth_token="+token);
        } catch (InterruptedException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (ExecutionException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }

    }

    @SuppressWarnings("unused")
    public void setPin(View v) {

        EditText et = (EditText) findViewById(R.id.pinInput);
        String pin = et.getText().toString();
        try {
            Account acct = new GenerateAccountWithOauthTask().execute(pin).get();
                Intent i = new Intent().setClass(this, TabWidget.class);
                startActivity(i);
                finish();
// TODO make this some included activity that returns data to the caller ?

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "Error: " + e.getMessage(), 15000).show();
            e.printStackTrace();
        }
    }

    private class GetRequestTokenTask extends AsyncTask<Void,Void,RequestToken> {
        @Override
        protected RequestToken doInBackground(Void... voids) {
            RequestToken rt = null;
            try {
                TwitterHelper th = new TwitterHelper(TwitterLoginActivity.this,null);
                rt = th.getRequestToken(true);
                return rt;
            } catch (Exception e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
            return null;
        }
    }

    private class GenerateAccountWithOauthTask extends AsyncTask<String,Void,Account> {
        @Override
        protected Account doInBackground(String... strings) {
            TwitterHelper th = new TwitterHelper(TwitterLoginActivity.this,null);
            String pin = strings[0];
            Account acct = null;
            try {
                acct = th.generateAccountWithOauth(pin);
                return acct;
            } catch (Exception e) {
                System.err.println("-- GenerateAccountWithOAuth failed");
                e.printStackTrace();  // TODO: Customise this generated block
            }
            return null;
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);    // TODO: Customise this generated block
            System.out.println("on ps " + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);    // TODO: Customise this generated block
            System.out.println("on pf " + url);
            if (url.equals("https://api.twitter.com/oauth/authorize")) {
                System.out.println("Hit!");
                myWebView.loadUrl("javascript:window.HTMLOUT.setHTML('document.getElementsById('code-desc')');");
            }
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);    // TODO: Customise this generated block
            System.out.println("on lr " + url);
        }
    }

    public class MyJavaScriptClient {

        public void setHtml(String html) {
            System.out.println("html is " + html);
        }
    }
}