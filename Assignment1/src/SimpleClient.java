import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;



public class SimpleClient {

	public static void main(String[] args) throws UnknownHostException, IOException 
	{
		Socket s = new Socket("localhost", 3333);
		PrintStream ps = new PrintStream(s.getOutputStream());
		BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
		String word = "hello world";
		
		System.out.println("Client sending: " + word);
		
		ps.println(word);
		word = r.readLine();
		
		System.out.println("Cleint received: " + word);
		
		ps.close();
		r.close();
	}

}
