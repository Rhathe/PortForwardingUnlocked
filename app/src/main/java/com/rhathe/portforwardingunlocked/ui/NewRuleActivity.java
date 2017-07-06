package com.rhathe.portforwardingunlocked.ui;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;

import com.rhathe.portforwardingunlocked.AppDatabase;
import com.rhathe.portforwardingunlocked.R;
import com.rhathe.portforwardingunlocked.Rule;

public class NewRuleActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rule_detail);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rule_detail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch(id) {
			case R.id.action_save_rule:
				saveRule();
				break;
			case R.id.action_delete_rule:
				deleteRule();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void saveRule() {
		Rule rule = new Rule();

		try {
			setRuleName(rule);
			setRuleProtocol(rule);
			setRuleEnabled(rule);
			AppDatabase db = AppDatabase.getAppDatabase(this);

			new Thread(() -> {
				Log.e("Afsas", "asfasf");
				db.ruleDao().insert(rule);

				Intent mainActivityIntent = new Intent(this, MainActivity.class);
				mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
				startActivity(mainActivityIntent);
				finish();
			}).start();
		} catch(Exception e) {
			Log.e("portforwardingunlocked", e.getMessage());
		}
	}

	private void deleteRule() {
		// TODO
	}

	private void setRuleName(Rule rule) throws Exception {
		EditText nameText = findViewById(R.id.name);
		TextInputLayout nameLayout = findViewById(R.id.name_layout);

		if (nameText.getText() == null || nameText.getText().toString().length() <= 0) {
			nameLayout.setErrorEnabled(true);
			String err = "Empty rule name";
			nameLayout.setError(err);
			throw new Exception(err);
		} else {
			rule.setName(nameText.getText().toString());
			nameLayout.setErrorEnabled(false);
		}
	}

	private void setRuleProtocol(Rule rule) {
		Spinner protocolSpinner = (Spinner) findViewById(R.id.protocol);
		String protocol = protocolSpinner.getSelectedItem().toString();

		// determine the protocol
		switch (protocol) {
			case "TCP":
				rule.setIsTcp(true);
				break;
			case "UDP":
				rule.setIsUdp(true);
				break;
			case "BOTH":
			default:
				rule.setIsTcp(true);
				rule.setIsUdp(true);
				break;
		}
	}

	private void setRuleEnabled(Rule rule) {
		rule.setIsEnabled(true);
	}
}
