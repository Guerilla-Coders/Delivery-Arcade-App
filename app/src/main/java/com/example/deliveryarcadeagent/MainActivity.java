package com.example.deliveryarcadeagent;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends AppCompatActivity {

    public JSONObject readJSON(String fileName) {
        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open(fileName);
            int fileSize = inputStream.available();

            byte[] buffer = new byte[fileSize];
            inputStream.read(buffer);
            inputStream.close();

            String jsonData = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(jsonData);

            return jsonObject;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject networkConfig;

    private Socket socket;

    private final Emitter.Listener receiveInfo = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i("Info", Arrays.toString(args));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();

        networkConfig = readJSON("config/network_config.json");
        Log.i("config", String.valueOf(networkConfig));

        String server_ip = "";
        String server_port = "";
        try {
            server_ip = networkConfig.getJSONObject("server").getString("ip");
            server_port = networkConfig.getJSONObject("server").getJSONObject("port").getString("control");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String socketIOURI = "http://" + server_ip + ":" + server_port + "/app";
        Log.i("config", "Connecting to: " + socketIOURI);
        socket = IO.socket(URI.create(socketIOURI));
        socket.on("info", receiveInfo);
        socket.connect();

        setContentView(R.layout.activity_main);

        WebView webcamView = findViewById(R.id.webcamView);
        String WebcamPageURI = "http://" + server_ip + ":" + server_port + "/video";
        Log.i("config", "Connecting to: " + WebcamPageURI);
        webcamView.loadUrl(WebcamPageURI);
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
                socket.emit("command", "throttle " + progress);
                throttleValueText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                socket.emit("command", "throttle start");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                socket.emit("command", "throttle end");
            }
        });

        steerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                socket.emit("command", "steering " + progress);
                steerValueText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                socket.emit("command", "steering start");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                socket.emit("command", "steering end");
            }
        });

    }

    public void press_up(View view) {
        Log.i("button", "up");
        socket.emit("command", "UP");
    }

    public void press_down(View view) {
        Log.i("button", "down");
        socket.emit("command", "DOWN");
    }

    public void press_left(View view) {
        Log.i("button", "left");
        socket.emit("command", "LEFT");
    }

    public void press_right(View view) {
        Log.i("button", "right");
        socket.emit("command", "RIGHT");
    }

    public void press_fire(View view) {
        Log.i("button", "fire");
        socket.emit("command", "FIRE");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        socket.disconnect();
        socket.off("info", receiveInfo);
    }
}