package com.rhathe.portforwardingunlocked.ui;

import android.arch.lifecycle.LifecycleActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rhathe.portforwardingunlocked.AppDatabase;
import com.rhathe.portforwardingunlocked.R;
import com.rhathe.portforwardingunlocked.Rule;


// Replace with AppCompatActivity when LiveData is stable
public class MainActivity extends LifecycleActivity {

	private static Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mContext = getApplicationContext();

		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(view -> {
			Intent newRuleIntent = new Intent(this, NewRuleActivity.class);
			startActivity(newRuleIntent);
		});

		showRuleItems();
	}

	private void showRuleItems() {
		LayoutInflater inflater = LayoutInflater.from(mContext);
		LinearLayout mainView = (LinearLayout) findViewById(R.id.main_content_view);

		AppDatabase db = AppDatabase.getAppDatabase(this);
		db.ruleDao().getAllEnabled().observe(this, rules -> {
			for (Rule rule : rules) {
				LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.rule_item, null, false);
				replaceInLayout(layout, R.id.name, rule.getName());
				mainView.addView(layout);
			}
		});
	}

	private void replaceInLayout(LinearLayout layout, int id, String text) {
		TextView view = layout.findViewById(id);
		view.setText(text);
	}
}
