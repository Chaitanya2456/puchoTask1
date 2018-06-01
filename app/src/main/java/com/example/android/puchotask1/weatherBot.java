package com.example.android.puchotask1;

import android.Manifest;
import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import com.dnkilic.waveform.WaveView;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.login.LoginManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;

public class weatherBot extends FragmentActivity implements AIListener{
    private AIService aiService;
    private TextView userGreeter;
    private SimpleDraweeView listenButton;
    private TextView queryTextView;
    private TextView resultTextView;
    private EditText typeQuery;
    private Button submitButton;
    private static final int RECORD_REQUEST_CODE = 101;
    // naming the file for storing user interactions data
    private static final String STORE_INTERACTIONS = "interactions.txt";
    private  static final String TAG = "weatherBot Activity";
    JSONObject profile_pic_data, profile_pic_url;
    SimpleDraweeView userimage;
    WaveView waveView;
    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_bot);

        LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
        // animation for launching this activity
        AlphaAnimation animation = new AlphaAnimation(0.0f , 1.0f ) ;
        animation.setFillAfter(true);
        animation.setDuration(200);
       //applies the animation ( fade In ) to Layout
        layout.startAnimation(animation);
        Fresco.initialize(this);
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if(permission != PackageManager.PERMISSION_GRANTED){
            Log.d("TAG", "Permission not granted to record audio");
            makeRequest();
        }
        // assigning the necessary variables
        userGreeter = (TextView) findViewById(R.id.userGreeter);
        queryTextView = (TextView)findViewById(R.id.queryTextView);
        resultTextView = (TextView)findViewById(R.id.resultTextView);
        userimage = (SimpleDraweeView)findViewById(R.id.userImage);
        typeQuery = (EditText)findViewById(R.id.typeQuery);
        submitButton = (Button)findViewById(R.id.submitButton);
        waveView = (WaveView)findViewById(R.id.waveView);
        // creating media player for playing speech prompt sounds when the speech recognition occurs
        mp = MediaPlayer.create(this, R.raw.beep);

        // Adding scrollability to textViews in CardViews
        resultTextView.setMovementMethod(new ScrollingMovementMethod());
        queryTextView.setMovementMethod(new ScrollingMovementMethod());


        SharedPreferences preferences = getSharedPreferences("MyPref", 0);
        // storing the name of log file in preferences for use in logData activity
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("txtFileName", STORE_INTERACTIONS);
        editor.commit();

        // getting the Google user information from sharedPreferences and using it in "userGreeter" and for setting the userImage
        if (preferences.contains("googleUserName")) {
            Log.d(TAG, "preferences contain google user details");
            String userName = preferences.getString("googleUserName", null);
            int spaceIndex = userName.indexOf(' ');
            userName = userName.substring(0, spaceIndex);
            userGreeter.setText("Hi " + userName + ", I am your Weatherbot");
            if(preferences.contains("googlePhotoUrl")){
                Log.d(TAG, "google user has a profile photo");
                userimage.setImageURI(Uri.parse(preferences.getString("googlePhotoUrl", null)));
            }
        }

        // getting the Facebook user information from sharedPreferences and using it in "userGreeter" and for setting the userImage
        if (preferences.contains("userProfile")) {
            Log.d(TAG, "preferences contain Facebook user data");
            String jsondata = preferences.getString("userProfile", null);
            try {
                JSONObject response = new JSONObject(jsondata);
                String userName = response.get("name").toString();
                int spaceIndex = userName.indexOf(' ');
                userName = userName.substring(0, spaceIndex);
                userGreeter.setText("Hi " + userName + ", I am your Weatherbot");
                profile_pic_data = new JSONObject(response.get("picture").toString());
                profile_pic_url = new JSONObject(profile_pic_data.getString("data"));
                userimage.setImageURI(Uri.parse(profile_pic_url.getString("url")));
            } catch (JSONException e) {
                Log.e(TAG, "received an exception", e);
            }
        }

        // configuring the ai with client access Token: for using the dialogflow agent
        final AIConfiguration config = new AIConfiguration("2aabeb45d7404ff59c085ecfd79cded8",
                AIConfiguration.SupportedLanguages.English, AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);

        // setting the AI Listener
        aiService.setListener(this);
        final AIDataService aiDataService = new AIDataService(config);
        final AIRequest aiRequest = new AIRequest();
        listenButton = (SimpleDraweeView)findViewById(R.id.listenButton);

        // sending this query typed in editText to the Dialogflow agent for response
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = typeQuery.getText().toString();
                Log.i(TAG, "QUERY: " + message);
                queryTextView.setText(message);
                queryTextView.setVisibility(View.INVISIBLE);
                resultTextView.setText("");
                if(!message.equals("")){
                    // sending the query to agent
                    aiRequest.setQuery(message);
                    // starting an Async task so as to take out load off the main UI thread
                    new AsyncTask<AIRequest, Void, AIResponse>() {
                        @Override
                        protected AIResponse doInBackground(AIRequest[] aiRequests) {
                            final AIRequest request = aiRequests[0];
                            try {
                                final AIResponse response = aiDataService.request(request);
                                return response;
                            }catch (AIServiceException e){
                                Log.e(TAG, "received an exception", e);
                            }
                            return null;
                        }
                        // receiving the response from agent and setting up the textViews
                        @Override
                        protected void onPostExecute(AIResponse response){
                            if(response!=null){
                                Log.d(TAG, "received result from agent");
                                Result result = response.getResult();
                                String reply = result.getFulfillment().getSpeech();
                                reply = reply.replaceAll("\\s+", " ");
                                Log.i(TAG, "BOT: "+ reply);
                                queryTextView.setVisibility(View.VISIBLE);
                                resultTextView.setText(reply);
                                try{
                                    OutputStreamWriter out = new OutputStreamWriter(openFileOutput(STORE_INTERACTIONS, MODE_APPEND));
                                    out.write("USER: " + queryTextView.getText().toString() + "\n");
                                    out.write("BOT: " + resultTextView.getText().toString() + "\n");
                                    out.close();
                                }catch (Throwable t){
                                    //
                                    Log.e(TAG, "received an exception", t);
                                }
                            }
                        }
                    }.execute(aiRequest);

                    // for logging the user interactions and AI responses in a txt file declared above

                }
                typeQuery.setText("");
            }
        });
    }

    // requesting permission to record audio for speech recognition
    private void makeRequest() {
        Log.d(TAG, "requesting user for permission to record audio");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_REQUEST_CODE);
    }

    // starting speech recognition and listenButton animations and also checking whether the permission to record is granted.
    public void startAction(View view) {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if(permission != PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "Permission not granted to record audio");
            makeRequest();
        }else{
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_button);
        listenButton.startAnimation(animation);
        aiService.startListening();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        waveView.setVisibility(View.VISIBLE);
        // starting the wave animation in userCard during the process of speech recognition
        waveView.initialize(dm);
        queryTextView.setText("");
        resultTextView.setText("");
        }
    }


    // getting response from agents and setting up the textViews
    @Override
    public void onResult(AIResponse response) {
        Log.d(TAG, "received result from agent");
        Result result = response.getResult();
        // Show results in TextView.
        waveView.stop();
        queryTextView.setText(result.getResolvedQuery());
        queryTextView.setVisibility(View.VISIBLE);
        String responseFromAgent = result.getFulfillment().getSpeech();
        responseFromAgent = responseFromAgent.replaceAll("\\s+", " ");
        resultTextView.setText(responseFromAgent);
        listenButton.clearAnimation();
        try{
            OutputStreamWriter out = new OutputStreamWriter(openFileOutput(STORE_INTERACTIONS, MODE_APPEND));
            out.write("USER: " + queryTextView.getText().toString() + "\n");
            out.write("BOT: " + resultTextView.getText().toString() + "\n");
            Log.d(TAG, "user interactions and response into a txt file");
            out.close();
        }catch (Throwable t){
            //
            Log.e(TAG, "received an exception", t);
        }
    }

    // informing the user that the AI has not recognised or something went wrong
    @Override
    public void onError(AIError error) {
        queryTextView.setText(error.toString());
        listenButton.clearAnimation();
        waveView.clearAnimation();
        waveView.setVisibility(View.INVISIBLE);
        resultTextView.setVisibility(View.VISIBLE);
        resultTextView.setText("Please check the internet connection");
        try{
            OutputStreamWriter out = new OutputStreamWriter(openFileOutput(STORE_INTERACTIONS, MODE_APPEND));
            out.write("USER: " + queryTextView.getText().toString() + "\n");
            out.write("BOT: " + resultTextView.getText().toString() + "\n");
            Log.d(TAG, "user interactions and response into a txt file");
            out.close();
        }catch (Throwable t){
            //
            Log.e(TAG, "received an exception", t);
        }
    }

    @Override
    public void onAudioLevel(float level) {

    }

    // starting the wave animation and playing prompt sounds when AI starts listening
    @Override
    public void onListeningStarted() {
         waveView.speechStarted();
         mp.start();
         Log.d(TAG, "started waveView animation, mediaPlayer");
    }

    @Override
    public void onListeningCanceled() {

    }

    // stopping the wave animation and stop playing the sounds when AI stops listening
    @Override
    public void onListeningFinished() {
        waveView.speechEnded();
        mp.stop();
        Log.d(TAG, "stopped waveView animation and mediaPlayer");
    }

    // navigating to logData activity to show user the interactions and responses of agent
    public void showLogs(View view) {
         Intent intent = new Intent(this, logData.class);
         startActivity(intent);
    }
}
