package com.example.tictactoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.PorterDuff.Mode;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

@SuppressLint("UseSparseArrays")
public class MainActivity extends Activity {

	private MyThread mt;
	private Map<Integer, Button> buttonMap;
	
	private int turns = 0;
	
	public Handler handler;
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		EditText instr = (EditText) findViewById(R.id.startText);
		
		buttonMap = new HashMap<Integer, Button>();
		buttonMap.put(1, (Button) findViewById(R.id.button1));
		buttonMap.put(2, (Button) findViewById(R.id.button2));
		buttonMap.put(3, (Button) findViewById(R.id.button3));
		buttonMap.put(4, (Button) findViewById(R.id.button4));
		buttonMap.put(5, (Button) findViewById(R.id.button5));
		buttonMap.put(6, (Button) findViewById(R.id.button6));
		buttonMap.put(7, (Button) findViewById(R.id.button7));
		buttonMap.put(8, (Button) findViewById(R.id.button8));
		buttonMap.put(9, (Button) findViewById(R.id.button9));
		
		for (Button b : buttonMap.values()) {
			b.setClickable(false);
		}
		
		handler = new Handler () {

			@Override
			public void handleMessage(Message msg) {
				String message = msg.obj.toString();

				if (message.startsWith("POS")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					int position = Integer.parseInt(st.nextToken());
					newTurn(position);
				} else if (message.startsWith("OPP")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					String opp = st.nextToken();
					int myStart = Integer.parseInt(st.nextToken());
					
					EditText et = (EditText) findViewById(R.id.messageText);
					String str = "You are now playing " + opp + ".";
					et.setText(str);
					
					if (myStart == 1) { // go first
						for (Button b : buttonMap.values()) {
							b.setClickable(true);
							et.setText(str + "\nMake your first move.");
						}
					} else { // go second
						waitTurn();
					}		
				}
			}	
		};
		instr.setText("Please enter a name.");
	}

	@Override
	protected void onDestroy() {
	
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void enterName (View v) {
		EditText instr = (EditText) findViewById(R.id.startText);
		EditText namet = (EditText) findViewById(R.id.nameText);
		EditText msgText = (EditText) findViewById(R.id.messageText);
		Button submit = (Button) findViewById(R.id.buttonStart);
		
		String name = namet.getText().toString();
		msgText.setText("Your name is " + name + ".\nWaiting for another player");
		
		instr.setVisibility(View.INVISIBLE);
		namet.setVisibility(View.INVISIBLE);
		submit.setVisibility(View.INVISIBLE);
				
		mt = new MyThread(name, handler);
		mt.start();
	}
	
	public void sendMove(int position) {		
		String msg = "" + position;
		
		new AsyncTask<String, Void, Void> () {

			@Override
			protected Void doInBackground(String... params) {
				InetSocketAddress serverSocketAddress = new InetSocketAddress(
						MyThread.SERVER_ADDR, MyThread.SERVER_PORT);
				
				String payload = "POS " + mt.name + " " + mt.opp + " " + params[0];
				
                try {
					 
	                DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
							payload.length(), serverSocketAddress);
					// send the packet through the socket to the server
					mt.socket.send(txPacket);
				} catch (IOException e) {
					Log.i("Network Thread", "" + e);
				}
				return null;
			}
		}.execute(msg);	
		
		if (!checkForWin()) 
		{
			waitTurn();
		}
	}
	
	public void waitTurn() {
		EditText et = (EditText) findViewById(R.id.messageText);
		et.setText("Waiting for other player to move.");
		for (Button b : buttonMap.values()) {
			b.setClickable(false);
		}
	}
	
	public void newTurn(int pos) {
		// get message from network thread
		// set correct button to O
		buttonMap.get(pos).setText("O");
		buttonMap.get(pos).getBackground().setColorFilter(0xFFFF0000, Mode.MULTIPLY);
		if (!checkForWin()) {
			for (Button b : buttonMap.values()) {
				if (!b.getText().toString().equals("X") || 
						!b.getText().toString().equals("O")) {
					b.setClickable(true);
				}
			}
			
			EditText et = (EditText) findViewById(R.id.messageText);
			et.setText("Your Turn, please make a move.");
		}
	}
	
	public boolean checkForWin() {
		String b1 = buttonMap.get(1).getText().toString(),
				b2 = buttonMap.get(2).getText().toString(),
				b3 = buttonMap.get(3).getText().toString(),
				b4 = buttonMap.get(4).getText().toString(),
				b5 = buttonMap.get(5).getText().toString(),
				b6 = buttonMap.get(6).getText().toString(),
				b7 = buttonMap.get(7).getText().toString(),
				b8 = buttonMap.get(8).getText().toString(),
				b9 = buttonMap.get(9).getText().toString();
		
		++turns;
		
		if (b1.equals(b2) && b1.equals(b3)) { 
			if (b1.equals("X")) {
				endGame("win");
				return true;
			} else if (b1.equals("O")){ // b1 is "O"
				endGame("lose");
				return true;
			}
		} else if (b1.equals(b5) && b1.equals(b9)) {
			if (b1.equals("X")) {
				endGame("win");
				return true;
			} else if (b1.equals("O")){ // b1 is "O"
				endGame("lose");
				return true;
			}
		} else if (b1.equals(b4) && b1.equals(b7)) {
			if (b1.equals("X")) {
				endGame("win");
				return true;
			} else if (b1.equals("O")) { // b1 is "O"
				endGame("lose");
				return true;
			}
		} else if (b4.equals(b5) && b4.equals(b6)) {
			if (b4.equals("X")) {
				endGame("win");
				return true;
			} else if (b4.equals("O")){ // b4 is "O"
				endGame("lose");
				return true;
			}
		} else if (b2.equals(b5) && b2.equals(b8)) {
			if (b2.equals("X")) {
				endGame("win");
				return true;
			} else if (b2.equals("O")){ // b2 is "O"
				endGame("lose");
				return true;
			}
		} else if (b7.equals(b8) && b7.equals(b9)) {
			if (b7.equals("X")) {
				endGame("win");
				return true;
			} else if (b7.equals("O")){ // b7 is "O"
				endGame("lose");
				return true;
			}
		} else if (b7.equals(b5) && b7.equals(b3)) {
			if (b7.equals("X")) {
				endGame("win");
				return true;
			} else if (b7.equals("O")){ // b7 is "O"
				endGame("lose");
				return true;
			}
		} else if (b3.equals(b6) && b3.equals(b9)) {
			if (b3.equals("X")) {
				endGame("win");
				return true;
			} else if (b3.equals("O")){ // b3 is "O"
				endGame("lose");
				return true;
			}		
		} else if (turns == 9) { // DRAW
			endGame("draw");
			return true;
		}
		return false;
	}
	
	public void endGame(String status) {
		String message = "";
		turns = 0;
		
		if (status.equals("win")) {
			message = "Congratualtions! You won!";
		} else if (status.equals("lose")) {
			message = "You lost, better luck next time!";
		} else  { // draw
			message = "Nobody wins!";
		}
				
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
		EditText msgText = (EditText) findViewById(R.id.messageText);
		msgText.setText("Looking for new opponent");
		
		for (Button b : buttonMap.values()) {
			b.setText(null);
			b.getBackground().clearColorFilter();
			b.setClickable(false);
		}
		
		String msg =  "GAMEOVER";
		new AsyncTask<String, Void, Void> () {

			@Override
			protected Void doInBackground(String... params) {
				InetSocketAddress serverSocketAddress = new InetSocketAddress(
						MyThread.SERVER_ADDR, MyThread.SERVER_PORT);
				
				String payload = "JOIN " + mt.name + " WaitingRoom";
				
                try {	 
	                DatagramPacket txPacket = new DatagramPacket(payload.getBytes(),
							payload.length(), serverSocketAddress);
					// send the packet through the socket to the server
					mt.socket.send(txPacket);    
				} catch (IOException e) {
					Log.i("Network Thread", "" + e);
				}	
				return null;
			}	
		}.execute(msg);	
	}
	
	public void chosePosition1 (View v) {
		Button b = buttonMap.get(1);
		b.setText("X");
		b.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);
		b.setClickable(false);
		sendMove(1);
	}

	public void chosePosition2 (View v) {
		Button b = buttonMap.get(2);
		b.setText("X");
		b.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);
		b.setClickable(false);
		sendMove(2);
	}
	
	public void chosePosition3 (View v) {
		Button b = buttonMap.get(3);
		b.setText("X");
		b.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);
		b.setClickable(false);
		sendMove(3);
	}
	
	public void chosePosition4 (View v) {
		Button b = buttonMap.get(4);
		b.setText("X");
		b.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);
		b.setClickable(false);
		sendMove(4);
	}
	
	public void chosePosition5 (View v) {
		Button b = buttonMap.get(5);
		b.setText("X");
		b.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);
		b.setClickable(false);
		sendMove(5);
	}
	
	public void chosePosition6 (View v) {
		Button b = buttonMap.get(6);
		b.setText("X");
		b.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);
		b.setClickable(false);
		sendMove(6);
	}
	
	public void chosePosition7 (View v) {
		Button b = buttonMap.get(7);
		b.setText("X");
		b.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);
		b.setClickable(false);
		sendMove(7);
	}
	
	public void chosePosition8 (View v) {
		Button b = buttonMap.get(8);
		b.setText("X");
		b.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);
		b.setClickable(false);
		sendMove(8);
	}
	
	public void chosePosition9 (View v) {
		Button b = buttonMap.get(9);
		b.setText("X");
		b.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);
		b.setClickable(false);
		sendMove(9);
	}
}
