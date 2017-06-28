package com.rhathe.portforwardingunlocked;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;


public class ForwardingService extends IntentService {
	private static final String TAG = "ForwardingService";
	private static final String WAKE_LOCK_TAG = "PortForwardingUnlockedServiceWakeLockTag";

	public static final String BROADCAST_ACTION = "com.rhathe.portforwardingunlocked.ForwardingService.BROADCAST";
	public static final String ERROR_MESSAGE = "com.rhathe.portforwardingunlocked.ForwardingService.PORT_FORWARD_ERROR";

	private final ExecutorService executorService;


	//wake lock
	private PowerManager.WakeLock wakeLock;

	public ForwardingService() {
		super(TAG);
		this.executorService = Executors.newFixedThreadPool(30);
	}

	public ForwardingService(ExecutorService executorService) {
		super(TAG);
		this.executorService = executorService;
	}


	@Override
	public void onCreate() {
		super.onCreate();

		// https://developer.android.com/intl/ja/training/scheduling/wakelock.html
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
		wakeLock.acquire();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "Running port forwarding service");

		Intent localIntent = new Intent(BROADCAST_ACTION);
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

		AppDatabase db = AppDatabase.getAppDatabase(this);
		List<Rule> rules = db.ruleDao().getAllEnabled();

		CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);

		int remainingFutures = 0;
		int fromPort;
		int targetPort;
		InetSocketAddress fromAddress;
		InetSocketAddress targetAddress;

		for (Rule rule : rules) {
			for (int i = 0; i < rule.getPortRange(); i++) {
				try {
					fromPort = rule.getFromPort() + i;
					targetPort = rule.getTargetPort() + i;
					fromAddress = getFromAddress(rule.getFromInterface(), fromPort);
					targetAddress = new InetSocketAddress(rule.getTarget(), targetPort);

					if (rule.getIsTcp()) {
						completionService.submit(new TcpForwarder(fromAddress, targetAddress, rule.getName()));
						remainingFutures++;
					}

					if (rule.getIsUdp()) {
						completionService.submit(new UdpForwarder(fromAddress, targetAddress, rule.getName()));
						remainingFutures++;
					}

				} catch(Exception e){
					Log.e(TAG, "Error generating IP Address for FROM interface with rule '" + rule.getName() + "'", e);

					// graceful UI Exception handling - broadcast this to ui
					localIntent = new Intent(BROADCAST_ACTION).putExtra(ERROR_MESSAGE, rule.getName());
					// Broadcasts the Intent to receivers in this app.
					LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
				}
			}
		}

		Future<?> completedFuture;

		// loop through each callback, and handle an exception
		while (remainingFutures > 0) {

			// block until a callable completes
			try {
				completedFuture = completionService.take();
				remainingFutures--;

				completedFuture.get();
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();

				Log.e(TAG, "Error when forwarding port.", e);
				localIntent = new Intent(BROADCAST_ACTION).putExtra(ERROR_MESSAGE, e.getCause().getMessage());
				// Broadcasts the Intent to receivers in this app.
				LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private NetworkInterface getNetworkInterface(String interfaceName) throws SocketException, Exception {
		for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
			NetworkInterface intf = en.nextElement();

			Log.d(TAG, intf.getDisplayName() + " vs " + interfaceName);
			if (intf.getDisplayName().equals(interfaceName)){

				Log.i(TAG, "Found the relevant Interface.");
				return intf;
			}
		}

		throw new Exception("Could not find interface " + interfaceName);
	}

	private InetSocketAddress getFromAddress(String interfaceName, int port) throws Exception {
		NetworkInterface intf = getNetworkInterface(interfaceName);

		for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
			InetAddress inetAddress = enumIpAddr.nextElement();

			String address = new String(inetAddress.getHostAddress().toString());

			if (address != null & address.length() > 0 && inetAddress instanceof Inet4Address){
				return new InetSocketAddress(address, port);
			}
		}

		throw new Exception("Could not find address for interface " + interfaceName);
	}
}
