package com.rhathe.portforwardingunlocked;

import android.util.Log;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Iterator;
import java.util.concurrent.Callable;


public abstract class Forwarder implements Callable<Void> {
	public static final String TAG = "Forwarder";
	protected static final String protocol = "";

	protected final InetSocketAddress from, to;
	protected final String ruleName;

	protected AbstractSelectableChannel listening;
	protected static final int BUFFER_SIZE = 100000;

	abstract void bindSocket(AbstractSelectableChannel channel) throws IOException;
	abstract void registerListening(AbstractSelectableChannel channel, Selector selector) throws ClosedChannelException;
	abstract void processAcceptable(SelectionKey key) throws IOException;
	abstract void processConnectable(SelectionKey key) throws IOException;
	abstract void processWritable(SelectionKey key) throws IOException;
	abstract void processReadable(SelectionKey key, ByteBuffer readBuffer) throws IOException;
	abstract void handleKeyIterAfter(Iterator<SelectionKey> it);

	public Forwarder(InetSocketAddress from, InetSocketAddress to, String ruleName){
		this.from = from;
		this.to = to;
		this.ruleName = ruleName;
	}

	public Void call() throws IOException {
		String startMsg = "%s Port Forwarding Started from port %s to port %s";
		Log.d(this.TAG, String.format(startMsg, protocol, from.getPort(), to.getPort()));

		try {
			Selector selector = Selector.open();
			ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);

			listening = ServerSocketChannel.open();
			listening.configureBlocking(false);

			try {
				bindSocket(listening);
			} catch(SocketException e){
				String finalMsg = String.format("Could not bind port %s for %s Rule '%s'", from.getPort(), protocol, ruleName);
				Log.e(TAG, finalMsg, e);
				throw new BindException(finalMsg);
			}

			registerListening(listening, selector);

			while (true) {

				if (Thread.currentThread().isInterrupted()){
					Log.i(TAG, String.format("%s Thread interrupted, will perform cleanup", protocol));
					listening.close();
					break;
				}

				int count = selector.select();
				if (count > 0) {
					Iterator<SelectionKey> it = selector.selectedKeys().iterator();
					while (it.hasNext()) {

						SelectionKey key = it.next();
						it.remove();

						if (key.isValid() && key.isAcceptable()) {
							processAcceptable(key);
						}

						if (key.isValid() && key.isConnectable()) {
							processConnectable(key);
						}

						if (key.isValid() && key.isReadable()) {
							processReadable(key, readBuffer);
						}

						if (key.isValid() && key.isWritable()) {
							processWritable(key);
						}

						handleKeyIterAfter(it);
					}
				}
			}
		} catch(IOException e) {
			Log.e(TAG, "Problem opening Selector", e);
			throw e;
		}

		return null;
	}
}