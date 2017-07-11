package com.rhathe.portforwardingunlocked.ui

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner

import com.rhathe.portforwardingunlocked.AppDatabase
import com.rhathe.portforwardingunlocked.R
import com.rhathe.portforwardingunlocked.Rule
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import com.rhathe.portforwardingunlocked.BR


class BaseRuleActivity : AppCompatActivity() {
	var ruleUid: String = ""
	var rule: Rule = Rule()
	var db: AppDatabase = AppDatabase.getAppDatabase(this)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val binding: ViewDataBinding = DataBindingUtil.setContentView(this, R.layout.rule_detail)
		ruleUid = (intent.extras?.get("ruleUid") ?: "").toString()

		Thread {
			if (ruleUid.isNotEmpty()) {
				rule = db.ruleDao().getById(ruleUid)
			}

			initFromInterfaces()
			binding.setVariable(BR.rule, rule)
		}.start()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; this adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.rule_detail, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		val id = item.itemId

		when (id) {
			R.id.action_save_rule -> saveRule()
			R.id.action_delete_rule -> deleteRule()
		}

		return super.onOptionsItemSelected(item)
	}

	private fun getInterfaces(): List<String> {
		val en = NetworkInterface.getNetworkInterfaces()
		val intfs = if (en != null) Collections.list(en) else emptyList<NetworkInterface>()
		return intfs.map({ intf ->
			val isValid = Collections.list(intf.inetAddresses).any({ inetAddress ->
				val address = inetAddress.hostAddress
				address != null && address.isNotEmpty() && inetAddress is Inet4Address
			})

			if (isValid) intf.displayName
			else ""
		}).filter({ x -> x != "" })
	}

	private fun initFromInterfaces() {
		rule.fromInterfaces = getInterfaces()
	}

	private fun backToMain(fn: () -> Unit) {
		Thread {
			fn()
			val intent = Intent(this, MainActivity::class.java)
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
			startActivity(intent)
			finish()
		}.start()
	}

	private fun saveRule() {
		backToMain {
			if (ruleUid.isBlank()) db.ruleDao().insert(rule)
			else db.ruleDao().update(rule)
		}
	}

	private fun deleteRule() {
		backToMain {
			db?.ruleDao()?.delete(rule)
		}
	}
}
