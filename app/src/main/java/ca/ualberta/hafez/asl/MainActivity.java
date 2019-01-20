package ca.ualberta.hafez.asl;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.hanks.htextview.fall.FallTextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private TextView txtView;
    private String[] ttsResult;
    private String[] ttsWords;
    private String[] ttsFingerSpell;
    private VideoView videoView;
    private String sentence;
    private String word;
    private int index = 0;
    private int findex = 0;
    private boolean isF = false;
    ArrayList<String> dic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        txtView = findViewById(R.id.txtView);
        videoView = findViewById(R.id.videoView);
        dic = new ArrayList<String>();
        readDic();
//        setSupportActionBar(toolbar);
    }

    private String MakeItBold(String st) {
        String tmp = sentence;
        tmp = tmp.replaceAll(st, "<b>" + st + "</b>");
        return tmp;
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
                    if (videoView.isPlaying()) {
                        videoView.stopPlayback();
                    }
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    sentence = (result.get(0)).toLowerCase();
                    index = 0;
                    txtView.setText(sentence);
                    ttsWords = toWords(sentence);
                    ttsResult = linkMaker(toWords(sentence));
                    safePlayer(ttsResult, index);
                }
                break;
        }
    }

    private void safePlayer(String[] links, int index) {
        AddressValidator addressValidator = new AddressValidator();
        if (index < links.length) {
            addressValidator.execute(links[index]);
        }
    }


    private void playLinks(String[] links, int i) {
        if (i < links.length) {
            if (!isF) {
                txtView.setText(Html.fromHtml(MakeItBold(ttsWords[i])));
                FallTextView evaporateTextView = findViewById(R.id.logoview);
                evaporateTextView.animateText(ttsWords[i]);
                evaporateTextView.animate();
            } else {
                FallTextView evaporateTextView = findViewById(R.id.logoview);
                evaporateTextView.animateText(String.valueOf(word.charAt(i)).toUpperCase());
                evaporateTextView.animate();
            }
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
        String tmp = s.replaceAll("n\'t", " not");
        tmp = tmp.replaceAll("\'m", " am");
        tmp = tmp.replaceAll("\'", "");
        tmp = tmp.replaceAll("&", "and");
        String[] words = tmp.split("\\s+");
        return words;
    }

    private String[] linkMaker(String[] words) {
        String[] links = words;
        for (int i = 0; i < links.length; i++) {
            if (dic.contains(links[i])) {
                links[i] = "https://www.handspeak.com/word/" + links[i].charAt(0) + "/" + links[i] + ".mp4";
            } else {
                links[i] = Plurals.singularize(links[i]);
                if (dic.contains(links[i])) {
                    links[i] = "https://www.handspeak.com/word/" + links[i].charAt(0) + "/" + links[i] + ".mp4";
                } else {
                    for (int j = 0; j < dic.size(); j++) {
                        String tmp = dic.get(j);
                        tmp = tmp.replaceAll("-", "");
                        Log.d("FUCKED", String.valueOf(dic.size()));
                        if ((tmp.contains(links[i]) && links[i].length() > 3) || (links[i].contains(tmp) && tmp.length() > 3)) {
                            links[i] = "https://www.handspeak.com/word/" + dic.get(j).charAt(0) + "/" + dic.get(j) + ".mp4";
                            Log.d("FUCK", "linkMaker: " + links[i]);
                            break;
                        }
                    }
                }
            }
        }
        return links;
    }

    private String[] FingerSpellLinkMaker(String word) {
        String[] links = new String[word.length()];
        for (int i = 0; i < links.length; i++) {
            links[i] = "https://www.handspeak.com/word/" + word.charAt(i) + "/" + word.charAt(i) + "-abc.mp4";
        }
        return links;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.stop();
        mp.reset();
        if (isF) {
            if (ttsFingerSpell.length - 1 <= findex) {
                isF = false;
                safePlayer(ttsResult, index);
            } else {
                findex++;
                playLinks(ttsFingerSpell, findex);
            }
        } else {
            index++;
            safePlayer(ttsResult, index);
        }
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
                if (isF) {
                    if (ttsFingerSpell.length <= findex) {
                        isF = false;
                        playLinks(ttsResult, index);
                    } else {
                        playLinks(ttsFingerSpell, findex);
                    }
                } else {
                    playLinks(ttsResult, index);
                }
            } else {
                isF = true;
                findex = 0;
                word = ttsWords[index];
                ttsFingerSpell = FingerSpellLinkMaker(word);
                playLinks(ttsFingerSpell, findex);
                index++;
            }
        }
    }


    private void readDic() {
        try {
            InputStreamReader is = new InputStreamReader(getAssets()
                    .open("dic.csv"));

            BufferedReader reader = new BufferedReader(is);
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                dic.add(line);
            }
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }
    }


}
