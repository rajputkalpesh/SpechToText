package com.task.speectotext;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {

    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    LinearLayout linear,linearCamera,linearSpeech;
    Button buttonRecording,buttonPlayback;
    VideoView videoviewPlay;

    final static int REQUEST_VIDEO_CAPTURED = 1;
    Uri uriVideo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        linear =  findViewById(R.id.linear);
        linearSpeech =  findViewById(R.id.linearSpeech);
        linearCamera =  findViewById(R.id.linearCamera);

         buttonRecording =findViewById(R.id.recording);
         buttonPlayback = findViewById(R.id.playback);
         videoviewPlay =findViewById(R.id.videoview);

         CameraEvent();
         SpeechEvent();

    }

    private void CameraEvent() {

        buttonRecording.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
                startActivityForResult(intent, REQUEST_VIDEO_CAPTURED);
            }});

        buttonPlayback.setOnClickListener(new Button.OnClickListener(){

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                if(uriVideo == null){
                    Toast.makeText(MainActivity2.this,
                            "No Video",
                            Toast.LENGTH_LONG)
                            .show();
                }else{
                    Toast.makeText(MainActivity2.this,
                            "Playback: " + uriVideo.getPath(),
                            Toast.LENGTH_LONG)
                            .show();
                    videoviewPlay.setVideoURI(uriVideo);
                    videoviewPlay.start();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        try {
                            transcribeModelSelection(uriVideo.getPath());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    //startListeningWithoutDialog();

                }
            }});

    }


    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void transcribeModelSelection(String fileName) throws Exception {
        Path path = null;
        byte[] content = new byte[0];



        try (SpeechClient speech = SpeechClient.create()) {
            // Configure request with video media type

            path = Paths.get(fileName);
            content = Files.readAllBytes(path);

            RecognitionConfig recConfig =
                    RecognitionConfig.newBuilder()
                            // encoding may either be omitted or must match the value in the file header
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setLanguageCode("en-US")
                            // sample rate hertz may be either be omitted or must match the value in the file
                            // header
                            .setSampleRateHertz(16000)
                            .build();
                            /*.setModel("video")*/
            RecognitionAudio recognitionAudio =
                    RecognitionAudio.newBuilder().setContent(ByteString.copyFrom(content)).build();

            RecognizeResponse recognizeResponse = speech.recognize(recConfig, recognitionAudio);
            // Just print the first result here.
            SpeechRecognitionResult result = recognizeResponse.getResultsList().get(0);
            // There can be several alternative transcripts for a given chunk of speech. Just use the
            // first (most likely) one here.
            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
            System.out.printf("Transcript : %s\n", alternative.getTranscript());
            Toast.makeText(MainActivity2.this,alternative.getTranscript()+"",Toast.LENGTH_LONG).show();
        }
    }

    private void SpeechEvent() {

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startListeningWithoutDialog();
            }
        });
    }

    private void startListeningWithoutDialog() {

        // Intent to listen to user vocal input and return the result to the same activity.
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // Use a language model based on free-form speech recognition.
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                getApplicationContext().getPackageName());

        // Add custom listeners.
        CustomRecognitionListener listener = new CustomRecognitionListener();
        SpeechRecognizer sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        sr.setRecognitionListener(listener);
        sr.startListening(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
// TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_CAPTURED) {
                uriVideo = data.getData();
                Toast.makeText(MainActivity2.this,
                        uriVideo.getPath(),
                        Toast.LENGTH_LONG)
                        .show();
            }
        } else if (resultCode == RESULT_CANCELED) {
            uriVideo = null;
            Toast.makeText(MainActivity2.this,
                    "Cancelled!",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }



    private class CustomRecognitionListener implements RecognitionListener {
        private static final String TAG = "RecognitionListener";

        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
        }

        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        public void onRmsChanged(float rmsdB) {
            Log.d(TAG, "onRmsChanged");
        }

        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
        }

        public void onEndOfSpeech() {
            Log.d(TAG, "onEndofSpeech");
        }

        public void onError(int error) {
            Log.e(TAG, "error " + error);

            //conversionCallaback.onErrorOccured(TranslatorUtil.getErrorText(error));
        }

        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
           // ArrayList<String> matches = results.getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
            String text = "";
            for (String result : matches)
                text += result + "\n";

            if(matches.contains("red") || matches.contains("RED")){
                linear.setBackgroundColor(getResources().getColor(R.color.red));
                txtSpeechInput.setText("RED");
            }else if(matches.contains("blue") || matches.contains("BLUE")){
                linear.setBackgroundColor(getResources().getColor(R.color.blue));
                txtSpeechInput.setText("BLUE");
            }else if(matches.contains("yellow") || matches.contains("YELLOW")){
                linear.setBackgroundColor(getResources().getColor(R.color.yellow));
                txtSpeechInput.setText("YELLOW");
            }else {
                linear.setBackgroundColor(getResources().getColor(R.color.white));
                Toast.makeText(MainActivity2.this,"Color not found",Toast.LENGTH_LONG).show();
            }
        }



        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults");
        }

        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
        }
    }




}
