package bananabank.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.StringTokenizer;


public class WorkerThread extends Thread 
{
	private static final String HOST_IP = "127.0.0.1";
	Socket clientSocket;
	BananaBank bank;
	ServerSocket servSock;
	WorkerThread[] threads;

	public WorkerThread(Socket cs, BananaBank b, ServerSocket ss, WorkerThread[] wt) {
		this.clientSocket = cs;
		this.bank = b;
		this.servSock = ss;
		this.threads = wt;
	}

	@Override
	public void run() {
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			PrintStream ps = new PrintStream(clientSocket.getOutputStream());
			String line;
			
			while ((line = r.readLine()) != null)
			{
				//System.out.println(line);
				
				if (line.equals("SHUTDOWN"))
				{
					if (clientSocket.getLocalAddress().getHostAddress().equals(HOST_IP))
					{
						servSock.close();
						for(WorkerThread t: threads)
						{
							if (t != Thread.currentThread())
							{
								t.join();
							}
						}
						
						bank.save("accounts.txt");
						
						Collection<Account> accts = bank.getAllAccounts();
						int total = 0;
						
						for(Account a : accts)
						{
							total = total + a.getBalance();
						}
						ps.println(total);
					}
					else
					{
						ps.println("You are not authorized to shutdown.");
					}
				}
				else
				{
					StringTokenizer st = new StringTokenizer(line);
					int amt = Integer.parseInt(st.nextToken());
					int src = Integer.parseInt(st.nextToken());
					int dst = Integer.parseInt(st.nextToken());
					
					int first = src, last = dst;
					if(src > dst)
					{
						first = dst;
						last = src;
					}
					
					synchronized(bank.getAccount(first))
					{
						synchronized(bank.getAccount(last))
						{
							Account srcA = bank.getAccount(src);
							Account dstA = bank.getAccount(dst);
							if (srcA == null)
							{
								System.out.println("Invalid source account");
								ps.println("failure");
							}
							else if (dstA == null)
							{
								System.out.println("Invalid destination account");
								ps.println("failure");
							}
							else if (amt > srcA.getBalance())
							{
								System.out.println("Insufficient funds");
								ps.println("failure");
							}
							else
							{
								srcA.transferTo(amt, dstA);
								ps.println("success");
								//System.out.println("SRC: " + srcA.getBalance() + "\nDST: " + dstA.getBalance());
							}
						} //end synch
					} //end synch
				} // end if
			} //end while
			r.close();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
