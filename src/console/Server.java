package console;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import core.CommunicationCore;
import core.MsgReceivedHandler;

public class Server {

	public static void main(String[] args) throws IOException {
		println("easyChatJava Console Server.");
		String username = "defaultPlayer";
		String host = "localhost";
		int port = 5566;

		Scanner s = new Scanner(System.in);
		println("play name?");
		username = s.next();

		if (args.length < 1) {

			println("host to bind?");
			host = s.next();

			println("port to listen?");
			port = s.nextInt();

		} else if (args.length == 1) {

			port = Integer.parseInt(args[0]);

		} else if (args.length >= 2) {

			host = args[0];
			port = Integer.parseInt(args[1]);

		}

		println("Start Server:" + host + ":" + port);
		println("Starting...");
		CommunicationCore server = CommunicationCore.getInstance();
		server.setMsgHandler(new MsgReceivedHandler() {
			@Override
			public void msgReceived(String msg) {
				println(msg);
			}
		});
		server.startAsServer(host, port);
		println("Server started.");

		println("Input msg please. Type \"!exit\" to exit.");
		while (true) {
			String in = s.next();
			if (in.equals("!exit")) {
				server.stop();
				System.exit(0);
			}
			Date now = new Date();
			SimpleDateFormat formater = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss");
			server.sendMsg(formater.format(now) + " " + username + ":" + in);
		}

	}

	public static void println(Object o) {
		System.out.println(o);
	}
}
