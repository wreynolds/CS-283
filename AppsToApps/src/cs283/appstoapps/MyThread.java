package cs283.appstoapps;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MyThread extends Thread {

	public static final int MAX_PACKET_SIZE = 512;
	public static final int SERVER_PORT = 20010;
	public static final String SERVER_ADDR = "54.186.236.119";

	public DatagramPacket rxPacketIn;
	public DatagramSocket socket;

	public String name;
	public String group;
	
	public Handler mainHandler;
	public Handler groupHandler;
	public Handler gameHandler;
	
	private boolean isDead = false;

	public MyThread(String user, Handler h) {
		this.name = user;
		this.mainHandler = h;
		this.socket = null;
		try {
			this.socket = new DatagramSocket();
		} catch (Throwable e) {
			Log.i("Apples", "" + e);
		}
	}
	
	@Override
	public void run() {
		InetSocketAddress serverSocketAddress = new InetSocketAddress(
				SERVER_ADDR, SERVER_PORT);

		try {
			byte[] buf = new byte[MAX_PACKET_SIZE];
			rxPacketIn = new DatagramPacket(buf, buf.length);

			String payloadIn = "";
			String payloadOut = "";

			payloadOut = "REG " + name;

			DatagramPacket nPacket = new DatagramPacket(payloadOut.getBytes(),
					payloadOut.length(), serverSocketAddress);
			// send the packet through the socket to the server
			socket.send(nPacket);
			
			// accept ACK
			socket.receive(rxPacketIn);
			payloadIn = new String(rxPacketIn.getData(), 0,
					rxPacketIn.getLength()).trim();
		
			Message msg1 = Message.obtain();
			msg1.obj = payloadIn;
			mainHandler.sendMessage(msg1);
			
			while (true) {
				socket.receive(rxPacketIn);
				
				if (isDead) {
					break;
				}
				
				payloadIn = new String(rxPacketIn.getData(), 0,
						rxPacketIn.getLength()).trim();
				
				if (payloadIn.startsWith("REG")) {
					Message msg = Message.obtain();
					msg.obj = payloadIn;
					mainHandler.sendMessage(msg);
				} else if (payloadIn.startsWith("NAMES") ||
						payloadIn.startsWith("NEW") ||
						payloadIn.startsWith("ADD") ||
						payloadIn.startsWith("GROUP")) {
					
					Message msg = Message.obtain();
					msg.obj = payloadIn;
					groupHandler.sendMessage(msg);
				} else if (payloadIn.startsWith("RDRAW") ||
						payloadIn.startsWith("GDRAW") ||
						payloadIn.startsWith("SUBMIT") ||
						payloadIn.startsWith("PICKED") ||
						payloadIn.startsWith("CARD")) {
					
					Message msg = Message.obtain();
					msg.obj = payloadIn;
					gameHandler.sendMessage(msg);
				}
					
			}
		} catch (SocketException e) {
			Log.i("Apples", "" + e);
		} catch (IOException e) {
			Log.i("Apples", "" + e);
		} catch (Throwable e) {
			Log.i("Apples", "" + e);
		}
	}

	
	public void setGroupHandler(Handler newHandler) {
		groupHandler = newHandler;
	}
	
	public void setGameHandler(Handler newHandler) {
		gameHandler = newHandler;
	}
	
	public void kill() {
		isDead = true;
		socket.close();
	}
}
