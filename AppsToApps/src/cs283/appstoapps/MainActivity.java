package cs283.appstoapps;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.StringTokenizer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	static public Handler handler;
	static public MyThread mt;
	private boolean isCreator;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		handler = new Handler () {

			@Override
			public void handleMessage(Message msg) {
				String message = msg.obj.toString();
				
				if (message.startsWith("REG")) {
					StringTokenizer st = new StringTokenizer(message);
					st.nextToken(); // removes CMD
					String ans = st.nextToken();
					String name = st.nextToken();
					
					EditText edit = (EditText) findViewById(R.id.nameText);
					TextView subtitle = (TextView) findViewById(R.id.player1Score);
					
					if (ans.equals("N")) {
						subtitle.setText("You cannot be " + name + ".");
						edit.setText("Choose another name");
					} else {
						Intent myIntent = new Intent(MainActivity.this, GroupActivity.class);
						myIntent.putExtra("isCreator", isCreator);
						MainActivity.this.startActivity(myIntent);
					}
				} 
			}
		};
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onDestroy() {
		String msg = "UNREG " + mt.name;
		mt.kill();
		
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
					mt.socket.send(txPacket);    
				} catch (IOException e) {
					Log.i("Apples", "" + e);
				}	
				return null;
			}	
		}.execute(msg);	
		
		super.onDestroy();
	}

	public void startGroup (View v) {
		isCreator = ((CheckBox) findViewById(R.id.checkBoxGrpStart)).isChecked();
	}

	public void enterName (View v) {
		EditText namet = (EditText) findViewById(R.id.nameText);
		String name = namet.getText().toString();
		
		mt = new MyThread(name, handler);
		mt.start();
	}
}
