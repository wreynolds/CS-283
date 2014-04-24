package cs283.appstoapps;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff.Mode;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class GroupActivity extends Activity {

	private TextView body;
	private EditText adder;
	private String members;
	private String group;
	private String name;
	private static int numPlayers = 1;
	private static boolean created = false;
	
	static public Handler handler;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.activity_group);
		Intent intent = getIntent();
		Boolean isCreator = intent.getBooleanExtra("isCreator", false);
	   
		body = (TextView) findViewById(R.id.playersText);
		adder = (EditText) findViewById(R.id.editNameText);
		name = MainActivity.mt.name;
	   
		handler = new Handler () {

			@Override
			public void handleMessage(Message msg) {
				String message = msg.obj.toString();
								
				if (message.startsWith("NEW")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					String ans = st.nextToken();
					group = st.nextToken();
					
					EditText edit = (EditText) findViewById(R.id.editNameText);
					TextView title = (TextView) findViewById(R.id.addTitle);
					
					if (ans.equals("N")) {
						title.setText(group + " Already Exists");
						((TextView) findViewById(R.id.groupNameText)).setText("Choose another name");
						edit.getText().clear();
					} else {
						title.setText("Add Players");
						((TextView) findViewById(R.id.groupNameText)).setText(group);
						edit.getText().clear();
						created = true;
					}
				} else if (message.startsWith("ADD")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					String ans = st.nextToken();
					String name = st.nextToken();
					
					EditText edit = (EditText) findViewById(R.id.editNameText);
					TextView title = (TextView) findViewById(R.id.addTitle);
					
					if (ans.equals("N")) {
						title.setText(name + " cannot be added");
						((TextView) findViewById(R.id.groupNameText)).setText("Choose another name");
						edit.getText().clear();
					} else {
						title.setText("Add Players");
						edit.getText().clear();
						
						((TextView) findViewById(R.id.groupNameText)).setText(group);
						++numPlayers;
						
						members = members + "\n" + name;
						body.setText(members);
						
						if (numPlayers == 4) {
							Button start = (Button) findViewById(R.id.buttonStart);
							start.setClickable(true);
							start.getBackground().clearColorFilter();
						} else if (numPlayers == 8) {
							Button add = (Button) findViewById(R.id.buttonAdd);
							add.setClickable(false);
							add.getBackground().setColorFilter(0xFF7B7B7B, Mode.MULTIPLY);
							edit.setClickable(false);
							edit.setText("Start game.");
						}
					}
				} else if (message.startsWith("GROUP")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					group = st.nextToken();
					String admin = st.nextToken();
					
					TextView title = (TextView) findViewById(R.id.addTitle);
					
					title.setText("Waiting for admin to start");
					body.setText(admin + " added you to\n" + group);
					
					((TextView) findViewById(R.id.groupNameText)).setText(group);
				} else if (message.startsWith("NAMES")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					int numPlayer = Integer.parseInt(st.nextToken());
					
					String nextName;
					String players = "";
					for (int i = 0; i < numPlayer; ++i) {
						nextName = st.nextToken();
						players = players + " " + nextName;
					}
					
					beginGame(players, numPlayer);
				} 
			}	
		};
	   
		MainActivity.mt.setGroupHandler(handler);
		
		((TextView) findViewById(R.id.groupNameText)).setText("");
		
		if (isCreator) 
			gameAdmin();
		else
			player();
	}

	protected void beginGame(String players, int numPlayer) {
		Intent myIntent = new Intent(GroupActivity.this, GameActivity.class);
		myIntent.putExtra("group", group);
		myIntent.putExtra("players", players);
		myIntent.putExtra("numPlayers", numPlayer);
		GroupActivity.this.startActivity(myIntent);		
	}

	public void gameAdmin() {
		TextView title = (TextView) findViewById(R.id.addTitle);
		title.setText("Enter Group Name");
		
		Button start = (Button) findViewById(R.id.buttonStart);
		start.setClickable(false);
	
		members = name;
		body.setText(members);
	}
	
	public void player() {
		TextView title = (TextView) findViewById(R.id.addTitle);
		title.setText("Wait to be added to a group.");
	
		body.setText(name + ", ask your game admin to\nadd you to a group.");
		
		findViewById(R.id.editNameText).setVisibility(View.INVISIBLE);
		findViewById(R.id.buttonAdd).setVisibility(View.INVISIBLE);
		findViewById(R.id.buttonStart).setVisibility(View.INVISIBLE);
	}
	
	public void addPlayer(View v) {
		String msg;
		String str = adder.getText().toString();
		
		if (!created) {
			group = str;
			msg = "NEW " + name + " " + group;
		} else {
			msg = "ADD " + str + " " + group;
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
	}

	public void startGame(View v) {
		String msg = "START";
		
		new AsyncTask<String, Void, Void> () {

			@Override
			protected Void doInBackground(String... params) {
				InetSocketAddress serverSocketAddress = new InetSocketAddress(
						MyThread.SERVER_ADDR, MyThread.SERVER_PORT);
				
				String payload = "START " + group;
				
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
