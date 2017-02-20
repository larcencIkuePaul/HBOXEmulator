package com.sanjetco.hboxemulator;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

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

    Switch mSwitchImageTransmission;
    TextView mTextImageTransmissionStatus, mTextSeekBarValue, mTextTitleSeekBar;
    TextView mTextCmdSockStatus, mTextImgSockStatus;
    SeekBar mSeekBarDelayInterval;
    ServerSocket mCmdSocket, mImgSocket = null;
    boolean mConnStart, mImageStart = false;
    boolean mIsCmdSocketAccepted, mIsImgSocketAccepted = false;
    int mDelayIntervalMsec, mDelayIntervalNsec = 0;

    Handler mImgTransSwitchHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (!mIsCmdSocketAccepted && !mIsImgSocketAccepted) {
                mSwitchImageTransmission.setClickable(true);
            } else {
                mSwitchImageTransmission.setClickable(false);
                Log.d(DEBUGKW, "Current sleep interval: " + mDelayIntervalMsec + " msec " + mDelayIntervalNsec + " nsec");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(DEBUGKW, "App START");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        initUiElements();
    }

    void initUiElements() {
        Log.d(DEBUGKW, "UI init. START");
        //mToggleBtnConnStartStop = (ToggleButton) findViewById(R.id.toggle_btn_conn_start_stop);
        mSwitchImageTransmission = (Switch) findViewById(R.id.switch_send_image_start_stop);
        mTextImageTransmissionStatus = (TextView) findViewById(R.id.text_image_transmission_status);
        mTextSeekBarValue = (TextView) findViewById(R.id.text_seekbar_value);
        mSeekBarDelayInterval = (SeekBar) findViewById(R.id.seekbar_delay_interval);
        mTextTitleSeekBar = (TextView) findViewById(R.id.title_delay_interval);
        mTextTitleSeekBar.setTextColor(Color.BLACK);
        mTextCmdSockStatus = (TextView) findViewById(R.id.text_cmd_socket_status);
        mTextImgSockStatus = (TextView) findViewById(R.id.text_img_socket_status);
        //initClickListener();
        initSwitchListener();
        initSeekBarListener();
    }
