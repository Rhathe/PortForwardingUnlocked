package com.rhathe.portforwardingunlocked.ui

import android.content.DialogInterface
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

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.v7.app.AlertDialog
import com.rhathe.portforwardingunlocked.*


class RuleActivity : AppCompatActivity() {
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
		if (ruleUid.isBlank()) menu.findItem(R.id.action_delete_rule).setVisible(false)
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

	private fun initFromInterfaces() {
		rule.fromInterfaces = ForwardingService.getInterfaces().map({x -> x.displayName})
	}

	private fun backToMain(fn: () -> Unit) {
		Thread {
			try {
				fn()
				val intent = Intent(this, MainActivity::class.java)
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
				startActivity(intent)
				finish()
			} catch(e: Rule.EmptyNameException) {
				runOnUiThread({
					val et = findViewById<EditText>(R.id.name)
					et.error = "Name is empty"
				})
			} catch(e: Rule.NegativeRangeException) {
				runOnUiThread({
					val et = findViewById<EditText>(R.id.from_end_port)
					et.error = "End port must be >= to start port"
				})
			} catch(e: Rule.ZeroPortException) {
				runOnUiThread({
					val setErr = { id: Int ->
						val et = findViewById<EditText>(id)
						et.error = "Port cannot be 0"
					}

					if (rule.fromPort == 0) setErr(R.id.from_port)
					if (rule.targetPort == 0) setErr(R.id.target_port)
				})
			} catch(e: Rule.OverlappingException) {
				val errMsg = String.format("Port conflicts with '%s' ports [%s, %s]", e.rule.name, e.rule.fromPort, e.rule.fromEndPort)

				runOnUiThread({
					val startEt = findViewById<EditText>(R.id.from_port)
					if (rule.fromPort >= e.rule.fromPort && rule.fromPort <= e.rule.fromEndPort)
						startEt.error = errMsg

					val endEt = findViewById<EditText>(R.id.from_end_port)
					if (rule.fromEndPort >= e.rule.fromPort && rule.fromEndPort <= e.rule.fromEndPort)
						endEt.error = errMsg

					if (rule.fromPort <= e.rule.fromPort && rule.fromEndPort >= e.rule.fromEndPort){
						startEt.error = errMsg
						endEt.error = errMsg
					}
				})
			}
		}.start()
	}

	private fun saveRule() {
		backToMain {
			rule.checkIsValid()
			val overlappingRule = db.ruleDao().getOverlappingRule(rule.uid, rule.fromPort, rule.fromEndPort)
			if (overlappingRule != null) {
				val e = Rule.OverlappingException()
				e.rule = overlappingRule
				throw e
			}

			if (ruleUid.isBlank()) db.ruleDao().insert(rule)
			else db.ruleDao().update(rule)
		}
	}

	private fun deleteRule() {
		val alert = AlertDialog.Builder(this)
		alert.setMessage("Do you want to delete this rule?")
			.setPositiveButton("ОК") { _, _ -> backToMain { db.ruleDao().delete(rule) } }
			.setNegativeButton("CANCEL") { _, _ -> {} }
			.show()
	}
}
