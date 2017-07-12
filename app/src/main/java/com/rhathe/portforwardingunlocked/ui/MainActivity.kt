package com.rhathe.portforwardingunlocked.ui

import android.arch.lifecycle.Observer
import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import com.rhathe.portforwardingunlocked.*
import java.util.Collections
import com.rhathe.portforwardingunlocked.ForwardingService




class MainActivity : AppCompatActivity(), LifecycleRegistryOwner {
	private val mRegistry = LifecycleRegistry(this)
	private var mOptionsMenu: Menu? = null
	private var forwardingServiceIntent: Intent? = null

	override fun getLifecycle(): LifecycleRegistry {
		return mRegistry
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.main)

		val fab = findViewById<FloatingActionButton>(R.id.fab)
		fab.setOnClickListener { _ -> goToRule(null) }

		showRuleItems()

		forwardingServiceIntent = Intent(this, ForwardingService::class.java)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.main, menu)
		mOptionsMenu = menu
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		val id = item.itemId

		when (id) {
			R.id.action_run -> runForwardingService()
			R.id.action_stop -> stopForwardingService()
		}

		return super.onOptionsItemSelected(item)
	}

	private fun toggleForwardingService(b: Boolean) {
		mOptionsMenu?.findItem(R.id.action_run)?.setVisible(!b)
		mOptionsMenu?.findItem(R.id.action_stop)?.setVisible(b)
	}

	private fun runForwardingService() {
		toggleForwardingService(true)
		startService(forwardingServiceIntent)
	}

	private fun stopForwardingService() {
		toggleForwardingService(false)
		stopService(forwardingServiceIntent)
	}

	private fun showRuleItems() {
		val inflater = LayoutInflater.from(applicationContext)
		val enabledView = findViewById<View>(R.id.enabled_rules) as LinearLayout
		val disabledView = findViewById<View>(R.id.disabled_rules) as LinearLayout

		val db = AppDatabase.getAppDatabase(this)
		db.ruleDao().all.observe(this, Observer<List<Rule>> { _rules ->
			val rules = _rules ?: Collections.emptyList<Rule>()
			for (rule in rules) {
				val parent = if (rule.isEnabled) enabledView else disabledView

				val binding = DataBindingUtil.inflate<ViewDataBinding>(inflater, R.layout.rule_item, parent, false)
				binding.setVariable(BR.rule, rule)
				binding.setVariable(BR.fsStatus, ForwardingService.status)

				val layout = binding.root as LinearLayout
				layout.setOnClickListener { _ -> goToRule(rule) }

				parent.addView(layout)
			}
		})
	}

	fun goToRule(rule: Rule?) {
		if (ForwardingService.status.getEnabled()) return

		val ruleIntent = Intent(this, RuleActivity::class.java)
		if (rule != null) ruleIntent.putExtra("ruleUid", rule.uid)
		startActivity(ruleIntent)
	}
}
