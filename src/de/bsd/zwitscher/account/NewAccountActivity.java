package de.bsd.zwitscher.account;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import de.bsd.zwitscher.R;

/**
 * Create a new Account and possibly switch to it
 * @author Heiko W. Rupp
 */
public class NewAccountActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.new_account);
	}


    @SuppressWarnings("unused")
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

    }
}
