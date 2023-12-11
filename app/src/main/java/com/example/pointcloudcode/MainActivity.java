package com.example.pointcloudcode;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import com.example.pointcloudcode.Permission;

import static android.util.Base64.*;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.net.Uri;
import java.io.FileOutputStream;
import android.database.Cursor;
import android.os.Build;
import android.Manifest;
import android.os.Bundle;
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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    private TextView predict;
    ImageView img_photo;
    private Button client_submit;
    private File outputImage;
    private EditText editTextIP;
    private EditText editTextPort;
    private Button btnConnectServer;  // 声明连接按钮
    private Button btnUpload; // 声明上传按钮
    private TextView textViewConnectionStatus; // 显示连接状态的TextView
    private Socket socket;  // 成员变量，用于保存连接
    final int TAKE_PHOTO = 1;

    private static final int SELECT_PHOTO = 2;
    private static final int CAMERA_PERMISSION_CODE = 1001;
    private static final int GALLERY_PERMISSION_REQUEST_CODE = 1002;

    Uri imageUri;
//    File outputImage;

    private static final int UPDATE_ok = 0;
    private static final int UPDATE_UI = 1;
    private static final int ERROR = 2;

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
        if (requestCode == GALLERY_PERMISSION_REQUEST_CODE) {
            if (Permission.isPermissionGranted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                selectPhotoFromAlbum();
            } else {
                // 权限被拒绝，显示适当的提示
                Toast.makeText(this, "需要相册存储权限来选择照片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化按钮
        btnUpload = findViewById(R.id.btnUpload);

        // 给按钮设置点击事件监听器
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (handler == null) {
                    Log.e("MainActivity", "Handler is null. Cannot start network thread.");
                    return;
                }
                // 处理上传按钮点击事件的逻辑
                startNetThread();
            }
        });


        initViews();
        initEvent();
    }

    //初始化控件
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

    //进行拍照
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

                String filename = "test.png";
                outputImage = new File(getExternalCacheDir(),filename);  //拍照后照片存储路径
                try {
                    if (outputImage.exists()){
                    outputImage.delete();
                }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    //图片的url
                    imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.pointcloudcode.fileprovider", outputImage);
                } else {
                    imageUri = Uri.fromFile(outputImage);
                }
                //跳转界面到系统自带的拍照界面
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");  //调用手机拍照功能其实就是启动一个activity
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);  //指定图片存放位置，指定后，在onActivityResult里得到的Data将为null
                startActivityForResult(intent, TAKE_PHOTO);  //开启相机
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



        // 从相册中选择照片
        Button btnSelectFromAlbum = findViewById(R.id.btnSelectFromAlbum);
        btnSelectFromAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 检查相册存储权限
                if (Permission.isPermissionGranted(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // 已授予权限，选择照片
                    selectPhotoFromAlbum();
                } else {
                    // 请求相册存储权限
                    Permission.requestPermission(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_PERMISSION_REQUEST_CODE);
                }
            }
        });

    }

    // 将拍摄的图片保存到相册
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(outputImage);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }
    private void selectPhotoFromAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_PHOTO);
    }
    private void saveImageToGallery(File imageFile) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(imageFile);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    private void saveImageToGallery(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File file = new File(imagePath);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);
        }
    }
    private String getRealPathFromURI(Uri contentUri) {
        String filePath;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 处理 Android 10 及以上版本的方式
            try {
                InputStream inputStream = getContentResolver().openInputStream(contentUri);
                File file = File.createTempFile("temp", "jpg", getCacheDir());
                if (file != null) {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.close();
                    filePath = file.getAbsolutePath();
                } else {
                    filePath = contentUri.getPath(); // 获取不到正确路径时的备选方案
                }
            } catch (IOException e) {
                e.printStackTrace();
                filePath = contentUri.getPath(); // 获取不到正确路径时的备选方案
            }
        } else {
            // 处理 Android 10 以下版本的方式
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor == null) {
                filePath = contentUri.getPath();
            } else {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                filePath = cursor.getString(column_index);
                cursor.close();
            }
        }
        return filePath;
    }
    //照片接收
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        //将图片显示
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        img_photo.setImageBitmap(bitmap);
                        galleryAddPic();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "加载拍摄的照片时出错", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK && data != null) {
                    try {
                        // 处理从相册选择的图片
                        Uri selectedImage = data.getData();
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage));
                        img_photo.setImageBitmap(bitmap);

                        // 保存从相册选择的图片到相册
                        saveImageToGallery(getRealPathFromURI(selectedImage));
                        imageUri = selectedImage;

                        // 启动网络线程处理数据
                        startNetThread();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "从相册选择照片时出错", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            default:
                break;
        }
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

                    if (imageUri != null && "file".equals(imageUri.getScheme())) {
                        // 图片来自拍照
                        File imageFile = new File(imageUri.getPath());
                        saveImageToGallery(imageFile);
                    }


                    // 获取socket读写流
                    OutputStream os = socket.getOutputStream();

                    // 读取原图的文件流
                    FileInputStream fis;

                    if (imageUri != null && "content".equals(imageUri.getScheme())) {
                        // 如果图片来自相册
                        String imagePath = getRealPathFromURI(imageUri);
                        fis = new FileInputStream(new File(imagePath));
                    } else {
                        fis = new FileInputStream(outputImage);
                    }

                    byte[] buffer = new byte[8192]; // 设置缓冲区大小
                    int bytesRead;

                    while ((bytesRead = fis.read(buffer)) != -1) {
                        // 发送当前块的数据
                        os.write(buffer, 0, bytesRead);
                        os.flush();
                    }

                    // 关闭发送数据的数据流，数据发送完毕
                    socket.shutdownOutput();

                    // 关闭连接和流
                    os.close();
                    fis.close();

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

