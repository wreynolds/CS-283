package bananabank.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;



public class SimpleClient {

	public static void main(String[] args) throws UnknownHostException, IOException 
	{
		Socket s = new Socket("localhost", 7777);
		PrintStream ps = new PrintStream(s.getOutputStream());
		BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
		
		String word = "2 44444 11111";
		ps.println(word);
		word = r.readLine();
		System.out.println("Transfer was a " + word);

		word = "2 44444 11111";
		ps.println(word);
		word = r.readLine();
		System.out.println("Transfer was a " + word);
		
		word = "2 44444 11111";
		ps.println(word);
		word = r.readLine();
		System.out.println("Transfer was a " + word);
		
		word = "2 44444 11111";
		ps.println(word);
		word = r.readLine();
		System.out.println("Transfer was a " + word);
		
		word = "SHUTDOWN";
		ps.println(word);
		word = r.readLine();
		System.out.println("Transfer was a " + word);

		
		ps.close();
		r.close();
	}
}
