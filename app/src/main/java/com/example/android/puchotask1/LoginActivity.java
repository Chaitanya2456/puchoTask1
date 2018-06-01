package com.example.android.puchotask1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class LoginActivity extends FragmentActivity implements
        GoogleApiClient.OnConnectionFailedListener {
    // for facebook login
    CallbackManager callbackManager;

    //apiClient for Google sign in
    private GoogleApiClient mGoogleApiClient;

    // request code for sign in
    private int RC_SIGN_IN = 1001;

    private static final String TAG = "login Activity";

    //onActivity result for Google sign in
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==RC_SIGN_IN){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
            return;
        }
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
// getting data from google user
    private void handleSignInResult(GoogleSignInResult result) {
        if(result.isSuccess()){
            Log.d(TAG, "google user data is received successfully");
            GoogleSignInAccount acct = result.getSignInAccount();
            Intent intent = new Intent(LoginActivity.this, weatherBot.class);

            // storing userData in sharedPreferences to be used later
            SharedPreferences preferences = getSharedPreferences("MyPref",0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("googleUserName",acct.getDisplayName());
            editor.putString("googleUserEmail", acct.getEmail());
            if(acct.getPhotoUrl()!=null){
                editor.putString("googlePhotoUrl", acct.getPhotoUrl().toString());
            }
            editor.putBoolean("IsLogin", true);
            editor.commit();
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initializing fresco for simpleDraweeViews
        Fresco.initialize(this);
        SharedPreferences preferences = getSharedPreferences("MyPref",0);

        // launching the weatherBot activity if the user is already logged in through Facebook
        if(preferences.contains("userProfile")){
            Log.d(TAG, "preferences contain Facebook user data");
            Intent intent = new Intent(LoginActivity.this, weatherBot.class);
            startActivity(intent);
            finish();
         // launching the weatherBot activity if the user is already logged in through Google
        }else if(preferences.getBoolean("IsLogin", false)){
            Log.d(TAG, "preferences contain google user data");
            Intent intent = new Intent(LoginActivity.this, weatherBot.class);
            startActivity(intent);
            finish();
        }
        else{
        setContentView(R.layout.activity_login);
        // animation for launching activity
            LinearLayout layout = (LinearLayout) findViewById(R.id.loginLayout);
            AlphaAnimation animation = new AlphaAnimation(0.0f , 1.0f ) ;
            animation.setFillAfter(true);
            animation.setDuration(1200);
        //apply the animation ( fade In ) to your Layout
            layout.startAnimation(animation);
            Log.i(TAG, "animation is set for launching activity");

        // handling the login function of Facebook
        callbackManager = CallbackManager.Factory.create();
            final LoginButton[] loginButton = {(LoginButton) findViewById(R.id.facebookLogin)};
        loginButton[0].setReadPermissions(Arrays.asList("public_profile", "email"));
        loginButton[0].registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "received Facebook user data successfully");
                LinearLayout rl = (LinearLayout)findViewById(R.id.loginLayout);
                rl.removeAllViews();
                 getData(loginResult);
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {
               Log.e(TAG, "received exception", error);
            }
        });

        // handling the sign in function of Google
            SignInButton signInButton = (SignInButton)findViewById(R.id.googleSignIn);
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();

            signInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                    Log.d(TAG, "launched intent for google sign in");
                }
            });
        }


    }
    //getting data from facebook user
    private void getData(LoginResult loginResult) {
        GraphRequest dataRequest = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {

                Intent intent = new Intent(LoginActivity.this, weatherBot.class);

                // storing Facebook userData in sharedPreferences to be used later
                SharedPreferences preferences = getSharedPreferences("MyPref",0);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("userProfile", object.toString());
                editor.commit();
                Log.d(TAG, "storing Facebook user data in preferences");
                startActivity(intent);
                finish();

            }
        });

        // creating permission fields for Facebook
        Bundle permissions_param = new Bundle();
        permissions_param.putString("fields", "id,name,email,picture.width(120).height(120)");
        dataRequest.setParameters(permissions_param);
        dataRequest.executeAsync();
    }

    private void printHashKey() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.example.android.puchotask1", PackageManager.GET_SIGNATURES);
            for(Signature signature:info.signatures){
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(),Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "received an exception, e");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "received an exception", e);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "connection failed");
    }
}
