package com.bitsblender.votingclient;

import android.support.v7.app.ActionBarActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

@SuppressLint({ "HandlerLeak", "NewApi" })
public class MainActivity extends ActionBarActivity {

	EditText ip, port, username, password;
	Button connectBtn;
	Socket socket;
	private InputStream socketInStream;
	private OutputStream socketOutStream;
	static final int SUCCESS_CONNECT = 0;
	static final int MSG_RECV = 1;
	Handler handler;
	String selectedCandiadate = "";
	ReceiveingThread receiveingThread;
	SendingThread sendingThread;
	AlertDialog.Builder votingDialogBuilder;
	ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ip = (EditText) findViewById(R.id.IP);
		port = (EditText) findViewById(R.id.port);
		username = (EditText) findViewById(R.id.username);
		password = (EditText) findViewById(R.id.password);
		connectBtn = (Button) findViewById(R.id.connect_btn);
		connectBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ConnectThread thread = new ConnectThread();
				thread.start();
			}
		});
		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				super.handleMessage(msg);
				switch (msg.what) {
				case SUCCESS_CONNECT:

					sendingThread = new SendingThread(
							username.getText().toString() + "~" + password.getText().toString());
					sendingThread.run();
					progressDialog = ProgressDialog.show(MainActivity.this, "Please Wait...",
							"Contacting Voting Servers", true);
					progressDialog.setCancelable(false);
					receiveingThread = new ReceiveingThread();
					receiveingThread.start();
					break;
				case MSG_RECV:
					boolean pollingStatus = false;
					String receivedMessage = (String) msg.obj;
					if (receivedMessage.startsWith("Results")) {
						Toast.makeText(getApplicationContext(), receivedMessage, Toast.LENGTH_LONG).show();
					} else {
						String[] messageParts = receivedMessage.split("~");
						int i;
						for (i = 0; i < messageParts.length; i++) {
							if (messageParts[i].compareTo("candidates") == 0) {
								pollingStatus = true;
								break;
							} else {
								Toast.makeText(getApplicationContext(), messageParts[i], Toast.LENGTH_SHORT).show();
							}
						}
						i++;
						if (pollingStatus) {
							selectedCandiadate = "";
							final CharSequence[] candidates = new String[messageParts.length - i];
							System.out.println(messageParts.length - i);
							for (; i < messageParts.length; i++) {
								System.out.println(messageParts.length - i - 1);
								candidates[messageParts.length - i - 1] = messageParts[i];
								System.out.println(candidates[messageParts.length - i - 1]);
							}
							votingDialogBuilder = new AlertDialog.Builder(MainActivity.this);
							votingDialogBuilder.setTitle("Candidates List");
							votingDialogBuilder.setSingleChoiceItems(candidates, -1,
									new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									selectedCandiadate = (String) candidates[which];
									Toast.makeText(getApplicationContext(), selectedCandiadate, 0).show();
								}
							});

							votingDialogBuilder.setPositiveButton("Vote Now", new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (selectedCandiadate.compareTo("") == 0) {
										Toast.makeText(getApplicationContext(),
												"Please Choose Candidate to cast your vote", Toast.LENGTH_LONG).show();
										try {
											socket.close();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									} else {
										sendingThread = new SendingThread("candidate~" + selectedCandiadate);
										sendingThread.run();
									}
								}
							});

							votingDialogBuilder.setCancelable(false);
							AlertDialog votingDialog = votingDialogBuilder.create();
							votingDialog.show();
						}
						progressDialog.dismiss();
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
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class ConnectThread extends Thread {

		@Override
		public void run() {
			String dstAddress = ip.getText().toString();
			int dstPort = Integer.parseInt(port.getText().toString());
			// System.out.println("Waiting for " + dstAddress + ":" +
			// selectedPortTxt.getText().toString());
			try {
				socket = new Socket(dstAddress, dstPort);
				// ByteArrayOutputStream byteArrayOutputStream = new
				// ByteArrayOutputStream(1024);
				// byte[] buffer = new byte[1024];
				// int bytesRead;
				socketInStream = socket.getInputStream();
				socketOutStream = socket.getOutputStream();
				/*
				 * notice: inputStream.read() will block if no data return
				 */
				/*
				 * while ((bytesRead = socketInStream.read(buffer)) != -1) {
				 * byteArrayOutputStream.write(buffer, 0, bytesRead); response
				 * += byteArrayOutputStream.toString("UTF-8"); }
				 */
				/*
				 * socketInStream.read(buffer); response = new String(buffer);
				 * System.out.println(response);
				 */
				handler.obtainMessage(SUCCESS_CONNECT).sendToTarget();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();/*
									 * response = "UnknownHostException: " +
									 * e.toString();
									 * handler.obtainMessage(CONNECTION_TIMEDOUT
									 * ).sendToTarget();
									 */
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				/*
				 * response = "IOException: " + e.toString();
				 * handler.obtainMessage(CONNECTION_TIMEDOUT).sendToTarget();
				 */
			} catch (Exception e) {
				/* handler.obtainMessage(CONNECTION_TIMEDOUT).sendToTarget(); */
			}

		}
	}

	private class SendingThread implements Runnable {

		String msg;

		public SendingThread(String messaage) {
			msg = messaage + "~";
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {

				synchronized (ACCESSIBILITY_SERVICE) {

					byte[] buffer = new byte[msg.length()];
					socketOutStream.flush();
					buffer = msg.getBytes();
					socketOutStream.write(buffer);
					socketOutStream.flush();
				}

			} catch (

			IOException e)

			{
				e.printStackTrace();
				finish();
			}
		}

	}

	private class ReceiveingThread extends Thread {
		public void run() {

			// Keep listening to the InputStream until an exception occurs
			try {
				read();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void read() throws IOException {
			try {

				{
					byte[] buffer; // buffer store for the stream
					// bytes returned from read()
					buffer = new byte[1024];
					// Read from the InputStream
					int bytes = socketInStream.read(buffer);
					buffer = trimBuffer(buffer, bytes);
					handler.obtainMessage(MSG_RECV, new String(buffer)).sendToTarget();
				}
			}

			catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		private byte[] trimBuffer(byte[] buffer, int size) {
			if (size < 0) {
				return null;
			}

			byte[] tempBuffer = new byte[size];
			for (int i = 0; i < size; i++) {
				tempBuffer[i] = buffer[i];
			}
			return tempBuffer;
		}
	}

}
