package cs283.appstoapps;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity extends Activity {

	private static final Integer TOTAL_POINTS = 7;
	private String group;
	private String name;
	private boolean isChooser = false;
	private Map<String, Integer> members;
	private Map<String, TextView> scores;
	private Map<Button, String> myCards;
	private Map<String, String> redCards;
	private TextView gCard;
	private TextView chosenCard;
	Vector<TextView> rows;
	
	static public Handler handler;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		Intent intent = getIntent();
		group = intent.getStringExtra("group");
		String ppl = intent.getStringExtra("players");
		int numPpl = intent.getIntExtra("numPlayers", 4);
		
		name = MainActivity.mt.name;
		members = new HashMap<String, Integer>();
		redCards = new HashMap<String, String>();
		scores = new HashMap<String, TextView>();
		myCards = new HashMap<Button, String>();
		chosenCard = (TextView) findViewById(R.id.yourCard);
		gCard = (TextView) findViewById(R.id.greenCard);
		
		
		handler = new Handler () {

			@Override
			public void handleMessage(Message msg) {
				String message = msg.obj.toString();
				Log.i("Apples", "Game Hanlder message: " + message);
				
				if (message.startsWith("RDRAW")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					String card = st.nextToken();
					
					addCard(card);
				} else if (message.startsWith("GDRAW")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					String chooser = st.nextToken().trim();
					String card = st.nextToken();

					indicateChooser(chooser);
					
					gCard.setText(card);
					
					if (chooser.equals(name)) {
						isChooser = true;
						pickCards();
					}
				} else if (message.startsWith("SUBMIT")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					String cName = st.nextToken();
					String card = st.nextToken();
					
					redCards.put(card, cName);
					getSubmittedCards(card, cName);
				} else if (message.startsWith("PICKED")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					String winner = st.nextToken().trim();
					String card = st.nextToken();
					
					roundOver(winner, card);
					if (isChooser) {
						drawGreenCard();
						isChooser = false;
					}
					
				} else if (message.startsWith("CARD")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					int numCards = Integer.parseInt(st.nextToken());
					String cards = st.nextToken();
					
					for (int i = 0; i < numCards - 1; ++i) {
						cards = cards + "\n" + st.nextToken();
					}
				}
			}
		};

		MainActivity.mt.setGameHandler(handler);
		
		populateTable(ppl, numPpl);
		
		chosenCard.setText("");
		((TextView) findViewById(R.id.winnerText)).setText("");
		
		initializeButtons();
		
		for (int i = 0; i < 7; ++i) {
			drawCard();
		}
		
		if (ppl.trim().startsWith(name)) {
			drawGreenCard();
		}
	}

	protected void indicateChooser(String chooser) {
		if (((TextView) findViewById(R.id.player1)).getText().toString().trim().equals(chooser)) 
			((TextView) findViewById(R.id.player1Choose)).setText("*");
		else 
			((TextView) findViewById(R.id.player1Choose)).setText(" ");
		
		if (((TextView) findViewById(R.id.player2)).getText().toString().trim().equals(chooser)) 
			((TextView) findViewById(R.id.player2Choose)).setText("*");
		else 
			((TextView) findViewById(R.id.player2Choose)).setText(" ");
		
		if (((TextView) findViewById(R.id.player3)).getText().toString().trim().equals(chooser))
			((TextView) findViewById(R.id.player3Choose)).setText("*");
		else  
			((TextView) findViewById(R.id.player3Choose)).setText(" ");
		
		if (((TextView) findViewById(R.id.player4)).getText().toString().trim().equals(chooser))
			((TextView) findViewById(R.id.player4Choose)).setText("*");
		else
			((TextView) findViewById(R.id.player4Choose)).setText(" ");
		
		if (((TextView) findViewById(R.id.player5)).getText().toString().trim().equals(chooser)) 
			((TextView) findViewById(R.id.player5Choose)).setText("*");
		else 
			((TextView) findViewById(R.id.player5Choose)).setText(" ");
		
		if (((TextView) findViewById(R.id.player6)).getText().toString().trim().equals(chooser))
			((TextView) findViewById(R.id.player6Choose)).setText("*");
		else
			((TextView) findViewById(R.id.player6Choose)).setText(" ");
		
		if (((TextView) findViewById(R.id.player7)).getText().toString().trim().equals(chooser))
			((TextView) findViewById(R.id.player7Choose)).setText("*");
		else
			((TextView) findViewById(R.id.player7Choose)).setText(" ");
		
		if (((TextView) findViewById(R.id.player8)).getText().toString().trim().equals(chooser))
			((TextView) findViewById(R.id.player8Choose)).setText("*");
		else
			((TextView) findViewById(R.id.player8Choose)).setText(" ");
	}

	private void roundOver(String winner, String card) {
		TextView winnerView = (TextView) findViewById(R.id.winnerText);
		winnerView.setText("Winner: " + winner + "\nCard: " + card);
		winnerView.setVisibility(View.VISIBLE);
		
		int newScore = members.get(winner) + 1;
		
		members.remove(winner);
		members.put(winner, newScore);

		scores.get(winner).setText("" + newScore);
		
		if (members.get(winner) == TOTAL_POINTS) {
			endGame(winner);
			if (name.equals(winner)) {
				String msg = "WIN " + group + " " + winner;
				
				new AsyncTask<String, Void, Void> () {
	
					@Override
					protected Void doInBackground(String... params) {
						InetSocketAddress serverSocketAddress = new InetSocketAddress(
								MyThread.SERVER_ADDR, MyThread.SERVER_PORT);
						
						String payload = params[0];
						
		                try {	 
			                DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
									payload.length(), serverSocketAddress);
							// send the packet through the socket to the server
							MainActivity.mt.socket.send(txPacket);    
						} catch (IOException e) {
							Log.i("Apples", "" + e);
						}	
						return null;
					}	
				}.execute(msg);	
			}
		}
	}

	private void endGame(String winner) {
		String message = winner + " Won the game!";
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}

	private void getSubmittedCards(String card, String cName) {
		int cards = 0;
		for (Button b : myCards.keySet()) {
			++cards;
			if(b.getText().toString().equals("")) {
				b.setText(card);
				b.setClickable(true);
				break;
			}
		}
		if (cards == members.size() - 1) {
			((Button) findViewById(R.id.buttonSubmit)).setClickable(true);
		}
	}

	private void initializeButtons() {
		myCards.put((Button) findViewById(R.id.button1), "");
		myCards.put((Button) findViewById(R.id.button2), "");
		myCards.put((Button) findViewById(R.id.button3), "");
		myCards.put((Button) findViewById(R.id.button4), "");
		myCards.put((Button) findViewById(R.id.button5), "");
		myCards.put((Button) findViewById(R.id.button6), "");
		myCards.put((Button) findViewById(R.id.button7), "");
		
		for (Button b : myCards.keySet()) {
			b.setClickable(false);
			//b.setText("");
		}
	}
	
	private void addCard(String card) {
		for (Button b : myCards.keySet()) {
			if (myCards.get(b).equals("")) {
				myCards.put(b, card);
				b.setText(card);
				b.setClickable(true);
				return;
			}
		}
	}

	private void populateTable(String ppl, int numPpl) {
		rows = new Vector<TextView>();
		rows.add((TextView) findViewById(R.id.player1));
		rows.add((TextView) findViewById(R.id.player2));
		rows.add((TextView) findViewById(R.id.player3));
		rows.add((TextView) findViewById(R.id.player4));
		rows.add((TextView) findViewById(R.id.player5));
		rows.add((TextView) findViewById(R.id.player6));
		rows.add((TextView) findViewById(R.id.player7));
		rows.add((TextView) findViewById(R.id.player8));
		
		StringTokenizer st = new StringTokenizer(ppl);
		
		String nextName;
		for (int i = 0; i < numPpl; ++i) {
			nextName = st.nextToken();
			members.put(nextName, 0);
		}
		
		int i = 0;
		for (String s : members.keySet()) {
			rows.get(i).setText(" " + s + " ");
			populateScores(s);
			++i;
		}
		
		if (rows.get(4).getText().toString().startsWith("Player"))
			((TextView) findViewById(R.id.player5Score)).setVisibility(View.INVISIBLE);
		if (rows.get(5).getText().toString().startsWith("Player"))
			((TextView) findViewById(R.id.player6Score)).setVisibility(View.INVISIBLE);
		if (rows.get(6).getText().toString().startsWith("Player"))
			((TextView) findViewById(R.id.player7Score)).setVisibility(View.INVISIBLE);
		if (rows.get(7).getText().toString().startsWith("Player"))
			((TextView) findViewById(R.id.player8Score)).setVisibility(View.INVISIBLE);
		
		for (TextView r : rows) {
			if (r.getText().toString().startsWith("Player"))
				r.setVisibility(View.INVISIBLE);
		}
	}	
	
	private void populateScores(String player) {
		TextView tv = (TextView) findViewById(R.id.player1Score);
		
		if (((TextView) findViewById(R.id.player1)).getText().toString().trim().equals(player))
			tv = (TextView) findViewById(R.id.player1Score);
		else if (((TextView) findViewById(R.id.player2)).getText().toString().trim().equals(player))
			tv = (TextView) findViewById(R.id.player2Score);
		else if (((TextView) findViewById(R.id.player3)).getText().toString().trim().equals(player))
			tv = (TextView) findViewById(R.id.player3Score);
		else if (((TextView) findViewById(R.id.player4)).getText().toString().trim().equals(player))
			tv = (TextView) findViewById(R.id.player4Score);
		else if (((TextView) findViewById(R.id.player5)).getText().toString().trim().equals(player))
			tv = (TextView) findViewById(R.id.player5Score);
		else if (((TextView) findViewById(R.id.player6)).getText().toString().trim().equals(player))
			tv = (TextView) findViewById(R.id.player6Score);
		else if (((TextView) findViewById(R.id.player7)).getText().toString().trim().equals(player))
			tv = (TextView) findViewById(R.id.player7Score);
		else
			tv = (TextView) findViewById(R.id.player8Score);
		
		scores.put(player, tv);
	}

	private void pickCards() {
		for (Button b : myCards.keySet()) {
			b.setClickable(false);
			b.setText("");
		}
		((Button) findViewById(R.id.buttonSubmit)).setClickable(false);
	}

	private void drawCard() {
		String msg = "RDRAW " + group + " " + name;
		
		new AsyncTask<String, Void, Void> () {

			@Override
			protected Void doInBackground(String... params) {
				InetSocketAddress serverSocketAddress = new InetSocketAddress(
						MyThread.SERVER_ADDR, MyThread.SERVER_PORT);
				
				String payload = params[0];
				
                try {	 
	                DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
							payload.length(), serverSocketAddress);
					// send the packet through the socket to the server
					MainActivity.mt.socket.send(txPacket);    
				} catch (IOException e) {
					Log.i("Apples", "" + e);
				}	
				return null;
			}	
		}.execute(msg);	
	}

	private void drawGreenCard() {
		String msg = "GDRAW " + group;
		
		new AsyncTask<String, Void, Void> () {

			@Override
			protected Void doInBackground(String... params) {
				InetSocketAddress serverSocketAddress = new InetSocketAddress(
						MyThread.SERVER_ADDR, MyThread.SERVER_PORT);
				
				String payload = params[0];
				Log.i("Apples", "Green card payload: " + payload);
				
                try {	 
	                DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
							payload.length(), serverSocketAddress);
					// send the packet through the socket to the server
					MainActivity.mt.socket.send(txPacket);    
				} catch (IOException e) {
					Log.i("Apples", "" + e);
				}	
				return null;
			}	
		}.execute(msg);	
		
	}
	
	public void choose1(View v) {
		String text = ((Button) findViewById(R.id.button1)).getText().toString();
		chosenCard.setText(text);		
	}
	
	public void choose2(View v) {
		String text = ((Button) findViewById(R.id.button2)).getText().toString();
		chosenCard.setText(text);		
	}
	
	public void choose3(View v) {
		String text = ((Button) findViewById(R.id.button3)).getText().toString();
		chosenCard.setText(text);		
	}
	
	public void choose4(View v) {
		String text = ((Button) findViewById(R.id.button4)).getText().toString();
		chosenCard.setText(text);		
	}
	
	public void choose5(View v) {
		String text = ((Button) findViewById(R.id.button5)).getText().toString();
		chosenCard.setText(text);		
	}
	
	public void choose6(View v) {
		String text = ((Button) findViewById(R.id.button6)).getText().toString();
		chosenCard.setText(text);		
	}
	
	public void choose7(View v) {
		String text = ((Button) findViewById(R.id.button7)).getText().toString();
		chosenCard.setText(text);		
	}

	public void submitCard(View v) {
		String msg = "";
		String card = chosenCard.getText().toString().trim();
		if (isChooser) {
			String winner = redCards.get(card);
			msg = "PICKED " + group + " " + winner + " " + card;
			
			for(Button b : myCards.keySet()) {
				b.setClickable(true);
				b.setText(myCards.get(b));
			}
		} else {
			msg = "SUBMIT " + group + " " + name + " " + card;
			for(Button b : myCards.keySet()) {
				if (myCards.get(b).equals(card)) {
					myCards.put(b, "");
					b.setText("");
				}
			}
		}
				
		new AsyncTask<String, Void, Void> () {

			@Override
			protected Void doInBackground(String... params) {
				InetSocketAddress serverSocketAddress = new InetSocketAddress(
						MyThread.SERVER_ADDR, MyThread.SERVER_PORT);
				
				String payload = params[0];
				
                try {	 
	                DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
							payload.length(), serverSocketAddress);
					// send the packet through the socket to the server
					MainActivity.mt.socket.send(txPacket);    
				} catch (IOException e) {
					Log.i("Apples", "" + e);
				}	
				return null;
			}	
		}.execute(msg);	
		
		if(!isChooser) {
			drawCard();
		}
	}
}
