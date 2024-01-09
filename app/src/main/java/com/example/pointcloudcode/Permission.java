// Permission.java

package com.example.pointcloudcode;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.app.Activity;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Permission {
    public static final int CAMERA_PERMISSION_CODE = 1001;
    public static final int GALLERY_PERMISSION_REQUEST_CODE = 1002;

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    public static boolean isPermissionGranted(Activity activity, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = ContextCompat.checkSelfPermission(activity, permission);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static boolean isPermissionGranted(Activity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            for (String permission : PERMISSIONS) {
                int checkPermission = ContextCompat.checkSelfPermission(activity, permission);
                if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }

    public static void requestPermission(Activity activity, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    public static void checkPermissionAndProceed(Activity activity, int requestCode) {
        if (isPermissionGranted(activity)) {
            Log.i("PERMISSION", "权限已授予");
            // 执行需要权限的操作
        } else {
            requestPermission(activity, PERMISSIONS, requestCode);
        }
    }

    public static void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults, Activity activity) {
        if (requestCode == CAMERA_PERMISSION_CODE || requestCode == GALLERY_PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permission", permissions[i] + " 权限被拒绝！");
                    // 处理权限被拒绝的情况
                    showPermissionDeniedDialog(activity);
                    return;
                }
            }
            Log.i("Permission", "所有权限已授予");
            // 执行需要权限的操作
        }
    }

    private static void showPermissionDeniedDialog(Activity activity) {
        // 在这里可以显示一个对话框，提示用户需要权限才能继续使用应用
        // 或者提供跳转到应用程序设置页面的选项，让用户手动授予权限
    }
}
