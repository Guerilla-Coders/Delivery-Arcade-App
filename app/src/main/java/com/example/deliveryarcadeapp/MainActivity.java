package com.example.deliveryarcadeapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


class Commands {
    JSONObject movement(int angle, int strength) throws JSONException {
        String rawMovementJSON = "{\"type\":\"command\", \"joystick\": {\"angle\":" + angle + ", \"strength\":" + strength + "}}\n";
        return new JSONObject(rawMovementJSON);
    }

    JSONObject soundEffect(String code) throws JSONException {
        String rawSoundEffectJSON = "{\"type\":\"command\", \"sound_effect\": {\"code\":" + code + "}}\n";
        return new JSONObject(rawSoundEffectJSON);
    }

    JSONObject lidAction(String action) throws JSONException {
        String rawLidActionJSON = "{\"type\":\"command\", \"lid_action\": {\"action\":" + action + "}}\n";
        return new JSONObject(rawLidActionJSON);
    }
}


public class MainActivity extends AppCompatActivity {


    private static class JsonTask extends AsyncTask<String, String, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                Log.i("config", "Connecting to " + params[0]);
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "da-app");
                connection.connect();

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                    Log.i("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }
                String jsonData = buffer.toString();

                return new JSONObject(jsonData);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private JSONObject networkConfig;
    private Socket socket;

    int joystickInterval = 10;
    int lastStrength;
    int lastAngle;

    Commands commands = new Commands();


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

        // networkConfig = readJSON("config/network_config.json");
        try {
            networkConfig = new JsonTask().execute("http://49.50.175.88:6000/config/server").get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

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
        Log.i("config", "Control server URI is " + socketIOURI);
        Toast socketIOURIToast = Toast.makeText(this.getApplicationContext(), "Connecting to: " + socketIOURI, Toast.LENGTH_LONG);
        socketIOURIToast.show();
        socket = IO.socket(URI.create(socketIOURI));
        socket.on("info", receiveInfo);
        socket.connect();

        setContentView(R.layout.activity_main);

        WebView webcamView = findViewById(R.id.webcamView);
        String WebcamPageURI = "http://" + server_ip + ":" + server_port + "/video";
        Log.i("config", "Webcam page URI is " + WebcamPageURI);
        webcamView.loadUrl(WebcamPageURI);
        webcamView.setWebViewClient(new WebViewClient());
        WebSettings webcamViewSettings = webcamView.getSettings();
        webcamViewSettings.setJavaScriptEnabled(true);
        webcamViewSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webcamViewSettings.setLoadWithOverviewMode(true);
        webcamViewSettings.setUseWideViewPort(true);
        webcamViewSettings.setSupportZoom(true);

        JoystickView joystick_left = findViewById(R.id.joystickView_left);
        JoystickView joystick_right = findViewById(R.id.joystickView_right);

        joystick_left.setOnMoveListener((angle, strength) -> {
            try {
                if (lastStrength != strength || lastAngle != angle) {
                    lastStrength = strength;
                    lastAngle = angle;
                    JSONObject joystickJSON = commands.movement(angle, strength);
                    Log.d("command", "Sending " + joystickJSON);
                    socket.emit("command", joystickJSON);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, 1000 / joystickInterval);

        joystick_right.setOnMoveListener((angle, strength) -> Log.d("joystick", "Right A " + angle + " S " + strength), 1000 / joystickInterval);
    }

    public void press_sound_greeting(View view) {
        try {
            JSONObject soundEffectJSON = commands.soundEffect("GREETING");
            Log.i("button", "button onClick - Sound \"GREETING\"");
            socket.emit("command", soundEffectJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void press_sound_apology(View view) {
        try {
            JSONObject soundEffectJSON = commands.soundEffect("APOLOGY");
            Log.i("button", "button onClick - Sound \"APOLOGY\"");
            socket.emit("command", soundEffectJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void press_sound_appreciation(View view) {
        try {
            JSONObject soundEffectJSON = commands.soundEffect("APPRECIATION");
            Log.i("button", "button onClick - Sound \"APPRECIATION\"");
            socket.emit("command", soundEffectJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void press_sound_yield(View view) {
        try {
            JSONObject soundEffectJSON = commands.soundEffect("YIELD");
            Log.i("button", "button onClick - Sound \"YIELD\"");
            socket.emit("command", soundEffectJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void press_sound_alert(View view) {
        try {
            JSONObject soundEffectJSON = commands.soundEffect("ALERT");
            Log.i("button", "button onClick - Sound \"ALERT\"");
            socket.emit("command", soundEffectJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void press_sound_pigeon(View view) {
        try {
            JSONObject soundEffectJSON = commands.soundEffect("PIGEON");
            Log.i("button", "button onClick - Sound \"PIGEON\"");
            socket.emit("command", soundEffectJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void press_open(View view) {
        try {
            JSONObject lidActionJSON = commands.lidAction("open");
            Log.i("button", "button onClick - Lid Open");
            socket.emit("command", lidActionJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void press_close(View view) {
        try {
            JSONObject lidActionJSON = commands.lidAction("close");
            Log.i("button", "button onClick - Lid Close");
            socket.emit("command", lidActionJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        socket.disconnect();
        socket.off("info", receiveInfo);
    }
}