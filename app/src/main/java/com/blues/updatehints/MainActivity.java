package com.blues.updatehints;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private static String versionCode;
    private CustomDialogVersion customDialog;
    private TextView tv_sure;
    private View dialogView;
    private static String filePath;
    public final static String SD_FOLDER = Environment.getExternalStorageDirectory() + "/VersionChecker/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getVersionCode(this);
        //首页展示升级提示
        dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_version, null);
        customDialog = new CustomDialogVersion(MainActivity.this, dialogView);
        tv_sure = (TextView) dialogView.findViewById(R.id.tv_sure);
        tv_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] perms = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                if (!EasyPermissions.hasPermissions(MainActivity.this, perms)) {
                    EasyPermissions.requestPermissions(MainActivity.this, "需要读取手机存储权限！", 10086, perms);
                } else {
                    installThread();
                }
            }
        });
        getVersion();
    }

    // 对比服务器跟本地版本号，服务器版本较大则提示升级
    private void getVersion() {
//        LogUtils.d("服务器返回版本号：" + model.getData().getVersion());
        if (1 < Integer.parseInt(versionCode)) {
            //开启升级弹窗
            customDialog.show();
            customDialog.setCanceledOnTouchOutside(false);
        }
    }

    private void installThread() {
        //弹出对话框app内下载
        final ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setCancelable(false);// 必须一直下载完，不可取消
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMessage("正在下载安装包，请稍后……");
        pd.setTitle("版本升级");
        pd.show();
        new Thread() {
            @Override
            public void run() {
                try {
                    File file = downloadFile("https://raw.githubusercontent.com/hust201010701/XRadarView/master/app-debug.apk", "UpdateHints", pd);
//                                sleep(3000);
                    installApk(getApplicationContext(), file);
                    // 结束掉进度条对话框
                    pd.dismiss();
                } catch (Exception e) {
                    pd.dismiss();
                }
            }
        }.start();
    }

    /**
     * 从服务器下载最新更新文件
     *
     * @param path 下载路径
     * @param pd   进度条
     * @return
     * @throws Exception
     */
    public static File downloadFile(String path, String appName, ProgressDialog pd) throws Exception {
        // 如果相等的话表示当前的sdcard挂载在手机上并且是可用的
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            // 获取到文件的大小
            pd.setMax(conn.getContentLength());
            InputStream is = conn.getInputStream();
            String fileName = SD_FOLDER + appName + ".apk";
            filePath = fileName;
            File file = new File(fileName);
            // 目录不存在创建目录
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            byte[] buffer = new byte[1024];
            int len;
            int total = 0;
            while ((len = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                total += len;
                // 获取当前下载量
                pd.setProgress(total);
            }
            fos.close();
            bis.close();
            is.close();
            return file;
        } else {
            throw new IOException("未发现有SD卡");
        }
    }

    /**
     * 安装apk(兼容7.0)
     */
    public static void installApk(Context mContext, File file) {
        Uri fileUri;
        Intent it = new Intent();
        it.setAction(Intent.ACTION_VIEW);
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// 防止打不开应用
        if (Build.VERSION.SDK_INT >= 24) {
            //判读版本是否在7.0以上
            //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
            fileUri = FileProvider.getUriForFile(mContext, "com.blus.updatehints.fileprovider", file);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            it.setDataAndType(fileUri, "application/vnd.android.package-archive");
        } else {
            fileUri = Uri.fromFile(file);
            it.setDataAndType(fileUri, "application/vnd.android.package-archive");
        }
        mContext.startActivity(it);

    }

    /**
     * 广播监听apk文件是否安装完成
     */
    public static class AppInstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            PackageManager manager = context.getPackageManager();
            if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
//                Toast.makeText(context, "apk文件已删除"+packageName, Toast.LENGTH_LONG).show();
                deleteFile();
            }
        }
    }

    /**
     * 删除安装文件
     */
    public static void deleteFile() {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * get App versionCode
     *
     * @param context
     * @return
     */
    public String getVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
//        String versionCode="";
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionCode = packageInfo.versionCode + "";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }
}
