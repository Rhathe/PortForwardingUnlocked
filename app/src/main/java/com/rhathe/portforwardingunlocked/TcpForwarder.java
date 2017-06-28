package com.rhathe.portforwardingunlocked;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Iterator;


public class TcpForwarder extends Forwarder {
	public static final String TAG = "TcpForwarder";
	protected static final String protocol = "TCP";

	public TcpForwarder(InetSocketAddress from, InetSocketAddress to, String ruleName) {
		super(from, to, ruleName);
	}

	protected void bindSocket(AbstractSelectableChannel channel) throws IOException {
		((ServerSocketChannel) channel).socket().bind(this.from, 0);
	}

	protected void registerListening(AbstractSelectableChannel channel, Selector selector) throws ClosedChannelException {
		channel.register(selector, SelectionKey.OP_ACCEPT, channel);
	}

	protected void processAcceptable(SelectionKey key) throws IOException {
		SocketChannel from = ((ServerSocketChannel) key.attachment()).accept();
		System.out.println("Accepted " + from.socket());
		from.socket().setTcpNoDelay(true);
		from.configureBlocking(false);

		SocketChannel forwardToSocket = SocketChannel.open();
		forwardToSocket.configureBlocking(false);

		if (forwardToSocket.connect(this.to)) {
			forwardToSocket.socket().setTcpNoDelay(true);
			registerReads(key.selector(), from, forwardToSocket);
		} else {
			forwardToSocket.register(key.selector(), SelectionKey.OP_CONNECT, from);
		}
	}

	protected void processConnectable(SelectionKey key) throws IOException {
		SocketChannel from = (SocketChannel) key.attachment();
		SocketChannel forwardToSocket = (SocketChannel) key.channel();

		forwardToSocket.finishConnect();
		forwardToSocket.socket().setTcpNoDelay(true);
		registerReads(key.selector(), from, forwardToSocket);
	}

	protected void processWritable(SelectionKey key) throws IOException {
		RoutingPair pair = (RoutingPair) key.attachment();

		pair.writeBuffer.flip();
		pair.to.write(pair.writeBuffer);

		if (pair.writeBuffer.remaining() > 0) {
			pair.writeBuffer.compact();
		} else {
			key.interestOps(SelectionKey.OP_READ);
			pair.writeBuffer.clear();
		}
	}

	protected void processReadable(SelectionKey key, ByteBuffer readBuffer) throws IOException {
		readBuffer.clear();
		RoutingPair pair = (RoutingPair) key.attachment();

		int r = 0;
		try {
			r = pair.from.read(readBuffer);
		} catch(IOException e) {
			key.cancel();
			System.out.println("Connection closed: " + key.channel());
		}

		if (r <= 0) {
			pair.from.close();
			pair.to.close();
			key.cancel();
			System.out.println("Connection closed: " + key.channel());
		} else {
			readBuffer.flip();
			pair.to.write(readBuffer);

			if (readBuffer.remaining() > 0) {
				pair.writeBuffer.put(readBuffer);
				key.interestOps(SelectionKey.OP_WRITE);
			}
		}
	}

	// noop, not necessary for tcp
	protected void handleKeyIterAfter(Iterator<SelectionKey> it) {}

	private static void registerReads(
			Selector selector,
			SocketChannel socket,
			SocketChannel forwardToSocket) throws ClosedChannelException {
		RoutingPair pairFromToPair = new RoutingPair();
		pairFromToPair.from = socket;
		pairFromToPair.to = forwardToSocket;
		pairFromToPair.from.register(selector, SelectionKey.OP_READ, pairFromToPair);

		RoutingPair pairToFromPair = new RoutingPair();
		pairToFromPair.from = forwardToSocket;
		pairToFromPair.to = socket;
		pairToFromPair.from.register(selector, SelectionKey.OP_READ, pairToFromPair);
	}

	static class RoutingPair {
		SocketChannel from;
		SocketChannel to;
		ByteBuffer writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
	}
}
