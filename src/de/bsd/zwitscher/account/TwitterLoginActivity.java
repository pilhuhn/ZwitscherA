package de.bsd.zwitscher.account;

import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TabWidget;
import de.bsd.zwitscher.TwitterHelper;
import twitter4j.auth.RequestToken;

/**
 * Activity to run the login at Twitter via OAuth game.
 * This displays a web view, which Twitter shows where the
 * user enters his credentials. Then on load of the result page
 * it gets the content of the page (via injecting JavaScript, that
 * calls us back) and then runs the TwitterHelper#generateAccountWithOauth
 * to obtain the account tokens. We then forward to the main view.
 *
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

        myWebView.setWebViewClient(new MyWebViewClient());
        myWebView.addJavascriptInterface(new MyJavaScriptClient(), "HTMLOUT");

        try {
            RequestToken rt = new GetRequestTokenTask().execute().get();
            if (rt==null) {
                Toast.makeText(this,"Failure to create token. Perhaps the server is down? Please retry later",Toast.LENGTH_LONG).show();
                return;
            }
            String token = rt.getToken();

            myWebView.loadUrl("https://api.twitter.com/oauth/authorize?force_login=true&oauth_token="+token);
        } catch (InterruptedException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (ExecutionException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }

    }

    private class GetRequestTokenTask extends AsyncTask<Void,Void,RequestToken> {
        @Override
        protected RequestToken doInBackground(Void... voids) {
            RequestToken rt;
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
            Account acct;
            try {
                acct = th.generateAccountWithOauth(pin);
                return acct;
            } catch (Exception e) {
                System.err.println("-- GenerateAccountWithOAuth failed");
                e.printStackTrace();  // TODO: Customise this generated block
            }
            return null;
        }

        @Override
        protected void onPostExecute(Account account) {
            Activity context = TwitterLoginActivity.this;

            // Switch to this new account
            AccountHolder.getInstance(context).setAccount(account);
            Intent i = new Intent().setClass(context, TabWidget.class);
            context.startActivity(i);

            context.finish();
        }
    }

    /**
     * Class that injects the JavaScript callback when the 2nd auth
     * page was reached.
     */
    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (url.equals("https://api.twitter.com/oauth/authorize")) {
                myWebView.loadUrl("javascript:window.HTMLOUT.obtain(document.body.innerHTML);");
                // not sure why the following fails, but doesn't really matter
                // myWebView.loadUrl("javascript:window.HTMLOUT.setHTML(document.getElementById('code-desc'));");
            }
        }
    }

    /**
     * This class holds the callback that we inject in MyWebViewClient#onPageFinished
     */
    public class MyJavaScriptClient {

        /**
         * Called from the java script in the web view. We parse the
         * html obtained and then generate an account with the help of the
         * passed pin
         * @param html Html as evaluated by the javascript
         */
        @SuppressWarnings("unused")
        public void obtain(String html) {
            int i = html.indexOf("<code>");
            if (i!=-1) {
                html = html.substring(i+6);
                i = html.indexOf("</code>");
                html = html.substring(0,i);

                try {
                    new GenerateAccountWithOauthTask().execute(html).get();
                    Intent intent = new Intent().setClass(TwitterLoginActivity.this, TabWidget.class);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    }
}