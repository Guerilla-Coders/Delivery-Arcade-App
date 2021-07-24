package com.example.deliveryarcadeagent;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends AppCompatActivity {


    Socket mSocket = IO.socket(URI.create("http://192.168.0.3:5010/app"));

    private final Emitter.Listener receiveInfo = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i("Info", Arrays.toString(args));
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSocket.on("info", receiveInfo);
        mSocket.connect();
        setContentView(R.layout.activity_main);

        WebView webcamView = findViewById(R.id.webcamView);
        webcamView.loadUrl("http://192.168.0.3:5010/video");
        webcamView.setWebViewClient(new WebViewClient());
        WebSettings webcamViewSettings = webcamView.getSettings();
        webcamViewSettings.setJavaScriptEnabled(true);
        webcamViewSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webcamViewSettings.setLoadWithOverviewMode(true);
        webcamViewSettings.setUseWideViewPort(true);
        webcamViewSettings.setSupportZoom(true);


        SeekBar throttleSeekBar = (SeekBar) findViewById(R.id.throttleBar);
        SeekBar steerSeekBar = (SeekBar) findViewById(R.id.steerBar);

        TextView throttleValueText = (TextView) findViewById(R.id.throttleBarValueDisplay);
        TextView steerValueText = (TextView) findViewById(R.id.steerBarValueDisplay);

        throttleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSocket.emit("command", "throttle " + progress);
                throttleValueText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mSocket.emit("command", "throttle start");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSocket.emit("command", "throttle end");
            }
        });

        steerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSocket.emit("command", "steering " + progress);
                steerValueText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mSocket.emit("command", "steering start");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSocket.emit("command", "steering end");
            }
        });
    }

    public void press_up(View view) {
        Log.i("button", "up");
        mSocket.emit("command", "UP");
    }

    public void press_down(View view) {
        Log.i("button", "down");
        mSocket.emit("command", "DOWN");
    }

    public void press_left(View view) {
        Log.i("button", "left");
        mSocket.emit("command", "LEFT");
    }

    public void press_right(View view) {
        Log.i("button", "right");
        mSocket.emit("command", "RIGHT");
    }

    public void press_fire(View view) {
        Log.i("button", "fire");
        mSocket.emit("command", "FIRE");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off("info", receiveInfo);
    }
}