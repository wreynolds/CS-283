package udpgroupchat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

public class WorkerThread extends Thread {

	private DatagramPacket rxPacket;
	private DatagramSocket socket;

	public WorkerThread(DatagramPacket packet, DatagramSocket socket) {
		this.rxPacket = packet;
		this.socket = socket;
	}

	@Override
	public void run() {
		// convert the rxPacket's payload to a string
		String payload = new String(rxPacket.getData(), 0, rxPacket.getLength())
				.trim();

		// dispatch request handler functions based on the payload's prefix

		if (payload.startsWith("REGISTER")) {
			onRegisterRequested(payload);
			return;
		}

		if (payload.startsWith("UNREGISTER")) {
			onUnregisterRequested(payload);
			return;
		}
		
		if (payload.startsWith("UPDATE_IP")) {
			onUpdateIPRequested(payload);
			return;
		}

		if (payload.startsWith("SEND")) {
			onSendRequested(payload);
			return;
		}

		if (payload.startsWith("JOIN")) {
			onJoinRequested(payload);
			return;
		}
		
		if (payload.startsWith("LEAVE")) {
			onLeaveRequested(payload);
			return;
		}
		
		if (payload.startsWith("GROUP_SEND")) {
			onSendGroupRequested(payload);
			return;
		}
		
		if (payload.startsWith("ACKNOWLEDGED")) {
			onAcknowledgedRequested(payload);
			return;
		}
		
		if (payload.startsWith("PULL")) {
			onPullRequested(payload);
			return;
		}
		
		if (payload.startsWith("LIST_GROUPS")) {
			onListGroupsRequested(payload);
			return;
		}
		
		if (payload.startsWith("LIST_USERS")) {
			onListUsersRequested(payload);
			return;
		}
		
		if (payload.startsWith("LIST_MEMBERS")) {
			onListMembersRequested(payload);
			return;
		}
		
		if (payload.startsWith("SHUTDOWN")) {
			onShutdownRequested(payload);
			return;
		}
		
		//
		// implement other request handlers here...
		//

		// if we got here, it must have been a bad request, so we tell the
		// client about it
		onBadRequest(payload);
	}

