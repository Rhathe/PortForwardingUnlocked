package com.rhathe.portforwardingunlocked;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import java.util.List;


public class MainActivity extends AppCompatActivity {

	private static Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mContext = getApplicationContext();

		//showRuleItems();
	}

	private void showRuleItems() {
		AppDatabase db = AppDatabase.getAppDatabase(this);
		List<Rule> rules = db.ruleDao().getAllEnabled();

		LayoutInflater inflater = LayoutInflater.from(mContext);

		LinearLayout mainView = (LinearLayout) findViewById(R.id.main_content_view);

		for (Rule rule : rules) {
			LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.rule_item, null, false);
			mainView.addView(layout);
		}
	}
}
