package com.example.mqtt_test;

import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.time.LocalDateTime;
import androidx.appcompat.app.AppCompatActivity;

/*import org.eclipse.paho.android.service.MqttAndroidClient;*/
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
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
    private DataBaseHelper dbHelper;

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

        dbHelper = new DataBaseHelper(this);

        int gas1 = 10;
        int gas2 = 200;
        int gas3 = 300;
        dbHelper.insertData(gas1, gas2, gas3);
        try {
            dbHelper.openDataBase();
        }catch (
                SQLException e) {
            e.printStackTrace();
        }

        String query = "select * from GAS";
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, null);

        StringBuilder result = new StringBuilder();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // 결과 처리
                int id = cursor.getInt(0);
                int example1 = cursor.getInt(1);
                int example2 = cursor.getInt(2);
                int example3 = cursor.getInt(3);
                String now = cursor.getString(4);

                // 결과 출력 또는 처리
                Log.d("Result", "ID: " + id + ", example1: " + example1 + ", example2: "
                        + example2 + ", example3: " + example3+ ", timestamp: " + now);

                // 결과 출력 또는 처리
                String line = "ID: " + id + ", Example1: " + example1 + ", Example2: "
                        + example2 + ", Example3: " + example3 + ", Timestamp: " + now;
                result.append(line).append("\n");
            } while (cursor.moveToNext());

            // 커서 닫기
            cursor.close();
        }

    }


    protected void onDestroy() {
        super.onDestroy();

        // 데이터베이스 연결 종료
        dbHelper.close();
    }
}