	// send a string, wrapped in a UDP packet, to the specified remote endpoint
	public void send(String payload, InetAddress address, int port)
			throws IOException {
		DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
				payload.length(), address, port);
		this.socket.send(txPacket);
	}

	//Payload Contents: CMD, CLIENT_NAME 
	private void onRegisterRequested(String payload) {
		// get the address of the sender from the rxPacket
		InetAddress address = this.rxPacket.getAddress();
		// get the port of the sender from the rxPacket
		int port = this.rxPacket.getPort();
		
		// separate payload
		StringTokenizer st = new StringTokenizer(payload, ",");
		st.nextToken(); // removes CMD
		String name = st.nextToken().trim();
		
		if(Server.nameToClient.containsKey(name)) {
			try {
				send("> ERROR:: " + name + " is already registered, "
						+ "please choose another name.\n", address, port);
				
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		ClientEndPoint newClient = new ClientEndPoint(address, port);
		// create a client object, and put it in the map that assigns names
		// to client objects
		Server.clientEndPoints.add(newClient);
		// note that calling clientEndPoints.add() with the same endpoint info
		// (address and port)
		// multiple times will not add multiple instances of ClientEndPoint to
		// the set, because ClientEndPoint.hashCode() is overridden. See
		// http://docs.oracle.com/javase/7/docs/api/java/util/Set.html for
		// details.
		
		// Associate name and client
		Server.nameToClient.put(name, newClient);
		// Initialize client queue
		Server.nameToMsg.put(name, new Vector<String>());

		// tell client we're OK
		try {
			send("> REGISTERED\n> \tNAME: " + name + "\n", address, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Payload contents: CMD, name
	private void onUnregisterRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();		
		ClientEndPoint clientEndPoint = new ClientEndPoint(address, port);

		StringTokenizer st = new StringTokenizer(payload, ",");
		st.nextToken(); // removes CMD
		String userName = st.nextToken().trim();
		
		// check if client is in the set of registered clientEndPoints
		if (Server.nameToClient.containsKey(userName) && 
				Server.clientEndPoints.contains(Server.nameToClient.get(userName))) {
			
			if (!address.equals(Server.nameToClient.get(userName).address) || 
					port != Server.nameToClient.get(userName).port) {
				
				try {
					send("> UNAUTHORIZED TO UNREGISTER USER - " + userName + "\n", 
							address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			} 
			
			// yes, remove it
			Server.clientEndPoints.remove(clientEndPoint);
			Server.nameToClient.remove(userName);
			Server.nameToMsg.remove(userName);
			
			for(Set<String> s: Server.groupsToNames.values()) {
				if (s.contains(userName)) {
					s.remove(userName);
				}
			}
			
			try {
				send("> UNREGISTERED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// no, send back a message
			try {
				send("> CLIENT NOT REGISTERED\n", this.rxPacket.getAddress(),
						this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Payload contents: CDM, NAME
	private void onUpdateIPRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();
		
		StringTokenizer st = new StringTokenizer(payload, ",");
		st.nextToken(); // removes CMD
		String userName = st.nextToken().trim();
		
		if (!Server.nameToClient.containsKey(userName)) {
			try {
				send("> CLIENT NOT REGISTERED\n", 
						this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return;
		}
		
		ClientEndPoint user = Server.nameToClient.get(userName);
		if (!user.address.equals(address) || user.port != port) {
			Server.clientEndPoints.remove(user);
			user = new ClientEndPoint(address, port);
			Server.clientEndPoints.add(user);
			Server.nameToClient.remove(userName);
			Server.nameToClient.put(userName, user);
		}
		
		try {
			send("> IP Adress Updated\nSending a PULL request\n", address, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		onPullRequested(payload); //Automatically send a pull request
	}
	
	// Payload contents: CDM, NAME, DST_NAME, MSG"
	private void onSendRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();
		
		// Separate payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken(); // removes CMD
		String userName = st.nextToken(",").trim();
		String dstName = st.nextToken(",").trim();
		String message = st.nextToken("\n").trim();
		
		message = message.substring(2);
		
		if (Server.nameToClient.containsKey(userName) && 
				Server.clientEndPoints.contains(Server.nameToClient.get(userName))) {
		
			if (!address.equals(Server.nameToClient.get(userName).address) || 
					port != Server.nameToClient.get(userName).port) {
				
				try {
					send("> ERROR::YOU ARE NOT USER - " + userName + "\n", 
							address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			
			
			ClientEndPoint dst = Server.nameToClient.get(dstName);
			
			try {
				message = userName + ": " + message + "\n"; 
				send("> " + message, dst.address, dst.port);
				Server.nameToMsg.get(dstName).add(message);
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (!Server.nameToMsg.get(dstName).isEmpty()) {
					send("> " + Server.nameToMsg.get(dstName).lastElement(), 
							dst.address, dst.port);
				}
				
				send("> SUCCESS::Message sent to " + dstName + "\n", 
						address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				send("> CLIENT NOT REGISTERED\n", address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Payload contents: CMD, USER_NAME, GROUP_NAME
	private void onJoinRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();
		
		StringTokenizer st = new StringTokenizer(payload, ",");
		st.nextToken(); // removes CMD
		String userName = st.nextToken().trim();
		String groupName = st.nextToken().trim();
		
		if (Server.nameToClient.containsKey(userName) && 
				Server.clientEndPoints.contains(Server.nameToClient.get(userName))) {
		
			if (Server.groupsToNames.containsKey(groupName)) {
				Server.groupsToNames.get(groupName).add(userName);
				try {
					send("> " + userName + " added to " + groupName + "\n", 
							address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				Server.groupsToNames.put(groupName, new HashSet<String>());
				Server.groupsToNames.get(groupName).add(userName);
				
				try {
					send("> " + groupName + " successfully created.\n", 
							address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (!address.equals(Server.nameToClient.get(userName).address) || 
					port != Server.nameToClient.get(userName).port) {
			
				try {
					String message = "You have been added to the group "
							+ groupName + "\n";
					send("> " + message, Server.nameToClient.get(userName).address, 
							Server.nameToClient.get(userName).port);
					Server.nameToMsg.get(userName).add(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			
		} else {
			try {
				send("> CLIENT NOT REGISTERED\n", address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	// Payload contents: CMD, USER_NAME, GROUP_NAME
	private void onLeaveRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();
		
		StringTokenizer st = new StringTokenizer(payload, ",");
		st.nextToken(); // removes CMD
		String userName = st.nextToken().trim();
		String groupName = st.nextToken().trim();
		
		if (Server.nameToClient.containsKey(userName) && 
				Server.clientEndPoints.contains(Server.nameToClient.get(userName))) {
		
			if (!address.equals(Server.nameToClient.get(userName).address) ||
					port != Server.nameToClient.get(userName).port) {
				
				try {
					send("> ERROR::You are not authorized to remove " + userName + "\n", 
							address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (Server.groupsToNames.containsKey(groupName)) {
				Server.groupsToNames.get(groupName).remove(userName);
			}
			
			try {
				send("> " + userName + " removed from " + groupName + "\n", 
						address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				send("> CLIENT NOT REGISTERED\n", address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Payload contents: CMD, USER_NAME, GROUP_NAME, MSG"
	private void onSendGroupRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();
		
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken(","); // removes CMD
		String userName = st.nextToken(",").trim();
		String groupName = st.nextToken(",").trim();
		String message = st.nextToken("\n").trim();
		
		message = message.substring(2);
		message = groupName + ":\n>\t" + userName + ": " + message + "\n";
		
		if (Server.nameToClient.containsKey(userName) && 
				Server.clientEndPoints.contains(Server.nameToClient.get(userName))) {
		
			if (!Server.groupsToNames.get(groupName).contains(userName)) {
				try {
					send("> ERROR::" + userName + " is not a member of " + groupName +
							". Message not sent.\n", address, port);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			
			if (!address.equals(Server.nameToClient.get(userName).address) ||
					port != Server.nameToClient.get(userName).port) {
				
				try {
					send("> ERROR::YOU ARE NOT USER - " + userName + "\n", 
							address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			
			ClientEndPoint dst;
			for (String s: Server.groupsToNames.get(groupName)) {
				if (!s.equals(userName)) {
					dst = Server.nameToClient.get(s);
					try {
						send("> " + message, dst.address, dst.port);
						Server.nameToMsg.get(s).add(message);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}		
			}
			
			try {
				send("> SUCCESS::Message sent to " + groupName + "\n", address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			for (String s: Server.groupsToNames.get(groupName)) {
				if (!s.equals(userName)) {
					if (!Server.nameToMsg.get(s).isEmpty()) {
						dst = Server.nameToClient.get(s);
						try {
							send("> " +  Server.nameToMsg.get(s).lastElement(),
									dst.address, dst.port);
							Server.nameToMsg.get(s).add(message);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}		
			}		
			
		} else {
			try {
				send("> CLIENT NOT REGISTERED\n", address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Payload Contents: CMD, USER_NAME
	private void onAcknowledgedRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();
		
		StringTokenizer st = new StringTokenizer(payload, ",");
		st.nextToken(); // removes CMD
		String userName = st.nextToken().trim();
		
		if (Server.nameToClient.containsKey(userName) && 
				Server.clientEndPoints.contains(Server.nameToClient.get(userName))) {
			
			if (!address.equals(Server.nameToClient.get(userName).address) ||
					port != Server.nameToClient.get(userName).port) {
				
				try {
					send("> ERROR::YOU ARE NOT USER - " + userName + "\n", 
							address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			
			Server.nameToMsg.get(userName).clear();
		} else {
			try {
				send("> CLIENT NOT REGISTERED\n", address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Payload contents: CMD, USERNAME
	private void onPullRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();
		
		StringTokenizer st = new StringTokenizer(payload, ",");
		st.nextToken(); // removes CMD
		String userName = st.nextToken().trim();
		
		if (Server.nameToClient.containsKey(userName) && 
				Server.clientEndPoints.contains(Server.nameToClient.get(userName))) {
		
			if (!address.equals(Server.nameToClient.get(userName).address) ||
					port != Server.nameToClient.get(userName).port) {
				
				try {
					send("> ERROR::YOU ARE NOT USER - " + userName + "\n", 
							address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			
			if (Server.nameToMsg.get(userName).isEmpty()) {
				try {
					send("> No messages to receive.\n", address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			
			int i = 1;
			for (String s: Server.nameToMsg.get(userName)) {
				if (i == Server.nameToMsg.get(userName).size()) {
					s = s + "> END OF MESSAGES\n";
				}
				
				try {
					send(">" + i + "\t" + s, address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				++i;
			}
		} else {
			try {
				send("> CLIENT NOT REGISTERED\n", address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Payload contents: CMD, USER_NAME
	private void onListGroupsRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();
		
		StringTokenizer st = new StringTokenizer(payload, ",");
		st.nextToken(); // removes CMD
		String userName = st.nextToken().trim();
		
		if (Server.nameToClient.containsKey(userName) && 
				Server.clientEndPoints.contains(Server.nameToClient.get(userName))) {
		
			if (!address.equals(Server.nameToClient.get(userName).address) ||
					port != Server.nameToClient.get(userName).port) {
				
				try {
					send("> ERROR::YOU ARE NOT USER - " + userName + "\n", 
							address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
						
			if (Server.groupsToNames.isEmpty()) {
				try {
					send("> There are no groups registered\n", address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			
			String message = "GROUPS:\n";
			for (String s: Server.groupsToNames.keySet()) {
				if (Server.groupsToNames.get(s).contains(userName)) {
					s = "+" + s;
				} else {
					s = "-" + s;
				}
				message = message + ">\t" + s + "\n";
			}
			
			try {
				send("> " + message, address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				send("> CLIENT NOT REGISTERED\n", address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Payload contents: CMD
	private void onListUsersRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();
		
		if (Server.nameToClient.isEmpty()) {
			try {
				send("> THERE ARE NO REGISTERED USERS\n", address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		String message = "> REGISTERED USERS:\n";
		
		for (String s: Server.nameToClient.keySet()) {
			message = message + ">\t " + s + "\n";
		}
		
		try {
			send(message, address, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Payload contents: CMD, USER_NAME, GROUP_NAME
	private void onListMembersRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		int port = this.rxPacket.getPort();
		
		StringTokenizer st = new StringTokenizer(payload, ",");
		st.nextToken(); // removes CMD
		String userName = st.nextToken().trim();
		String groupName = st.nextToken().trim();
		
		if (Server.nameToClient.containsKey(userName) && 
				Server.clientEndPoints.contains(Server.nameToClient.get(userName))) {
		
			if (Server.groupsToNames.isEmpty()) {
				try {
					send("> There are no groups registered\n", address, port);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			
			String message = groupName + ":\n";
			for (String s: Server.groupsToNames.get(groupName)) {
				message = message + ">\t" + s + "\n";
			}
			
			try {
				send("> " + message, address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				send("> CLIENT NOT REGISTERED\n", address, port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Payload contents: SHUTDOWN
	private void onShutdownRequested(String payload) {
		InetAddress address = this.rxPacket.getAddress();
		InetAddress localHost = null;
		try {
			localHost = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		if (address.equals(localHost))
		{
			try {
				send("SHUTTING DOWN\nWAITING FOR ALL ACTIVE CLIENTS TO TERMINATE\n", 
						this.rxPacket.getAddress(), this.rxPacket.getPort());
				Server.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		else
		{
			try {
				send("UNAUTHORIZED TO SHUT DOWN\n", this.rxPacket.getAddress(), 
						this.rxPacket.getPort());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void onBadRequest(String payload) {
		try {
			send("BAD REQUEST\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
