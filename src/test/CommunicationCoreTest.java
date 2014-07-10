package test;

import java.io.IOException;
import java.net.UnknownHostException;

import core.CommunicationCore;

public class CommunicationCoreTest {

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
			InterruptedException {
		// TODO Auto-generated method stub
		CommunicationCore server = CommunicationCore.getInstance();
		server.startAsServer(9988);
		CommunicationCore client = CommunicationCore.getInstance();
		client.startAsClient("localhost", 9988);
		client.sendMsg("12333");
		new Thread() {
			@Override
			public void run() {
				CommunicationCore client = CommunicationCore.getInstance();
				try {
					client.startAsClient("localhost", 9988);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				for (int i = 0; i < 100; i++) {
					try {
						client.sendMsg("sending..." + i);
						Thread.sleep(250);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

		for (int i = 0; i < 200; i++) {
			System.out.println(client.receiveMsg());
			Thread.sleep(500);
		}
	}

}
