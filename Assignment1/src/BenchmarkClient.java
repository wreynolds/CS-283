import java.io.IOException;
import java.net.UnknownHostException;

 
public class BenchmarkClient {
	
	private static final int NUM_CT = 4;
	private static final int NUM_REQS = 1000;
	private static final int HOST = 7777;

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException 
	{
		long start = System.currentTimeMillis();
		ClientThread[] ct = new ClientThread[NUM_CT];
		
		for (int i = 0; i < NUM_CT; ++i) 
		{
			ct[i] = new ClientThread(NUM_REQS, HOST);
			ct[i].start();
		}
		for(int i = 0; i < NUM_CT; ++i)
		{
			ct[i].join();
		}
		
		
		long end = System.currentTimeMillis();
		double avgTime = (end - start)/(double)(NUM_REQS*NUM_CT);
		
		System.out.println("The average time per request is " + avgTime + " ms.");
	}

}
