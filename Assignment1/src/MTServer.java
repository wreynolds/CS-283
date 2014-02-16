import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class MTServer {

	public static final int PORT = 7777;
	
	public static void main(String[] args) throws IOException, InterruptedException {

		ServerSocket ss = new ServerSocket(PORT);
		System.out.println("MAIN: ServerSocket created");
		for(;;) {
		//	System.out.println("MAIN: Waiting for client connection on port " + PORT);				
			Socket cs = ss.accept();
		//	System.out.println("MAIN: Client connected");
			WorkerThread wt = new WorkerThread(cs);
			wt.start();
			//wt.join();
		}
		
	}

}
