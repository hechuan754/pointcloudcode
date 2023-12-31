package com.example.pointcloudcode;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;


import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    private TextView predict;
    ImageView img_photo;
    private Button client_submit;
    private String base64Data;
    private String imagePath;
    private String originalImagePath;

    private File outputImage;
    private EditText editTextIP;
    private EditText editTextPort;
    private Button btnConnectServer;  // 声明连接按钮
    private Button btnUpload; // 声明上传按钮
    private TextView textViewConnectionStatus; // 显示连接状态的TextView
    private Socket socket;  // 成员变量，用于保存连接
    final int TAKE_PHOTO = 1;

    private static final int UPDATE_ok = 0;
    private static final int UPDATE_UI = 1;
    private static final int ERROR = 2;

    Uri imageUri;

    // 主线程创建消息处理器
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_UI) {
                handleUIUpdate((String) msg.obj);
            } else if (msg.what == ERROR) {
                Toast.makeText(getApplicationContext(), "预测失败", Toast.LENGTH_LONG).show();
            } else if (msg.what == UPDATE_ok) {
                textViewConnectionStatus.setText("连接服务器成功");
                Toast.makeText(getApplicationContext(), "连接服务器成功", Toast.LENGTH_LONG).show();
            }
        }
    };

    private boolean isConnected = false;

    @Override
    protected void onStart() {
        super.onStart();
        Permission.checkPermissionAndProceed(this, Permission.CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 省略权限请求结果处理
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initEvent();
    }

    // 初始化控件
    private void initViews() {
        img_photo = findViewById(R.id.img_photo);
        client_submit = findViewById(R.id.client_submit);
        textViewConnectionStatus = findViewById(R.id.textViewConnectionStatus);

        // 新添加的EditText控件
        editTextIP = findViewById(R.id.editTextIP);
        editTextPort = findViewById(R.id.editTextPort);

        // 为btnConnectServer赋值
        btnConnectServer = findViewById(R.id.btnConnectServer);
    }

    private void handleUIUpdate(String content) {
        if ("0".equals(content)) {
            predict.setText("预测失败");
        } else {
            predict.setText(content);
        }
    }

    private void connectToServer(final String ip, final String port) {
        new Thread() {
            public void run() {
                try {
                    socket = new Socket(ip, Integer.parseInt(port));
                    Thread.sleep(1000);

                    Message msg1 = new Message();
                    msg1.what = UPDATE_ok;
                    msg1.obj = socket;
                    handler.sendMessage(msg1);

                    isConnected = true;

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    // 进行拍照
    private void initEvent() {
        client_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isConnected) {
                    // 提示用户先连接服务器
                    Toast.makeText(getApplicationContext(), "请先连接服务器", Toast.LENGTH_SHORT).show();
                    return;
                }
                String ip = editTextIP.getText().toString();
                String port = editTextPort.getText().toString();

                String filename = "test.jpg";
                outputImage = new File(getExternalCacheDir(), filename);  // 拍照后照片存储路径

                // 保存图片的路径
                imagePath = outputImage.getAbsolutePath();

                try {
                    if (outputImage.exists()) {
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 使用 FileProvider 获取 Uri
                imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.pointcloudcode.fileprovider", outputImage);

                // 跳转界面到系统自带的拍照界面
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);  // 指定图片存放位置，在onActivityResult里得到的Data将为null
                startActivityForResult(intent, TAKE_PHOTO);
            }
        });



        // 新添加的按钮的事件处理
        btnConnectServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 获取用户输入的IP和端口号
                String ip = editTextIP.getText().toString();
                String port = editTextPort.getText().toString();

                // 检查输入是否为空
                if (ip.isEmpty() || port.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "请输入服务器IP和端口号", Toast.LENGTH_SHORT).show();
                    // 使用 Handler 延迟3秒后取消显示
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT).cancel();
                        }
                    }, 3000);
                    return;
                }

                // 连接服务器的逻辑...
                connectToServer(ip, port);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK) {
            // 拍照成功，处理图片
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            // 将 Bitmap 转为 Base64 字符串
            base64Data = bitmapToBase64(bitmap);

            // 保存原图路径
            originalImagePath = imagePath;

            startNetThread();  // 传递原图路径
        }
    }


    // 将 Bitmap 转为 Base64 字符串
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);  // 使用 NO_WRAP 替代 DEFAULT
    }

    private void startNetThread() {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                try {
                    if (socket == null || !socket.isConnected()) {
                        return;
                    }

                    OutputStream os = socket.getOutputStream();

                    // 将原图路径长度发送到服务器
                    int imagePathSize = originalImagePath.length();
                    String imagePathSizeStr = String.valueOf(imagePathSize);
                    os.write(imagePathSizeStr.getBytes());
                    os.flush();

                    // 将原图路径发送到服务器
                    os.write(originalImagePath.getBytes());
                    os.flush();

                    // 读取原图文件并发送到服务器
                    FileInputStream fis = new FileInputStream(originalImagePath);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    fis.close();

                    socket.shutdownOutput();

                    os.close();

                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "网络操作异常", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

}
