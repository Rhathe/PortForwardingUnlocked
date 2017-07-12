package com.rhathe.portforwardingunlocked

import android.app.IntentService
import android.content.Intent
import android.databinding.BaseObservable
import android.databinding.Bindable
import android.databinding.Observable
import android.databinding.PropertyChangeRegistry
import android.util.Log

import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.concurrent.CompletionService
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

import android.os.PowerManager
import android.support.v4.content.LocalBroadcastManager
import java.util.*


class ForwardingService : IntentService {

	private val executorService: ExecutorService

	//wake lock
	private var wakeLock: PowerManager.WakeLock? = null


	constructor() : super(TAG) {
		this.executorService = Executors.newFixedThreadPool(30)
	}

	constructor(executorService: ExecutorService) : super(TAG) {
		this.executorService = executorService
	}

	override fun onCreate() {
		super.onCreate()

		// https://developer.android.com/intl/ja/training/scheduling/wakelock.html
		val powerManager = getSystemService(POWER_SERVICE) as PowerManager
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
		wakeLock!!.acquire(100000)
	}

	override fun onHandleIntent(intent: Intent?) {
		Log.i(TAG, "Running port forwarding service")

		var localIntent = Intent(BROADCAST_ACTION)
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)

		val db = AppDatabase.getAppDatabase(this)
		val rules = db.ruleDao().allEnabledSync

		val completionService = ExecutorCompletionService<Void>(executorService)

		var remainingFutures = 0
		var fromPort: Int
		var targetPort: Int
		var fromAddress: InetSocketAddress
		var targetAddress: InetSocketAddress

		for (rule in rules) {
			for (i in 0..rule.portRange - 1) {
				try {
					fromPort = rule.fromPort + i
					targetPort = rule.targetPort + i
					fromAddress = getFromAddress(rule.fromInterface, fromPort)
					targetAddress = InetSocketAddress(rule.target, targetPort)

					if (rule.isTcp!!) {
						completionService.submit(TcpForwarder(fromAddress, targetAddress, rule.name))
						remainingFutures++
					}

					if (rule.isUdp!!) {
						completionService.submit(UdpForwarder(fromAddress, targetAddress, rule.name))
						remainingFutures++
					}

				} catch (e: Exception) {
					Log.e(TAG, "Error generating IP Address for FROM interface with rule '" + rule.name + "'", e)

					// graceful UI Exception handling - broadcast this to ui
					localIntent = Intent(BROADCAST_ACTION).putExtra(ERROR_MESSAGE, rule.name)
					// Broadcasts the Intent to receivers in this app.
					LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
				}

			}
		}

		var completedFuture: Future<*>

		// loop through each callback, and handle an exception
		while (remainingFutures > 0) {

			// block until a callable completes
			try {
				completedFuture = completionService.take()
				remainingFutures--

				completedFuture.get()
			} catch (e: ExecutionException) {
				Log.e(TAG, "Error when forwarding port.", e)
				localIntent = Intent(BROADCAST_ACTION).putExtra(ERROR_MESSAGE, e.cause?.message)
				// Broadcasts the Intent to receivers in this app.
				LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)

				break
			} catch (e: InterruptedException) {
				e.printStackTrace()
			}

		}
	}

	@Throws(SocketException::class, Exception::class)
	private fun getNetworkInterface(interfaceName: String): NetworkInterface {
		val intf = getInterfaces().find({x -> x.displayName == interfaceName}) ?:
				throw Exception("Could not find interface " + interfaceName)
		return intf
	}

	@Throws(Exception::class)
	private fun getFromAddress(interfaceName: String, port: Int): InetSocketAddress {
		val intf = getNetworkInterface(interfaceName)

		val enumIpAddr = intf.inetAddresses
		while (enumIpAddr.hasMoreElements()) {
			val inetAddress = enumIpAddr.nextElement()

			val address = inetAddress.hostAddress.toString()

			if (address.isNotEmpty() && inetAddress is Inet4Address) {
				return InetSocketAddress(address, port)
			}
		}

		throw Exception("Could not find address for interface " + interfaceName)
	}

	companion object {
		private val TAG = "ForwardingService"
		private val WAKE_LOCK_TAG = "PortForwardingUnlockedServiceWakeLockTag"

		val BROADCAST_ACTION = "com.rhathe.portforwardingunlocked.ForwardingService.BROADCAST"
		val ERROR_MESSAGE = "com.rhathe.portforwardingunlocked.ForwardingService.PORT_FORWARD_ERROR"

		private var enabled: Boolean = false
		var status: Status = Status()

		class Status: BaseObservable() {
			@Bindable
			fun getEnabled(): Boolean {
				return enabled
			}

			fun setEnabled(_enabled: Boolean) {
				enabled = _enabled
				notifyPropertyChanged(BR.enabled)
			}
		}
		fun getInterfaces(): List<NetworkInterface> {
			val en = NetworkInterface.getNetworkInterfaces()
			val intfs = if (en != null) Collections.list(en) else emptyList<NetworkInterface>()
			return intfs.map({ intf ->
				val isValid = Collections.list(intf.inetAddresses).any({ inetAddress ->
					val address = inetAddress.hostAddress
					address != null && address.isNotEmpty() && inetAddress is Inet4Address
				})

				if (isValid) intf
				else null
			}).filterIsInstance<NetworkInterface>()
		}
	}
}
