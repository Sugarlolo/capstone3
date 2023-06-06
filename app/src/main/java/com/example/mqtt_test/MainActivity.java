package com.example.mqtt_test;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

/*import org.eclipse.paho.android.service.MqttAndroidClient;*/
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import info.mqtt.android.service.MqttAndroidClient;


public class MainActivity extends AppCompatActivity {
    MqttAndroidClient mqttAndroidClient;

    final String serverUri = "tcp://broker.mqtt-dashboard.com:1883";
    final String clientId = "android_client";

    final String subscriptionTopic = "myeongseung";
    final String username = "";
    final String password = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Handler handler = new Handler();

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                if (b) {
                    Log.w("Debug", "Connection lost");
                } else {
                    Log.w("Debug", "Connected to: " + serverUri);
                    mqttAndroidClient.subscribe(subscriptionTopic, 0);
                }
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.d("Debug","Connection lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.d("Debug", mqttMessage.toString());

                // Here you can convert mqttMessage to JSON Object
                JSONObject jsonObject = new JSONObject(new String(mqttMessage.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.d("Debug","Delivery Completed...");
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());

        handler.post(new Runnable() {
            @Override
            public void run() {
                mqttAndroidClient.connect(mqttConnectOptions);
            }
        });
    }
}