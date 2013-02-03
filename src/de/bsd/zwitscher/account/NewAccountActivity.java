package de.bsd.zwitscher.account;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import de.bsd.zwitscher.R;

/**
 * Create a new Account and possibly switch to it
 * @author Heiko W. Rupp
 */
public class NewAccountActivity extends Activity {

    private TableRow urlRow;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.new_account);
        Spinner serviceSpinner = (Spinner) findViewById(R.id.new_account_spinner);
        urlRow = (TableRow) findViewById(R.id.new_account_url_row);
        final TextView urlText= (TextView) findViewById(R.id.new_account_url_text);
        final EditText urlEdit= (EditText) findViewById(R.id.new_account_url_edit);
        serviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                urlRow.setEnabled(position==3);
                urlText.setEnabled(position==3);
                urlEdit.setEnabled(position==3);

                TextView usernameView = (TextView) findViewById(R.id.new_account_username);
                TextView passwordView = (TextView) findViewById(R.id.new_account_password);

                usernameView.setEnabled(position!=0);
                passwordView.setEnabled(position!=0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do
            }
        });
	}


    @SuppressWarnings("unused")
    public void create(View v) {

        Spinner serviceSpinner = (Spinner) findViewById(R.id.new_account_spinner);
        TextView usernameView = (TextView) findViewById(R.id.new_account_username);
        TextView passwordView = (TextView) findViewById(R.id.new_account_password);
        CheckBox switchBox = (CheckBox) findViewById(R.id.new_account_switch);
        TextView urlView = (TextView) findViewById(R.id.new_account_url_edit);

        String username = usernameView.getText().toString();
        String password = passwordView.getText().toString();
        String tmp = serviceSpinner.getSelectedItem().toString();
        long id = serviceSpinner.getSelectedItemId();
        System.err.println("id" + id);
        id = serviceSpinner.getSelectedItemPosition();
        System.err.println("pos " + id );

        Account.Type service ;
        String url;

        if (tmp.toLowerCase().startsWith("twitter")) {
            service = Account.Type.TWITTER;
            url = " - do not care -";
        } else if (tmp.equalsIgnoreCase("identi.ca")) {
            service = Account.Type.IDENTICA;
            url = "http://identi.ca";
        } else {
            service = Account.Type.STATUSNET;
            url = urlView.getText().toString();
        }

        if (!url.endsWith("/"))
            url = url + "/";

        if (!url.startsWith("https://") && !url.startsWith("http://"))  // TODO explicit ssl selection for status.net
            url = "https://" + url;

        Log.i("newAccount","url is " + url);

        boolean shouldSwitch = switchBox.isChecked();

        if (serviceSpinner.getSelectedItemId()==0) { // Twitter with OAuth
            Intent i = new Intent(this,TwitterLoginActivity.class);
            startActivity(i);

            if (shouldSwitch) {
                // TODO
            }
        }
        else {
            new CreateAccountTask(this,username,password,service,shouldSwitch, url).execute();
        }

    }
}
