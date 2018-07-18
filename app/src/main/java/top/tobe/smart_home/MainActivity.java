package top.tobe.smart_home;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ImageButton light_button;
    private TextView temperature;
    private TextView humidity;

    private SharedPreferences sp;
    private String UUID;

    private String temperature_humidity_path = "https://maker.tobeh.xin/data/WS01";

    private InternetRequest internetRequest=new InternetRequest();

    private Boolean buttonCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp=getSharedPreferences("uuid&start",MODE_PRIVATE);
        UUID=sp.getString("uuid","");

        light_button=findViewById(R.id.imageButton);
        temperature=findViewById(R.id.temperature);
        humidity=findViewById(R.id.humidity);

        //灯光初始状态设置的是关闭
        light_button.setBackgroundResource(R.drawable.light_off);
        buttonCount=true;

        System.out.println("111111111111.就到这里吧！");
        //开启一个线程，作用是循环开启另一个线程
        DataShowThread();

        light_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(buttonCount){ //true就是打开灯
                    buttonCount=false; //更改状态
                    light_button.setBackgroundResource(R.drawable.light_on);
                    internetRequest.sendCmdThread(UUID,"0","DG","01","1111");
                }else{ //false就是关闭灯
                    buttonCount=true;
                    light_button.setBackgroundResource(R.drawable.light_off);
                    internetRequest.sendCmdThread(UUID,"0","DG","01","0000");
                }
            }
        });
    }

    private void DataShowThread() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        System.out.println("就到这里吧！");
                        startThread(temperature_humidity_path);
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    private void startThread(final String path) {
        new Thread() {
            private String Path = path;
            public void run() {
                //写请求网络的代码
                try {
                    System.out.println("222222222222222222222222");
                    URL url = new URL(Path);
                    //封装JSON数据
                    JSONObject Data_Package = new JSONObject();
                    Data_Package.put("TAG", "data");
                    Data_Package.put("USERNAME", "sjzb");
                    Data_Package.put("PASSWORD", "1024");
                    //转换成String类型使用输出流向服务器写
                    String content = String.valueOf(Data_Package);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(30000);
                    //设置允许输出输入
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    //设置POST方式
                    conn.setRequestMethod("POST");
                    // 设置contentType
                    conn.setRequestProperty("Content-Type", "application/json");
                    //链接conn
                    conn.connect();
                    OutputStream os = conn.getOutputStream();
                    os.write(content.getBytes());
                    os.flush();
                    System.out.println("3333333333333333333333333");
                    //获得接受码如果是200，表明成功链接
                    int code = conn.getResponseCode();
                    System.out.println(code);
                    if (code == 200) {
                        //获得输入流，即服务器端的json
                        InputStream is = conn.getInputStream();
                        String json_get = internetRequest.readTextFromSDcard(is);
                        System.out.println(json_get);
                        //然后我们把json转换成JSONObject类型得到{"Person"://{"username":"zhangsan","age":"12"}}
                        JSONObject jsonObject = new JSONObject(json_get);
                        Message msg = new Message();
                        //将json格式传给obj
                        msg.obj = jsonObject;
                        //传给handler类
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //将obj类型强制转换
            JSONObject jsonObject = (JSONObject) msg.obj;
            // TODO
            // UI界面的更新等相关操作
            //UI实现部分给主线程做
            try {
                //tag为TAG的判据
                String tag = jsonObject.getString("TAG");
                //只有收到tag:data，auth:true才执行
                //如下思路，switch语句作为选择，里面是解析的json数据的类型字段，根据不同类型调用不同函数
                //返回tag是data才解析数据
                if (tag.equals("data")) {
                    if (jsonObject.getBoolean("AUTH")) {
                        //得到数据里面的DATA的包
                        JSONObject jsonObject_data = jsonObject.getJSONObject("DATA");
                        String sensor_type = jsonObject_data.getString("stype");
                        switch (sensor_type) {
                            case "WS": //温湿度
                                String Temperature_Humidity_Value = jsonObject_data.getString("value");
                                setTemperature(Temperature_Humidity_Value);
                                break;
                            case "TR": //土壤
                                break;
                            default:
                                break;
                        }
                    }
                } else {
                    if (jsonObject.getBoolean("AUTH")) {
                        Toast.makeText(getApplicationContext(), "命令成功发送！", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "命令发送失败！", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void setTemperature(String Temperature_Humidity_Value){
        //获取子字符串
        String temperatureValue=Temperature_Humidity_Value.substring(0,2);
        String humidityValue=Temperature_Humidity_Value.substring(2,4);
        temperatureValue=temperatureValue+"℃";
        humidityValue=humidityValue+"%";
        temperature.setText(temperatureValue);
        humidity.setText(humidityValue);
    }
}
