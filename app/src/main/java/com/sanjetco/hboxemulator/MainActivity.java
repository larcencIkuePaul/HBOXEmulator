package com.sanjetco.hboxemulator;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements CommonDef {

    final static int CMD_PORT = 4000;
    final static int IMAGE_PORT = 4001;
    final static int BUF_SIZE = 256;

    ToggleButton mToggleBtnConnStartStop;
    Switch mSwitchImageTransmission;
    TextView mTextImageTransmissionStatus, mTextMsg;
    ServerSocket mCmdSocket, mImgSocket = null;
    boolean mConnStart, mImageStart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(DEBUGKW, "App START");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        initUiElements();
    }

    void initUiElements() {
        Log.d(DEBUGKW, "UI init. START");
        mToggleBtnConnStartStop = (ToggleButton) findViewById(R.id.toggle_btn_conn_start_stop);
        mSwitchImageTransmission = (Switch) findViewById(R.id.switch_send_image_start_stop);
        mTextImageTransmissionStatus = (TextView) findViewById(R.id.text_image_transmission_status);
        mTextMsg = (TextView) findViewById(R.id.text_msg);
        initClickListener();
    }

    void initClickListener() {
        Log.d(DEBUGKW, "Button click listener init. START");
        mToggleBtnConnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mToggleBtnConnStartStop.isChecked()) {
                    Log.d(DEBUGKW, "Button on");
                    mConnStart = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwitchImageTransmission.setChecked(true);
                            mSwitchImageTransmission.setClickable(true);
                            mTextImageTransmissionStatus.setText(R.string.title_image_transmission_status_start);
                        }
                    });
                } else {
                    Log.d(DEBUGKW, "Button off");
                    mConnStart = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwitchImageTransmission.setChecked(false);
                            mSwitchImageTransmission.setClickable(false);
                            mTextImageTransmissionStatus.setText(R.string.title_image_transmission_status_stop);
                        }
                    });
                }
            }
        });
        mSwitchImageTransmission.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mImageStart = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startCmdSocket();
                        }
                    }).start();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startImageSocket();
                        }
                    }).start();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextImageTransmissionStatus.setText(R.string.title_image_transmission_status_start);
                        }
                    });
                } else {
                    mImageStart = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextImageTransmissionStatus.setText(R.string.title_image_transmission_status_stop);
                        }
                    });
                }
            }
        });
    }

    void startCmdSocket() {
        try {
            while (mConnStart && mImageStart) {
                mCmdSocket = new ServerSocket(CMD_PORT);
                Log.d(DEBUGKW, "Waiting for command socket connection...");
                Socket cmd_socket = mCmdSocket.accept();
                Log.d(DEBUGKW, "Command socket accept new connection");
                int result = cmdSocketWorker(cmd_socket);
                Log.d(DEBUGKW, "Command socket worker result: " + result);
                while (mConnStart && mImageStart) {
                    Log.d(DEBUGKW, "Command socket ready to sleep 2 sec...");
                    Thread.sleep(2000);
                }
                if (!mImageStart) {
                    sendStopImageTransmissionCommand(cmd_socket);
                }
            }
            Log.d(DEBUGKW, "Close command socket");
            mCmdSocket.close();
        } catch (IOException e) {
            Log.d(DEBUGKW, "startCmdSocket: " + e.getMessage());
        } catch (InterruptedException e) {
            Log.d(DEBUGKW, e.getMessage());
        }
    }

    int cmdSocketWorker(Socket socket) {
        try {
            InputStream istream = socket.getInputStream();
            OutputStream ostream = socket.getOutputStream();

            byte[] recv_buf = new byte[256];
            int read_byte_count;
            DataInputStream distream = new DataInputStream(istream);
            DataOutputStream dostream = new DataOutputStream(ostream);


            // Receive connection notification
            read_byte_count = distream.read(recv_buf, 0, 6);
            if (read_byte_count > 0) {
                Log.d(DEBUGKW, "Command: " + String.valueOf(recv_buf[0]));
                Log.d(DEBUGKW, "Length: " + String.valueOf(recv_buf[2]));
                Log.d(DEBUGKW, "Protocol: " + String.valueOf(recv_buf[4]));
                Log.d(DEBUGKW, "Camera ID: " + String.valueOf(recv_buf[5]));
                if (String.valueOf(recv_buf[0]).equals("4")) {
                    Log.d(DEBUGKW, "Receive connection notification from DVR");
                }
            } else {
                return -1;
            }

            // Send connection notification reply
            byte[] send_buf = new byte[5];
            send_buf[0] = (byte) 0;
            send_buf[0] = (byte) 0;
            send_buf[0] = (byte) 1;
            send_buf[0] = (byte) 0;
            send_buf[0] = (byte) 0;
            dostream.write(send_buf, 0, 5);
            dostream.flush();
            Log.d(DEBUGKW, "Send connection notification reply");

            // Send image request
            send_buf = new byte[12];
            send_buf[0] = (byte) 1;     // Command 1~8 bit
            send_buf[1] = (byte) 0;     // Command 9~16 bit
            send_buf[2] = (byte) 8;     // Length 1~8 bit
            send_buf[3] = (byte) 0;     // Length 9~16 bit
            send_buf[4] = (byte) 1;     // Control 1~8 bit
            send_buf[5] = (byte) 0;     // Control 9~16 bit
            send_buf[6] = (byte) 0;     // Main resolution 1~8 bit
            send_buf[7] = (byte) 0;     // Main resolution 9~16 bit
            send_buf[8] = (byte) 2;     // Sub resolution 1~8 bit
            send_buf[9] = (byte) 0;     // Sub resolution 9~16 bit
            send_buf[10] = (byte) 0;    // Frame rate 1~8 bit
            send_buf[11] = (byte) 0;    // Frame rate 9~16 bit
            dostream.write(send_buf, 0, 12);
            dostream.flush();
            Log.d(DEBUGKW, "Send image request");

            // Receive image request reply
            read_byte_count = distream.read(recv_buf, 0, 5);
            if (read_byte_count > 0) {
                Log.d(DEBUGKW, "Command: " + String.valueOf(recv_buf[0]));
                Log.d(DEBUGKW, "Length: " + String.valueOf(recv_buf[2]));
                Log.d(DEBUGKW, "Status: " + String.valueOf(recv_buf[4]));
                if (String.valueOf(recv_buf[4]).equals("0")) {
                    Log.d(DEBUGKW, "Receive Horiba Image Request Reply from DVR");
                }
            } else {
                return -1;
            }
        } catch (IOException e) {
            Log.d(DEBUGKW, e.getMessage());
        }
        return 0;
    }

    void sendStopImageTransmissionCommand(Socket socket) {
        try {

            OutputStream ostream = socket.getOutputStream();
            DataOutputStream dostream = new DataOutputStream(ostream);

            // Send image request
            byte[] send_buf = new byte[12];
            send_buf[0] = (byte) 1;     // Command 1~8 bit
            send_buf[1] = (byte) 0;     // Command 9~16 bit
            send_buf[2] = (byte) 8;     // Length 1~8 bit
            send_buf[3] = (byte) 0;     // Length 9~16 bit
            send_buf[4] = (byte) 0;     // Control 1~8 bit
            send_buf[5] = (byte) 0;     // Control 9~16 bit
            send_buf[6] = (byte) 0;     // Main resolution 1~8 bit
            send_buf[7] = (byte) 0;     // Main resolution 9~16 bit
            send_buf[8] = (byte) 2;     // Sub resolution 1~8 bit
            send_buf[9] = (byte) 0;     // Sub resolution 9~16 bit
            send_buf[10] = (byte) 0;    // Frame rate 1~8 bit
            send_buf[11] = (byte) 0;    // Frame rate 9~16 bit
            dostream.write(send_buf, 0, 12);
            dostream.flush();
            Log.d(DEBUGKW, "Send stop image transmission request");
            dostream.close();
            ostream.close();
        } catch (IOException e) {
            Log.d(DEBUGKW, e.getMessage());
        }
    }

    void startImageSocket() {
        try {
            while (mConnStart && mImageStart) {
                Log.d(DEBUGKW, "Waiting for image socket connection...");
                mImgSocket = new ServerSocket(IMAGE_PORT);
                Socket image_socket = mImgSocket.accept();
                Log.d(DEBUGKW, "Image socket accept new connection");
                while (mConnStart && mImageStart) {
                    int result = imgSocketWorker(image_socket);
                    Log.d(DEBUGKW, "Image socket worker result: " + result);
                    Thread.sleep(200);
                }
            }
            Log.d(DEBUGKW, "Close image socket");
            mImgSocket.close();
        } catch (IOException e) {
            Log.d(DEBUGKW, "startImageSocket: " + e.getMessage());
        } catch (InterruptedException e) {
            Log.d(DEBUGKW, e.getMessage());
        }
    }

    int imgSocketWorker(Socket socket) {
        try {
            InputStream istream = socket.getInputStream();
            DataInputStream distream = new DataInputStream(istream);
            while (mConnStart && mImageStart) {
                byte[] buf = new byte[1500];
                int read_byte_count;
                read_byte_count = distream.read(buf);
                if (read_byte_count > 0) {
                    Log.d(DEBUGKW, "Read " + read_byte_count + "bytes");
                }
            }
            distream.close();
            istream.close();
        } catch (IOException e) {
            Log.d(DEBUGKW, e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
