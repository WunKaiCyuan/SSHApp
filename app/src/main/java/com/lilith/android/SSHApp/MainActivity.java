package com.lilith.android.SSHApp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    private Button btnExecute;
    private Button btnConnect;
    private EditText etAddress;
    private EditText etUserName;
    private EditText etPassword;
    private EditText etCommand;


    JSch jsch = new JSch();
    Session session;
    boolean isConnection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btnConnect);
        btnExecute = findViewById(R.id.btnExecute);
        etAddress = findViewById(R.id.etAddress);
        etUserName = findViewById(R.id.etUserName);
        etPassword = findViewById(R.id.etPassword);
        etCommand = findViewById(R.id.etCommand);

        btnConnect.setOnClickListener(v -> {
            if (isConnection)
                disconnect();
            else{
                connect();

                // 記憶連線資訊
                SharedPreferences sp = getSharedPreferences("ssh_connection", MODE_PRIVATE);
                sp.edit()
                        .putString("address", etAddress.getText().toString())
                        .putString("username", etUserName.getText().toString())
                        .apply();
            }
        });
        btnExecute.setOnClickListener(v -> {
            if (isConnection)
                execute();
            else
                Toast.makeText(this, "尚未連線", Toast.LENGTH_SHORT).show();
        });

        // 取得連線記憶資訊
        SharedPreferences sp = getSharedPreferences("ssh_connection", MODE_PRIVATE);
        etAddress.setText(sp.getString("address", ""));
        etUserName.setText(sp.getString("username", ""));
    }

    private void execute() {
        AsyncTask<Void, Void, String> executeCommandAsync = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... integers) {
                String message = executeCommand();
                return message;
            }

            @Override
            protected void onPostExecute(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        };

        executeCommandAsync.execute();
    }

    private void connect() {
        AsyncTask<Void, Void, String> connectAsync = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... integers) {
                String message = executeSSHConnect();
                if (session.isConnected()) {
                    isConnection = true;
                    btnConnect.setText("中斷連線");
                }
                return message;
            }

            @Override
            protected void onPostExecute(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        };

        connectAsync.execute();
    }

    private void disconnect() {
        if (session.isConnected())
            session.disconnect();

        isConnection = false;
        btnConnect.setText("開始連線");
    }

    private String executeSSHConnect() {
        String message = null;
        try {
            session = jsch.getSession(etUserName.getText().toString(), etAddress.getText().toString(), 22);
            session.setPassword(etPassword.getText().toString());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(10000);
            session.connect();

            message = "連線成功";
        } catch (Exception e) {
            message = "連線失敗" + e.getLocalizedMessage();
            Log.d(TAG, message);
        }
        return message;
    }

    private String executeCommand() {
        String message = null;
        ChannelExec channel = null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(etCommand.getText().toString());
            channel.setOutputStream(outputStream);
            channel.connect();

            while (channel.getExitStatus() == -1) {
                try {
                    Thread.sleep(300);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }

            message = new String(outputStream.toByteArray());
        } catch (Exception e) {
            message = e.getLocalizedMessage();
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }

        return message;
    }
}