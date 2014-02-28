package bananabank.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class MTServer {

	private static final int PORT = 7777;
	private static final int NUM_WT = 8; //for simple client NUM_WT = 1
	private static final String BANK_F = "accounts.txt";
	
	public static void main(String[] args) throws InterruptedException, IOException {

		ServerSocket ss = new ServerSocket(PORT);
		WorkerThread[] wt = new WorkerThread[NUM_WT];
		BananaBank bank = new BananaBank(BANK_F);
		
		try 
		{
			for(int i = 0; true; i = (i + 1) % NUM_WT) 
			{
				Socket cs;
				cs = ss.accept();
				
				wt[i] = new WorkerThread(cs, bank, ss, wt);
				wt[i].start();
			}
		} 
		catch (IOException e) 
		{
		}
		
		for(WorkerThread t : wt)
		{
			t.join();
		}
		//System.out.println("Server shutdown.");
	}

}
