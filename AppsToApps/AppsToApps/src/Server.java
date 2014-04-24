import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;



public class Server {
	
	// constants
	public static final int DEFAULT_PORT = 20010;
	public static final int MAX_PACKET_SIZE = 512;

	// port number to listen on
	protected int port;
	
	static DatagramSocket socket = null;
	
	//data members
	protected static final Set<ClientEndPoint> clientEndPoints = Collections
			.synchronizedSet(new HashSet<ClientEndPoint>());
	protected static final Set<String> Lobby = Collections.synchronizedSet(new HashSet<String>());
	protected static final Map<String, ClientEndPoint> nameTOclient  = Collections
			.synchronizedMap(new HashMap<String, ClientEndPoint>());
	protected static final Map<String, 	Vector<String>> groupTOnames = Collections
			.synchronizedMap(new HashMap<String, Vector<String>>());
	protected static final Map<String, Queue<String>> groupTOqueue = Collections
			.synchronizedMap(new HashMap<String, Queue<String>>());
	protected static final Map<String, Queue<String>> groupTOred = Collections
			.synchronizedMap(new HashMap<String, Queue<String>>());
	protected static final Map<String, Queue<String>> groupTOgreen = Collections
			.synchronizedMap(new HashMap<String, Queue<String>>());
	protected static final Map<String, Vector<String>> groupTOsubmitted = Collections
			.synchronizedMap(new HashMap<String, Vector<String>>());
	protected static final Vector<String> Rcards = new Vector<String>();
	protected static final Vector<String> Gcards = new Vector<String>();
	
	// constructor
	Server(int port) {
		this.port = port;      
	}

	// start up the server
	public void start() {
		try {
			// create a datagram socket, bind to port port. See
			// http://docs.oracle.com/javase/tutorial/networking/datagrams/ for
			// details.

			socket = new DatagramSocket(port);

			// receive packets in an infinite loop
			while (true) {
				// create an empty UDP packet
				byte[] buf = new byte[Server.MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				// call receive (this will poulate the packet with the received
				// data, and the other endpoint's info)
				socket.receive(packet);
				// start up a worker thread to process the packet (and pass it
				// the socket, too, in case the
				// worker thread wants to respond)
				WorkerThread t = new WorkerThread(packet, socket);
				t.start();
			}
		} catch (IOException e) {
			// we jump out here if there's an error, or if the worker thread (or
			// someone else) closed the socket
			e.printStackTrace();
		} finally {
			if (socket != null && !socket.isClosed())
				socket.close();
		}
	}

	// main method
	public static void main(String[] args) {
		int port = Server.DEFAULT_PORT;
		try {
			BufferedReader brRed = new BufferedReader(new FileReader("RedCards.txt"));
			String card = brRed.readLine();
			while(card != null) {
				Server.Rcards.add(card);
				card = brRed.readLine();
			}
			
			BufferedReader brGreen = new BufferedReader(new FileReader("GreenCards.txt"));
			card = brGreen.readLine();
			while(card != null) {
				Server.Gcards.add(card);
				card = brGreen.readLine();
			}	
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// check if port was given as a command line argument
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("Invalid port specified: " + args[0]);
				System.out.println("Using default port " + port);
			}
		}

		// instantiate the server
		Server server = new Server(port);

		System.out
				.println("Starting server. Connect with netcat (nc -u localhost "
						+ port
						+ ") or start multiple instances of the client app to test the server's functionality.");
		// start it
		server.start();

	}
	

}
