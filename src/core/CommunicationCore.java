package core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommunicationCore {

	private List<Socket> clients = new ArrayList<Socket>();
	private Set<Socket> diedClients = new HashSet<Socket>();
	private ServerSocket server;
	private ServerListenerDaemon listenerDaemon;
	private MsgTransportDaemon msgDaemon;
	private boolean startedAsServer = false;
	private boolean started = false;
	private long SLEEP = 500;
	private ByteArrayOutputStream receivedMsgBuff = new ByteArrayOutputStream();
	private MsgReceivedHandler msgHandler;

	public void setMsgHandler(MsgReceivedHandler msgHandler) {
		this.msgHandler = msgHandler;
	}

	private CommunicationCore() {

	}

	public static CommunicationCore getInstance() {
		return new CommunicationCore();
	}

	public void startAsServer(String host, int port) throws IOException {
		startedAsServer = true;
		started = true;

		server = new ServerSocket();
		server.bind(new InetSocketAddress(InetAddress.getByName(host), port));

		listenerDaemon = new ServerListenerDaemon();
		listenerDaemon.setDaemon(true);
		listenerDaemon.start();

		startMsgDaemon();
	}

	public void startAsServer(int port) throws IOException {
		startAsServer(null, port);
	}

	public void startAsClient(String host, int port)
			throws UnknownHostException, IOException {
		if (started || startedAsServer)
			return;
		startedAsServer = false;
		started = true;

		Socket client = new Socket();
		client
				.connect(new InetSocketAddress(InetAddress.getByName(host),
						port));
		clients.add(client);

		startMsgDaemon();

	}

	public void stop() {
		startedAsServer = false;
		started = false;
	}

	private void startMsgDaemon() {
		msgDaemon = new MsgTransportDaemon();
		msgDaemon.setDaemon(true);
		msgDaemon.start();
	}

	public void sendMsg(String msg) {
		if (!started || clients.size() == 0)
			return;

		for (Socket client : clients) {
			try {
				OutputStream output = client.getOutputStream();
				byte[] buff = msg.getBytes();
				output.write(buff);
				output.flush();
			} catch (IOException e) {
				e.printStackTrace();
				diedClients.add(client);
			}

		}
	}

	public String receiveMsg() {
		String msg = new String(receivedMsgBuff.toByteArray());
		receivedMsgBuff.reset();
		return msg;
	}

	private void removeDiedClients() {
		for (Socket s : diedClients) {
			clients.remove(s);
		}
		diedClients.clear();
	}

	private class ServerListenerDaemon extends Thread {

		@Override
		public void run() {
			if (server == null)
				return;

			while (startedAsServer) {
				try {
					clients.add(server.accept());
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(SLEEP);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private class MsgTransportDaemon extends Thread {

		@Override
		public void run() {
			while (started) {
				for (Socket client : clients) {
					if (client.isClosed() || client.isInputShutdown()
							|| client.isOutputShutdown()
							|| !client.isConnected()) {
						diedClients.add(client);
						continue;
					}

					InputStream input = null;
					byte[] buff = null;

					try {
						input = client.getInputStream();
					} catch (IOException e) {
						e.printStackTrace();
						diedClients.add(client);
					}
					if (input == null)
						continue;

					try {
						if (input.available() == 0)
							continue;

						buff = new byte[input.available()];
						input.read(buff);

						if (msgHandler != null) {
							msgHandler.msgReceived(new String(buff));
						} else {
							receivedMsgBuff.write(buff);
						}

					} catch (IOException e) {
						e.printStackTrace();
						diedClients.add(client);
					}

					if (buff == null)
						continue;

					for (Socket otherClient : clients) {

						if (otherClient.equals(client))
							continue;

						OutputStream output = null;
						try {
							output = otherClient.getOutputStream();
						} catch (IOException e) {
							e.printStackTrace();
							diedClients.add(otherClient);
						}

						if (output == null)
							continue;

						try {
							output.write(buff);
							output.flush();
						} catch (IOException e) {
							e.printStackTrace();
							diedClients.add(otherClient);
						}

					}

				}

				removeDiedClients();

				try {
					Thread.sleep(SLEEP);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			for (Socket c : clients) {
				try {
					c.shutdownInput();
					c.shutdownOutput();
					c.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
