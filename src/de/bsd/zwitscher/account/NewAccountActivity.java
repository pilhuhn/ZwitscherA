package de.bsd.zwitscher.account;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TabWidget;
import de.bsd.zwitscher.TwitterHelper;

public class NewAccountActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.new_account);
	}


    public void create(View v) {

        Spinner serviceSpinner = (Spinner) findViewById(R.id.new_account_spinner);
        TextView usernameView = (TextView) findViewById(R.id.new_account_username);
        TextView passwordView = (TextView) findViewById(R.id.new_account_password);
        CheckBox switchBox = (CheckBox) findViewById(R.id.new_account_switch);

        String username = usernameView.getText().toString();
        String password = passwordView.getText().toString();
        String service = serviceSpinner.getSelectedItem().toString();

        boolean shouldSwitch = switchBox.isChecked();

        new CreateAccountTask(this,username,password,service,shouldSwitch).execute();
//        finish();
/*
        TwitterHelper th = new TwitterHelper(this,null);
        try {
            // Try to generate a token and insert it.
            Account newAccount = th.generateAccountWithXauth(username, password, service, shouldSwitch);

            AccountHolder.getInstance().setAccount(newAccount);
            Intent intent = new Intent(this, TabWidget.class);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
            Toast.makeText(this,"Login failed: " + e.getLocalizedMessage(),Toast.LENGTH_LONG);
        }
*/

    }
}
