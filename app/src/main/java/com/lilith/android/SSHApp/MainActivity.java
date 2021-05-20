package com.lilith.android.SSHApp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    private Button btnExecute;
    private EditText etAddress;
    private EditText etUserName;
    private EditText etPassword;
    private EditText etCommand;


    JSch jsch = new JSch();
    Session session;
    ChannelExec channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnExecute = findViewById(R.id.btnExecute);
        etAddress = findViewById(R.id.etAddress);
        etUserName = findViewById(R.id.etUserName);
        etPassword = findViewById(R.id.etPassword);
        etCommand = findViewById(R.id.etCommand);

        btnExecute.setOnClickListener(v -> execute());
    }

    private void execute(){
        AsyncTask<Integer, Void,String> executeSSHCommandAsync = new AsyncTask<Integer, Void,String>(){
            @Override
            protected String doInBackground(Integer... integers) {
                String message = executeSSHCommand();
                return message;
            }

            @Override
            protected void onPostExecute(String message) {
                runOnUiThread(()->{
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        };

        executeSSHCommandAsync.execute(1);
    }

    private String executeSSHCommand() {
        String message = null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Session session = jsch.getSession(etUserName.getText().toString(), etAddress.getText().toString(), 22);
            session.setPassword(etPassword.getText().toString());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(10000);
            session.connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(etCommand.getText().toString());
            channel.setOutputStream(outputStream);
            channel.connect();

            message = new String(outputStream.toByteArray());
        } catch (Exception e) {
            runOnUiThread(()->{
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            });
        } finally {
            if(channel != null && channel.isConnected()){
                channel.disconnect();
            }
            if(session != null && session.isConnected()){
                session.disconnect();
            }
        }

        return message;
    }
}