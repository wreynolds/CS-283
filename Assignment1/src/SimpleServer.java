import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;


public class SimpleServer {

	public static final int PORT = 3333;
	
	public static void main(String[] args) throws IOException
	{
		ServerSocket ss = new ServerSocket(PORT);
		System.out.println("MIAN: Socket Server set up.");
		
		while (true) 
		{
			Socket cs = ss.accept();
			//System.out.println("MAIN: Client connected.");
			
			BufferedReader r = new BufferedReader(new InputStreamReader(
					cs.getInputStream()));
			PrintStream ps = new PrintStream(cs.getOutputStream());
			String line;
			
			while ((line = r.readLine()) != null) 
			{
				ps.println(line.toUpperCase());
			}
			
			//System.out.println("MAIN: Client Disconnected.");
			r.close();
			ps.close();
		}
	}
}
