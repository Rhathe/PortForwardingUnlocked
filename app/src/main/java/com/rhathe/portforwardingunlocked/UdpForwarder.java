package com.rhathe.portforwardingunlocked;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Iterator;


public class UdpForwarder extends Forwarder {
	public static final String TAG = "UdpForwarder";
	protected static final String protocol = "UDP";

	public UdpForwarder(InetSocketAddress from, InetSocketAddress to, String ruleName) {
		super(from, to, ruleName);
	}

	protected void bindSocket(AbstractSelectableChannel channel) throws IOException {
		((DatagramChannel) channel).socket().bind(this.from);
	}

	protected void registerListening(AbstractSelectableChannel channel, Selector selector) throws ClosedChannelException {
		channel.register(selector, SelectionKey.OP_READ, new ClientRecord(to));
	}

	// noop, not necessary for udp
	protected void processAcceptable(SelectionKey key) throws IOException {}

	// noop, not necessary for udp
	protected void processConnectable(SelectionKey key) throws IOException {}

	protected void processWritable(SelectionKey key) throws IOException {
		DatagramChannel channel = (DatagramChannel) key.channel();
		ClientRecord clientRecord = (ClientRecord) key.attachment();
		clientRecord.writeBuffer.flip(); // Prepare buffer for sending
		channel.send(clientRecord.writeBuffer, clientRecord.toAddress);

		if (clientRecord.writeBuffer.remaining() > 0) {
			clientRecord.writeBuffer.compact();
		} else {
			key.interestOps(SelectionKey.OP_READ);
			clientRecord.writeBuffer.clear();
		}
	}

	protected void processReadable(SelectionKey key, ByteBuffer readBuffer) throws IOException {
		DatagramChannel channel = (DatagramChannel) key.channel();
		ClientRecord clientRecord = (ClientRecord) key.attachment();

		//ensure the buffer is empty
		readBuffer.clear();

		//receive the data
		channel.receive(readBuffer);

		//get read to write, then send
		readBuffer.flip();
		channel.send(readBuffer, clientRecord.toAddress);

		//if there is anything remaining in the buffer
		if (readBuffer.remaining() > 0) {
			clientRecord.writeBuffer.put(readBuffer);
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	// noop, not necessary for tcp
	protected void handleKeyIterAfter(Iterator<SelectionKey> it) {}

	static class ClientRecord {
		public SocketAddress toAddress;
		public ByteBuffer writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);

		public ClientRecord(SocketAddress toAddress) {
			this.toAddress = toAddress;
		}
	}
}
