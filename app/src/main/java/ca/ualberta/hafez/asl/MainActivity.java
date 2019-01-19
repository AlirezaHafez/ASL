package ca.ualberta.hafez.asl;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private TextView txtView;
    private String[] ttsResult;
    private VideoView videoView;
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        txtView = findViewById(R.id.txtView);
        videoView = findViewById(R.id.videoView);
        setSupportActionBar(toolbar);
    }


    public void getSpeechInput(View view) {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, 10);
        } else {
            Toast.makeText(this, "Your Device Doesn't Support Speech Input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String tmp = (result.get(0)).toLowerCase();
                    index = 0;
                    txtView.setText(tmp);
                    ttsResult = linkMaker(toWords(tmp));
                    safePlayer();
//                    playLinks(ttsResult, index);
                }
                break;
        }
    }

    private void safePlayer() {
        AddressValidator addressValidator = new AddressValidator();
        if (index < ttsResult.length) {
            addressValidator.execute(ttsResult[index]);
        }
    }


    private void playLinks(String[] links, int i) {
        if (i < links.length) {
            Uri video = Uri.parse(links[i]);
            videoView.setVideoURI(video);
//          videoView.setZOrderOnTop(true); //Very important line, add it to Your code
            videoView.setOnPreparedListener(this);
            videoView.setOnCompletionListener(this);

        } else if (videoView.isPlaying()) {
            videoView.stopPlayback();
            videoView.setVisibility(GONE);
            videoView.setVisibility(VISIBLE);
        }
    }

    private String[] toWords(String s) {
        String[] words = s.split("\\s+");
        return words;
    }

    private String[] linkMaker(String[] words) {
        String[] links = words;
        for (int i = 0; i < links.length; i++) {
            if (links[i].equals("i")) {
                links[i] = "https://www.handspeak.com/word/i/i-abc.mp4";
            } else {
                links[i] = "https://www.handspeak.com/word/" + links[i].charAt(0) + "/" + links[i] + ".mp4";
            }
        }
        return links;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        index++;
        mp.stop();
        mp.reset();
//        playLinks(ttsResult, index);
        safePlayer();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }


    private class AddressValidator extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(String... params) {

            try {
                HttpURLConnection.setFollowRedirects(false);
                HttpURLConnection con = (HttpURLConnection) new URL(params[0]).openConnection();
                con.setRequestMethod("HEAD");
                return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            boolean bResponse = result;
            if (bResponse) {
                Log.d("FUCKEDUP", "onPostExecute: "+ttsResult[index]);
                playLinks(ttsResult, index);
            } else {
                index++;
                safePlayer();
            }
        }
    }


}
