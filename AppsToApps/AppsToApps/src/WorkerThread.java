import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;


public class WorkerThread extends Thread{

	private DatagramPacket rxPacket;
	public static DatagramSocket socket;

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

		if (payload.startsWith("REG")) {
			onRegisterRequested(payload);
			return;
		}

		if (payload.startsWith("NEW")) {
			onNewGroup(payload);
			return;
		}

		if (payload.startsWith("ADD")) {
			onAddName(payload);
			return;
		}
		
		if(payload.startsWith("START")) {
			onStartGame(payload);
			return;
		}
		
		if(payload.startsWith("RDRAW")) {
			onRdraw(payload);
			return;
		}
		
		if(payload.startsWith("GDRAW")) {
			onGdraw(payload);
			return;
		}
		
		if(payload.startsWith("SUBMIT")) {
			onSubmit(payload);
			return;
		}
		
		if(payload.startsWith("PICKED"))  {
			onPick(payload);
			return;
		}
		
		if(payload.startsWith("WIN")) {
			onWin(payload);
			return;
		}
		
		if(payload.startsWith("UNREG")) {
			onUnregister(payload);
			return;
		}
		
		if(payload.startsWith("RESET")) {
			onReset(payload);
			return;
		}
		
		
		if (payload.startsWith("SHUTDOWN")) {
			onShutdown(payload);
			return;
		}
		

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
	
