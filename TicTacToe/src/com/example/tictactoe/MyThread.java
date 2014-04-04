package com.example.tictactoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.StringTokenizer;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MyThread extends Thread {

	public static final int MAX_PACKET_SIZE = 512;
	public static final int SERVER_PORT = 20003;
	public static final String SERVER_ADDR = "54.186.236.119";

	public DatagramPacket rxPacketIn;
	public DatagramSocket socket;

	public String name;
	public String opp;

	public Handler mainHandler;

	public MyThread(String user, Handler h) {
		this.name = user;
		this.mainHandler = h;
		this.socket = null;
		try {
			this.socket = new DatagramSocket();
		} catch (Throwable e) {
			Log.i("Netowrk Thread", "" + e);
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

			payloadOut = "REGISTER " + name;

			DatagramPacket nPacket = new DatagramPacket(payloadOut.getBytes(),
					payloadOut.length(), serverSocketAddress);
			// send the packet through the socket to the server
			socket.send(nPacket);

			// accept ACK
			socket.receive(rxPacketIn);
			
			payloadOut = "JOIN " + name + " WaitingRoom";
			DatagramPacket jPacket = new DatagramPacket(payloadOut.getBytes(),
					payloadOut.length(), serverSocketAddress);
			// send the packet through the socket to the server
			socket.send(jPacket);
			
			// accept ACK
			socket.receive(rxPacketIn);

			while (true) {

				socket.receive(rxPacketIn);

				payloadIn = new String(rxPacketIn.getData(), 0,
						rxPacketIn.getLength()).trim();
			
				if (payloadIn.startsWith("POS")) {
					StringTokenizer st = new StringTokenizer(payloadIn);
					st.nextToken(); // removes CMD
					String position = st.nextToken();

					// Send message to MainActivity
					Message msg = Message.obtain();
					msg.obj = "POS " + position;
					mainHandler.sendMessage(msg);

					// Acknowledge message received
					payloadOut = "DONE " + name;
				} else if (payloadIn.startsWith("ACK")
						|| payloadIn.startsWith("CLEARED")) {
					continue; // If Message received was an ACK (or Cleared for DONE) do nothing
				} else {
					StringTokenizer st = new StringTokenizer(payloadIn);
					opp = st.nextToken();
					String myStart = st.nextToken();
				
					// Send message to MainActivity
					Message msg = Message.obtain();
					msg.obj = "OPP " + opp + " " + myStart;
					mainHandler.sendMessage(msg);
					
					// Acknowledge message received
					payloadOut = "DONE " + name;
				}
				
				DatagramPacket txPacket = new DatagramPacket(
						payloadOut.getBytes(), payloadOut.length(),
						serverSocketAddress);
				
				// send the packet through the socket to the server
				socket.send(txPacket);
			}
		} catch (SocketException e) {
			Log.i("Network Thread", "" + e);
		} catch (IOException e) {
			Log.i("Network Thread", "" + e);
		}
	}
}
