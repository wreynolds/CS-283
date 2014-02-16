import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class ClientThread extends Thread {

	Socket sock = null;
	int reqs = 0;
	int hostPort = 0;
	
	public ClientThread(int requests, int host) {
		reqs = requests;
		hostPort = host;
	}

	public void run() 
	{
		PrintStream ps = null;
		BufferedReader r = null;

		for (int i = 0; i < reqs; ++i) 
		{
			try {
				sock = new Socket("localhost", hostPort);

				System.out.println("CLIENT: Connected to the host at " + hostPort);

				ps = new PrintStream(sock.getOutputStream());
				r = new BufferedReader(new InputStreamReader(
						sock.getInputStream()));
				String word = "hello world";

				System.out.println("CLIENT: Sending \"" + word + "\"");

				ps.println(word);
				word = r.readLine();

				System.out.println("CLIENT: Received \"" + word + "\"");
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ps.close();
		}
	}
}