	//command: (REG name)
	private void onRegisterRequested(String payload) {
		//tokenize the payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken();
		String name = st.nextToken();
		
		if(Server.Lobby.contains(name)) {
			try {
				send("REG N " + name, this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		ClientEndPoint cl = new ClientEndPoint(this.rxPacket.getAddress(), this.rxPacket.getPort());
		Server.clientEndPoints.add(cl);
		Server.nameTOclient.put(name, cl);
		Server.Lobby.add(name);
		try {
			send("REG Y " + name, this.rxPacket.getAddress(), this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	//command: (NEW name groupname)
	private void onNewGroup(String payload) {
		//tokenize the payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken();
		String name = st.nextToken();
		String group = st.nextToken();
		
		if(Server.groupTOnames.containsKey(group)) {
			try {
				send("NEW N " + group, this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Server.groupTOnames.put(group, new Vector<String>());
		Server.groupTOnames.get(group).add(name);
		Queue<String> q = new LinkedList<String>();
		Server.groupTOqueue.put(group, q);
		Server.groupTOqueue.get(group).add(name);
		
		try {
			send("NEW Y " + group, this.rxPacket.getAddress(), this.rxPacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	//command: (ADD name groupname)
	private void onAddName(String payload) {
		//tokenize the payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken();
		String name = st.nextToken();
		String group = st.nextToken();
		
		if(!Server.Lobby.contains(name) || !Server.groupTOnames.containsKey(group)) {
			try {
				send("ADD N " + name, this.rxPacket.getAddress(), this.rxPacket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		Server.Lobby.remove(name);
		Server.groupTOnames.get(group).add(name);
		Server.groupTOqueue.get(group).add(name);
		try {
			send("ADD Y " + name, this.rxPacket.getAddress(), this.rxPacket.getPort());
			send("GROUP " + group + " " + name, Server.nameTOclient.get(name).address, Server.nameTOclient.get(name).port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	//command: (START groupname)
	private void onStartGame(String payload) {
		
		
		//tokenize the payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken();
		String group = st.nextToken();
		
		Queue<String> r = new LinkedList<String>();
		Queue<String> g = new LinkedList<String>();
		
		Server.groupTOred.put(group, r);
		Server.groupTOgreen.put(group, g);
//		System.out.println("green done.");
		
		Rshuffle(group);
		Gshuffle(group);
		
//		System.out.println(Server.groupTOgreen.get(group).size());
//		System.out.println(Server.groupTOred.get(group).size());
		
		Server.groupTOsubmitted.put(group, new Vector<String>());
		String info = "" + Server.groupTOnames.get(group).size();
		for (String n : Server.groupTOnames.get(group)) {
			info += (" " + n);
		}
		
		for (String n : Server.groupTOnames.get(group)) {
			try {
				send("NAMES " + info, Server
						.nameTOclient.get(n).address, Server.nameTOclient.get(n).port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
//		for(String n : Server.groupTOnames.get(group)) {
//			for(int i = 0; i < 7 ; i++) {
//				onRdraw("RDRAW " + group + " " + n);
//			}
//		}
//		
//		String chooser = Server.groupTOqueue.get(group).peek();
//		onGdraw("GDRAW " + group + " " + chooser);
	}
	
	//command: (RDRAW groupname name)
	private void onRdraw(String payload) {
		//tokenize the payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken();
		String group = st.nextToken();
		String name = st.nextToken();
		
		if(Server.groupTOred.get(group).isEmpty()) {
//			System.out.println("shuffled");
			Rshuffle(group);
		}
		else {
//			System.out.println("not shuffled");
		}
		String card = Server.groupTOred.get(group).remove();
		try {
			send("RDRAW " + card, Server.nameTOclient.get(name).address, Server.nameTOclient.get(name).port);
//			System.out.println("card sent to " + name);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//command: (GDRAW groupname)
	private void onGdraw(String payload) {
		//tokenize the payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken();
		String group = st.nextToken();
		
		if(Server.groupTOgreen.get(group).isEmpty()) {
			Gshuffle(group);
		}
		
		String card = Server.groupTOgreen.get(group).remove();
		for(String n : Server.groupTOnames.get(group)) {
			try {
				send("GDRAW " + Server.groupTOqueue.get(group).peek() + " " + card, Server
						.nameTOclient.get(n).address, Server.nameTOclient.get(n).port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//command: (SUBMIT groupname name card)
	private void onSubmit(String payload) {
		//tokenize the payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken();
		String group = st.nextToken();
		String name = st.nextToken();
		String card = st.nextToken();
		
		Server.groupTOsubmitted.get(group).add(card);
		try {
			String chooser = Server.groupTOqueue.get(group).peek();
			send("SUBMIT " + name + " " + card, Server
					.nameTOclient.get(chooser).address, Server.nameTOclient.get(chooser).port);
	//		onRdraw("RDRAW " + group + " " + name);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//command: (PICKED groupname name card)
	private void onPick(String payload) {
		//tokenize the payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken();
		String group = st.nextToken();
		String name = st.nextToken();
		String card = st.nextToken();
		
//		String totalCards = "" + Server.groupTOsubmitted.size();
//		for(String c : Server.groupTOsubmitted.get(group)) {
//			totalCards = " " + c;
//		}
		
		for(String n : Server.groupTOnames.get(group)) {
			try {
				send("PICKED " + name + " " + card, Server
						.nameTOclient.get(n).address, Server.nameTOclient.get(n).port);
//				send("CARD " + totalCards, Server
//						.nameTOclient.get(n).address, Server.nameTOclient.get(n).port);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Server.groupTOsubmitted.get(group).clear();
		String oldChooser = Server.groupTOqueue.get(group).remove();
		Server.groupTOqueue.get(group).add(oldChooser);
		String newChooser = Server.groupTOqueue.get(group).peek();
//		onGdraw("GDRAW " + group + " " + newChooser);
	}
	
	//command: (WIN groupname name)
	private void onWin(String payload) {
		//tokenize the payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken();
		String group = st.nextToken();
		String name = st.nextToken();
		for(String n : Server.groupTOnames.get(group)) {
			Server.Lobby.add(n);
		}
		Server.groupTOnames.remove(group);
		Server.groupTOqueue.remove(group);
		Server.groupTOsubmitted.remove(group);
		Server.groupTOred.remove(group);
		Server.groupTOgreen.remove(group);
	}
	
	//command: (UNREG name)
	private void onUnregister(String payload) {
		//tokenize the payload
		StringTokenizer st = new StringTokenizer(payload);
		st.nextToken();
		String name = st.nextToken();
		
		Server.nameTOclient.remove(name);
		
	}
	
	//command: (RESET)
	private void onReset(String payload) {
		Server.Lobby.clear();
		Server.nameTOclient.clear();
		Server.groupTOnames.clear();
		Server.groupTOqueue.clear();
		Server.groupTOsubmitted.clear();
		Server.groupTOred.clear();
		Server.groupTOgreen.clear();
	}
	
	//command: (SHUTDOWN)
	private void onShutdown(String payload) {
		try {
			send("SHUTDOWN STARTING\n", this.rxPacket.getAddress(), this.rxPacket.getPort());
			if(this.rxPacket.getAddress().isAnyLocalAddress()) {
				send("SHUTTING DOWN\n", this.rxPacket.getAddress(), this.rxPacket.getPort());
				this.socket.close();
			}
			else {
				send("SHUTDOWN NOT ALLOWED\n", this.rxPacket.getAddress(), this.rxPacket.getPort());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void onBadRequest(String payload) {
		try {
			send("BADREQUEST\n", this.rxPacket.getAddress(),
					this.rxPacket.getPort());
		} catch (IOException e) { 
			e.printStackTrace();
		}
	}
	
	protected static void Rshuffle(String group) {
		Set<Integer> temp = new HashSet<Integer>(); 
		Random rand = new Random();
		int x = rand.nextInt(Server.Rcards.size());
		Server.groupTOred.get(group).add(Server.Rcards.get(x));
		temp.add(x);
		while(temp.size() != Server.Rcards.size()) {
			int y = rand.nextInt(Server.Rcards.size());
			if(!temp.contains(y)) {
				Server.groupTOred.get(group).add(Server.Rcards.get(y));
				temp.add(y);
			}
		}
	}
	
	protected static void Gshuffle(String group) {		
		Set<Integer> temp = new HashSet<Integer>(); 
		Random rand = new Random();
		int x = rand.nextInt(Server.Gcards.size());
		Server.groupTOgreen.get(group).add(Server.Gcards.get(x));
		temp.add(x);
		while(temp.size() != Server.Gcards.size()) {
			int y = rand.nextInt(Server.Gcards.size());
			if(!temp.contains(y)) {
				Server.groupTOgreen.get(group).add(Server.Gcards.get(y));
				temp.add(y);
			}
		}
	}
	
}