/*
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
                            mSwitchImageTransmission.setChecked(true);
                            mTextImageTransmissionStatus.setText(R.string.title_image_transmission_status_start);
                        }
                    });
                } else {
                    mImageStart = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwitchImageTransmission.setChecked(false);
                            mTextImageTransmissionStatus.setText(R.string.title_image_transmission_status_stop);
                        }
                    });
                }
            }
        });
    }
 */
    void initSwitchListener() {
        mSwitchImageTransmission.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d(DEBUGKW, "Button on");
                    mConnStart = true;
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
                    Log.d(DEBUGKW, "Button off");
                    mImgTransSwitchHandler.sendEmptyMessage(0);
                    mConnStart = false;
                    mImageStart = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextImageTransmissionStatus.setText(R.string.title_image_transmission_status_stop);
                        }
                    });
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (!mIsCmdSocketAccepted) {
                                    Log.d(DEBUGKW, "Close command socket");
                                    mCmdSocket.close();
                                }
                                if (!mIsImgSocketAccepted) {
                                    Log.d(DEBUGKW, "Close image socket");
                                    mImgSocket.close();
                                }
                            } catch (IOException e) {
                                Log.d(DEBUGKW, e.getMessage());
                            }
                        }
                    }).start();
                }
            }
        });
    }

    void initSeekBarListener() {
        Log.d(DEBUGKW, "Seek bar listener init. START");
        mSeekBarDelayInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(DEBUGKW, "Server Delay Level: " + progress);
                String delay_status;
                switch (progress) {
                    case 0:
                        mDelayIntervalMsec = 0;
                        mDelayIntervalNsec = 0;
                        delay_status = "Lv. 0 / No Delay.";
                        break;
                    case 1:
                        mDelayIntervalMsec = 15;
                        mDelayIntervalNsec = 0;
                        delay_status = "Lv. 1 / Server is moderately busy.";
                        break;
                    case 2:
                        // 1.6~2.1 sec
                        mDelayIntervalMsec = 21;
                        //mDelayIntervalMsec = 200; // 14 sec
                        mDelayIntervalNsec = 500000;
                        delay_status = "Lv. 2 / Server is very busy.";
                        break;
                    default:
                        mDelayIntervalMsec = 0;
                        mDelayIntervalNsec = 0;
                        delay_status = "Lv. 0 / No Delay.";
                        break;
                }
                mTextSeekBarValue.setText(delay_status);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

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
                mIsCmdSocketAccepted = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextCmdSockStatus.setText(R.string.text_cmd_socket_status_connected);
                    }
                });
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
            mIsCmdSocketAccepted = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextCmdSockStatus.setText(R.string.text_cmd_socket_status_closed);
                }
            });
            mImgTransSwitchHandler.sendEmptyMessage(0);
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
            send_buf[6] = (byte) 2;     // Main resolution 1~8 bit
            send_buf[7] = (byte) 0;     // Main resolution 9~16 bit
            send_buf[8] = (byte) 2;     // Sub resolution 1~8 bit
            send_buf[9] = (byte) 0;     // Sub resolution 9~16 bit
            send_buf[10] = (byte) 2;    // Frame rate 1~8 bit
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
                mImgSocket = new ServerSocket(IMAGE_PORT);
                Log.d(DEBUGKW, "Image socket receive buffer size: " + mImgSocket.getReceiveBufferSize());
                //mImgSocket.setReceiveBufferSize(8192);
                //mImgSocket.setReceiveBufferSize(10240);
                //mImgSocket.setReceiveBufferSize(11264);
                mImgSocket.setReceiveBufferSize(40960);
                Log.d(DEBUGKW, "After config receive buffer size: " + mImgSocket.getReceiveBufferSize());
                Log.d(DEBUGKW, "Waiting for image socket connection...");
                Socket image_socket = mImgSocket.accept();
                Log.d(DEBUGKW, "Image socket accept new connection");
                mIsImgSocketAccepted = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextImgSockStatus.setText(R.string.text_img_socket_status_connected);
                    }
                });
                while (mConnStart && mImageStart) {
                    int result = imgSocketWorker(image_socket);
                    Log.d(DEBUGKW, "Image socket worker result: " + result);
                    Thread.sleep(200);
                }
            }
            Log.d(DEBUGKW, "Close image socket");
            mImgSocket.close();
            mIsImgSocketAccepted = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextImgSockStatus.setText(R.string.text_img_socket_status_closed);
                }
            });
            mImgTransSwitchHandler.sendEmptyMessage(0);
        } catch (IOException e) {
            Log.d(DEBUGKW, "startImageSocket: " + e.getMessage());
        } catch (InterruptedException e) {
            Log.d(DEBUGKW, e.getMessage());
        }
    }

    int imgSocketWorker(Socket socket) {
        try {
            /*
            Log.d(DEBUGKW, "Original socket timeout: " + socket.getSoTimeout());
            socket.setSoTimeout(5000);
            Log.d(DEBUGKW, "After config socket timeout: " + socket.getSoTimeout());
            */

            InputStream istream = socket.getInputStream();
            DataInputStream distream = new DataInputStream(istream);
            while (mConnStart && mImageStart) {

                int read_byte_count;
                byte[] buf = new byte[1412];
                read_byte_count = distream.read(buf);
                if (read_byte_count > 0) {
                    Log.d(DEBUGKW, "Read " + read_byte_count + " bytes");
                }
                if (mDelayIntervalMsec > 0 || mDelayIntervalNsec > 0) {
                    try {
                        //Log.d(DEBUGKW, "Sleep " + mDelayIntervalMsec + " msec " + mDelayIntervalNsec + " nsec");
                        Thread.sleep(mDelayIntervalMsec, mDelayIntervalNsec);
                    } catch (InterruptedException e) {
                        Log.d(DEBUGKW, e.getMessage());
                    }
                }

                /* 0xff version
                int read_byte_count;
                byte[] buf = new byte[1412];
                read_byte_count = distream.read(buf);
                if (read_byte_count > 0) {
                    if (read_byte_count == 16)
                        Log.d(DEBUGKW, "buf[8]" + (buf[8] & 0xff) + "buf[9]" + (buf[9] & 0xff) + "buf[10]" + (buf[10] & 0xff) + "buf[11]" + (buf[11] & 0xff));
                    else if (read_byte_count == 1412)
                        Log.d(DEBUGKW, "buf[6]" + (buf[6] & 0xff) + "buf[7]" + (buf[7] & 0xff) + "buf[8]" + (buf[8] & 0xff) + "buf[9]" + (buf[9] & 0xff) + "buf[10]" + (buf[10] & 0xff) + "buf[11]" + (buf[11] & 0xff));
                    Log.d(DEBUGKW, "Read " + read_byte_count + " bytes");
                }
                */

                /*
                int read_byte_count;
                byte[] buf = new byte[16];

                // Receive data start packet
                read_byte_count = distream.read(buf, 0, 16);
                if (read_byte_count > 0) {
                    Log.d(DEBUGKW, "Read " + read_byte_count + " bytes");
                    int main_size = buf[8] + buf[9] * 256 + buf[10] * 65536 + buf[11] * 16777216;
                    Log.d(DEBUGKW, "main_size: " + main_size);
                    do {
                        int prepare_size;
                        if (main_size >= 1400) {
                            prepare_size = 1412;
                            buf = new byte[prepare_size];
                        } else {
                            prepare_size = 12 + main_size;
                            buf = new byte[prepare_size];
                        }
                        read_byte_count = distream.read(buf, 0, prepare_size);
                        if (read_byte_count > 12) {
                            int photo_size = buf[10] + buf[11] * 256;
                            main_size = main_size - photo_size;
                            Log.d(DEBUGKW, "photo_size: " + photo_size);
                        }
                    } while (main_size >= 0);
                }
                */
                /*
                try {
                    Thread.sleep(mDelayIntervalMsec);
                } catch (InterruptedException e) {
                    Log.d(DEBUGKW, e.getMessage());
                }
                */
                /*
                int recv_data_size = 0;
                int read_byte_count;
                do {
                    byte[] buf = new byte[1500];
                    read_byte_count = distream.read(buf);
                    if (read_byte_count > 0) {
                        Log.d(DEBUGKW, "Read " + read_byte_count + "bytes");
                        recv_data_size = recv_data_size + read_byte_count;
                    }
                } while (read_byte_count == 1500);
                Log.d(DEBUGKW, "Total data size: " + recv_data_size);
                try {
                    Thread.sleep(mDelayIntervalMsec);
                } catch (InterruptedException e) {
                    Log.d(DEBUGKW, e.getMessage());
                }
                */
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
