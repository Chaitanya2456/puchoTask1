package com.example.android.puchotask1;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class logData extends Activity {
    private static final String TAG = "logData Activity";
    private TextView logTextView;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        setContentView(R.layout.activity_log_data);
        logTextView = (TextView)findViewById(R.id.logTextView);

        // adding scrollability to logTextView
        logTextView.setMovementMethod(new ScrollingMovementMethod());
        try{
            InputStream in = openFileInput(getSharedPreferences("MyPref",0)
                    .getString("txtFileName", null));
            if(in!=null){
                Log.d(TAG, "receiving input stream");
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String data;
                StringBuilder buf = new StringBuilder();
                while ((data = bufferedReader.readLine())!=null){
                    buf.append(data+"\n");
                }
                in.close();
                logTextView.setText(buf.toString());
            }
        }catch (java.io.FileNotFoundException e){
            Log.e(TAG, "received an exception", e);
        }catch (Throwable t){
            Log.e(TAG, "received an exception", t);
        }
    }

    public void clearLogs(View view) {
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(openFileOutput(getSharedPreferences("MyPref",0)
                    .getString("txtFileName",null), 0));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "received exception", e);
        }
        try {
            out.write("");
            logTextView.setText("");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "received exception", e);
        }
    }
}
