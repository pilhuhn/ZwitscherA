package de.bsd.zwitscher.account;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.account.NewAccountActivity;


public class SelectAccountActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.select_account);
	}

	public void newAccount(View view) {
		Intent i = new Intent(this,NewAccountActivity.class);
		startActivity(i);
	}
}
