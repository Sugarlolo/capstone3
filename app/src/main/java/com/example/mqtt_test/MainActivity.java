package com.example.mqtt_test;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

/*import org.eclipse.paho.android.service.MqttAndroidClient;*/
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import info.mqtt.android.service.MqttAndroidClient;


public class MainActivity extends AppCompatActivity {
    MqttAndroidClient mqttAndroidClient;
    static int LPG_value;
    static int Metaine_value;
    static int CO_value;

    final String serverUri = "tcp://broker.mqtt-dashboard.com:1883";
    final String clientId = "android_client";

    final String subscriptionTopic = "myeongseung";
    final String username = "";
    final String password = "";
    private DataBaseHelper dbHelper;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList;

    private TextView present_textView;

    private String gasSensor="";
    private int value=0;


    private TextView Sensor_Num;
    private TextView Sensor_Name;
    private TextView Sensor_State;
    private ProgressBar ps_bar;
    private ImageButton arrowbutton1, arrowbutton2;
    private String[] textArray = {"sensor 1", "sensor 2", "sensor 3"};
    private String[] textArray2 = {"LPG", "METAINE", "CO"};
    private int currentIndex = 0;



    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Handler handler = new Handler();
        dbHelper = new DataBaseHelper(this);


        //dbHelper.resetDatabase();
        //데이터베이스 리셋필요할때

        // ui_리스트뷰 출력
        listView = findViewById(R.id.listveiw);
        ProgressBar ps_bar = findViewById(R.id.ps_bar);
        dataList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        // ui _ arrow 버튼
        Sensor_Num = findViewById(R.id.Sensor_Num);
        Sensor_Name = findViewById(R.id.Sensor_Name);
        Sensor_State = findViewById(R.id.Sensor_State);

        present_textView = findViewById(R.id.present_textView);

        arrowbutton1 = findViewById(R.id.arrowbutton1);
        arrowbutton2 = findViewById(R.id.arrowbutton2);

        Sensor_Num.setText(textArray[currentIndex]);
        Sensor_Name.setText(textArray2[currentIndex]);

        arrowbutton1.setOnClickListener(new arrowbutton1ClickListener());
        arrowbutton2.setOnClickListener(new arrowbutton2ClickListener());


        // 데이터베이스에서 데이터 가져와서 리스트에 추가
        loadDataFromDatabase();


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
                String jsonMessage = new String(mqttMessage.getPayload());
                Log.d("Debug","Received MQTT message: " + jsonMessage);

                try {
                    JSONArray jsonArray = new JSONArray(jsonMessage);

                    int gas1=0;
                    int gas2=0;
                    int gas3=0;

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        gasSensor = jsonObject.getString("Gas_Sensor");
                        value = jsonObject.getInt("value");


                        Log.d("Debug","Gas Sensor: " + gasSensor + ", Value: " + value);


                        if(gasSensor.equals("LPG")) gas1=value;
                        else if(gasSensor.equals("Metaine")) gas2=value;
                        else if(gasSensor.equals("CO")) gas3=value;

                        LPG_value = ((gas1 - 50) * 100) / (200 - 40);
                        Metaine_value = ((gas2 - 50) * 100) / (200 - 50);
                        CO_value = ((gas3 - 40) * 100) / (270 - 40);
                    }

                    if(LPG_value >60 || Metaine_value >60|| CO_value > 60 ){
                        dbHelper.insertData(gas1, gas2, gas3);
                        try {
                            dbHelper.openDataBase();
                        }catch (
                                SQLException e) {
                            e.printStackTrace();
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
        Log.d("load_Data",textArray2[0]);
        Log.d("gasSensor",gasSensor);



        if (LPG_value <= 60) {
                Sensor_State.setText("good");
            } else {
                Sensor_State.setText("danger");
            }
        }


    protected void onDestroy() {
        super.onDestroy();

        // 데이터베이스 연결 종료
        dbHelper.close();
    }


    private void loadDataFromDatabase() {
        try {
            dbHelper.openDataBase();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String query = "SELECT * FROM GAS ORDER BY gas_pk DESC";
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
                String line =  id + ". LPGLNG : " + example1 + ", Metaine : "
                        + example2 + ", CO : " + example3 +"\n     "+ now;
                result.append(line).append("\n");

                dataList.add(line);



            } while (cursor.moveToNext());

            // 커서 닫기
            cursor.close();
        }

        // 어댑터에 변경된 데이터를 알려줌
        adapter.notifyDataSetChanged();
    }

    private class arrowbutton1ClickListener implements View.OnClickListener {
        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onClick(View v) {
            currentIndex--;
            if (currentIndex < 0) {
                currentIndex = textArray.length - 1;
            }
            Sensor_Num.setText(textArray[currentIndex]);
            Sensor_Name.setText(textArray2[currentIndex]);

            Log.d("LPG_value", String.valueOf(LPG_value));
            Log.d("textArray", String.valueOf(textArray2[0]));
            // value에 따라 Sensor_State 텍스트 변경 및 저장

            if (gasSensor.equals(textArray2[currentIndex])) {   //{"LPG", "METAINE", "CO"};  static int LPG_value; static int Metaine_value; static int CO_value;
                if (gasSensor.equals("LPG")) {
                    // 백분율 텍스트 뷰 출력
                    String value_string = String.valueOf(LPG_value);
                    present_textView.setText("love");
                    Log.d("ps_textview", LPG_value + " + " + value_string);

                    if (LPG_value <= 30) {
                        Sensor_State.setText("good");
                        ps_bar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_green)); // 프로그레스 바 색상
                    } else if (LPG_value <= 60) {
                        Sensor_State.setText("good");
                    } else if (LPG_value <= 60) {
                        Sensor_State.setText("danger");
                    } else {
                        Sensor_State.setText("danger");
                        ps_bar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_red));
                    }
                }else if (gasSensor.equals("METAINE")) {
                    String value_string = String.valueOf(Metaine_value);
                    present_textView.setText("true");
                    if (Metaine_value <= 60) {
                        Sensor_State.setText("good");

                    } else {
                        Sensor_State.setText("danger");
                    }
                } else if (gasSensor.equals("CO")) {
                    String value_string = String.valueOf(CO_value);
                    present_textView.setText("hi");
                    if (CO_value <= 60) {
                        Sensor_State.setText("good");
                    } else {
                        Sensor_State.setText("danger");
                    }

                }
            }
        }
    }

    private class arrowbutton2ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            currentIndex++;
            if (currentIndex >= textArray.length) {
                currentIndex = 0;
            }
            Sensor_Num.setText(textArray[currentIndex]);
            Sensor_Name.setText(textArray2[currentIndex]);

            // value에 따라 Sensor_State 텍스트 변경 및 저장
            if (gasSensor.equals(textArray2[currentIndex])) {   //{"LPG", "METAINE", "CO"};  static int LPG_value; static int Metaine_value; static int CO_value;
                if (gasSensor.equals("LPG")) {
                    if (LPG_value <= 60) {
                        Sensor_State.setText("good");
                    } else {
                        Sensor_State.setText("danger");
                    }
                } else if (gasSensor.equals("METAINE")) {
                    if (Metaine_value <= 60) {
                        Sensor_State.setText("good");
                    } else {
                        Sensor_State.setText("danger");
                    }
                } else if (gasSensor.equals("CO")) {
                    if (CO_value <= 60) {
                        Sensor_State.setText("good");
                    } else {
                        Sensor_State.setText("danger");
                    }
                }
            }
        }
    }
}