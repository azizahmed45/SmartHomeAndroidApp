package seu.smarthome;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    MqttAndroidClient client;
    String dataTopic = "seu2019-data";
    String switchTopic = "seu2019-switch";
    String broker = "tcp://broker.mqttdashboard.com:1883";
    String clientId = "android-client";

    TextView humidity;
    TextView temperature;

    ToggleButton switch1;
    ToggleButton switch2;

    ImageView fireImage;

    ImageView viewImage;

    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        humidity = findViewById(R.id.humidity);
        temperature = findViewById(R.id.temperature);
        switch1 = findViewById(R.id.switch1);
        switch2 = findViewById(R.id.switch2);
        fireImage = findViewById(R.id.fire_image);
        viewImage = findViewById(R.id.smart_home_image);
        fireImage.setVisibility(View.GONE);

        init();

        switch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sendMessage("switch1:on");
            } else {
                sendMessage("switch1:off");
            }
        });

        switch2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sendMessage("switch2:on");
            } else {
                sendMessage("switch2:off");
            }
        });

    }

    public void init() {
        client = new MqttAndroidClient(this.getApplicationContext(), broker, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        options.setCleanSession(true);
        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    subscribe();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("MainActivity", "Failed to connect to: " + broker);
                }

            });
        } catch (MqttException e) {
            e.printStackTrace();
        }


        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d("MainActivity", "Connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                handleMessage(message);
                Log.d("MainActivity", "Message arrived: " + message.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("MainActivity", "Delivery complete");
            }
        });
    }

    public void subscribe() {
        try {
            client.subscribe(dataTopic, 0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void sendMessage(String message) {
        try {
            client.publish(switchTopic, message.getBytes(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void handleMessage(MqttMessage message) {
        //message example humidity: 50.0 or temperature: 25.0 or smoke: 600
        String[] parts = message.toString().split(":");
        String type = parts[0];
        String value = parts[1];
        if (type.equals("humidity")) {
            //do something
            humidity.setText(String.format("%s%%", value));
        } else if (type.equals("temperature")) {
            //do something
            temperature.setText(String.format("%s °C", value));
        } else if (type.equals("smoke")) {
            //play alarm when value > 600
            if (Integer.parseInt(value) > 600) {
                playAlarm();
            } else {
                stopAlarm();
            }
        }

    }

    //play alarm
    public void playAlarm() {
        if(mediaPlayer == null){
            mediaPlayer = new MediaPlayer();
        }

        fireImage.setVisibility(View.VISIBLE);
        viewImage.setVisibility(View.GONE);

        if (!mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.reset();
                AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.fire_alarm);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mediaPlayer.prepare();
                mediaPlayer.setLooping(true); // Set the media player to loop
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //stop alarm
    public void stopAlarm() {
        if (mediaPlayer != null) {
            fireImage.setVisibility(View.GONE);
            viewImage.setVisibility(View.VISIBLE);
            mediaPlayer.stop();
        }
    }
}