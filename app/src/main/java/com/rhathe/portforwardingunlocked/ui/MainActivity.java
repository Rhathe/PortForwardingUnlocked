package com.rhathe.portforwardingunlocked.ui;

import android.arch.lifecycle.LifecycleActivity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rhathe.portforwardingunlocked.AppDatabase;
import com.rhathe.portforwardingunlocked.BR;
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
			Intent newRuleIntent = new Intent(this, BaseRuleActivity.class);
			startActivity(newRuleIntent);
		});

		showRuleItems();
	}

	private void showRuleItems() {
		LayoutInflater inflater = LayoutInflater.from(mContext);
		LinearLayout enabledView = (LinearLayout) findViewById(R.id.enabled_rules);
		LinearLayout disabledView = (LinearLayout) findViewById(R.id.disabled_rules);

		AppDatabase db = AppDatabase.getAppDatabase(this);
		db.ruleDao().getAll().observe(this, rules -> {
			for (Rule rule : rules) {
				LinearLayout parent = rule.getIsEnabled() ? enabledView : disabledView;

				ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.rule_item, parent, false);
				binding.setVariable(BR.rule, rule);

				LinearLayout layout = (LinearLayout) binding.getRoot();
				layout.setOnClickListener((View v) -> goToRule(rule));

				parent.addView(layout);
			}
		});
	}

	public void goToRule(Rule rule) {
		Intent ruleIntent = new Intent(this, BaseRuleActivity.class);
		ruleIntent.putExtra("ruleUid", rule.getUid());
		startActivity(ruleIntent);
	}
}
