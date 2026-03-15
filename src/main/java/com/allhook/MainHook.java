package com.allhook;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"deprecation", "unchecked"})
public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "AllHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        log("模块已加载到应用：" + lpparam.packageName);
        
        hookGetPackageInfo(lpparam);
        hookGetApplicationInfo(lpparam);
        hookQueryIntentActivities(lpparam);
        hookResolveActivity(lpparam);
    }

    // Hook getPackageInfo
    private void hookGetPackageInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                int flags = (int) param.args[1];
                PackageInfo info = (PackageInfo) param.getResult();
                
                String requestApp = lpparam.packageName;
                
                if (info != null) {
                    logFormatted(requestApp, pkg, "已安装", "真实信息", 
                        "versionCode=" + info.versionCode + ", versionName=" + info.versionName);
                    return;
                }
                
                logFormatted(requestApp, pkg, "未安装", "开始伪装", null);
                PackageInfo fakeInfo = createFakePackageInfo(pkg);
                param.setResult(fakeInfo);
                logFormatted(requestApp, pkg, "伪装成功", "返回伪造信息", 
                    "versionCode=" + fakeInfo.versionCode + ", versionName=" + fakeInfo.versionName);
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getPackageInfo",
                String.class,
                int.class,
                hook
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getPackageInfo",
                String.class,
                int.class,
                long.class,
                hook
            );
        } catch (Throwable ignored) {}
    }

    // Hook getApplicationInfo
    private void hookGetApplicationInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                int flags = (int) param.args[1];
                ApplicationInfo info = (ApplicationInfo) param.getResult();
                
                String requestApp = lpparam.packageName;
                
                if (info != null) {
                    logFormatted(requestApp, pkg, "已安装", "真实信息", null);
                    return;
                }
                
                logFormatted(requestApp, pkg, "未安装", "开始伪装", null);
                ApplicationInfo fakeInfo = new ApplicationInfo();
                fakeInfo.packageName = pkg;
                fakeInfo.enabled = true;
                fakeInfo.sourceDir = "/data/app/" + pkg + "-1/base.apk";
                fakeInfo.dataDir = "/data/data/" + pkg;
                param.setResult(fakeInfo);
                logFormatted(requestApp, pkg, "伪装成功", "返回伪造信息", 
                    "sourceDir=" + fakeInfo.sourceDir);
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getApplicationInfo",
                String.class,
                int.class,
                hook
            );
        } catch (Throwable ignored) {}
    }

    // Hook queryIntentActivities (通过 Intent 检测)
    private void hookQueryIntentActivities(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                int flags = (int) param.args[1];
                List<ResolveInfo> result = (List<ResolveInfo>) param.getResult();
                
                String requestApp = lpparam.packageName;
                String intentAction = intent != null ? intent.getAction() : "null";
                String intentPackage = intent != null ? intent.getPackage() : "null";
                
                if (result == null || result.isEmpty()) {
                    logFormatted(requestApp, intentPackage, "无匹配", "开始伪装", "Action=" + intentAction);
                    
                    List<ResolveInfo> fakeList = new ArrayList<>();
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = new android.content.pm.ActivityInfo();
                    resolveInfo.activityInfo.packageName = "com.fake.package";
                    resolveInfo.activityInfo.name = "FakeActivity";
                    resolveInfo.activityInfo.enabled = true;
                    fakeList.add(resolveInfo);
                    param.setResult(fakeList);
                    logFormatted(requestApp, intentPackage, "伪装成功", "返回伪造列表", "size=1");
                } else {
                    logFormatted(requestApp, intentPackage, "有匹配", "真实信息", "size=" + result.size());
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "queryIntentActivities",
                Intent.class,
                int.class,
                hook
            );
        } catch (Throwable ignored) {}
    }

    // Hook resolveActivity
    private void hookResolveActivity(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                int flags = (int) param.args[1];
                ResolveInfo result = (ResolveInfo) param.getResult();
                
                String requestApp = lpparam.packageName;
                String intentAction = intent != null ? intent.getAction() : "null";
                String intentPackage = intent != null ? intent.getPackage() : "null";
                
                if (result == null) {
                    logFormatted(requestApp, intentPackage, "未找到", "开始伪装", "Action=" + intentAction);
                    
                    ResolveInfo fakeInfo = new ResolveInfo();
                    fakeInfo.activityInfo = new android.content.pm.ActivityInfo();
                    fakeInfo.activityInfo.packageName = "com.fake.package";
                    fakeInfo.activityInfo.name = "FakeActivity";
                    fakeInfo.activityInfo.enabled = true;
                    param.setResult(fakeInfo);
                    logFormatted(requestApp, intentPackage, "伪装成功", "返回伪造信息", 
                        "packageName=" + fakeInfo.activityInfo.packageName);
                } else {
                    logFormatted(requestApp, intentPackage, "已找到", "真实信息", 
                        "packageName=" + result.activityInfo.packageName);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.PackageManager",
                lpparam.classLoader,
                "resolveActivity",
                Intent.class,
                int.class,
                hook
            );
        } catch (Throwable ignored) {}
    }

    private PackageInfo createFakePackageInfo(String pkg) {
        PackageInfo info = new PackageInfo();
        info.packageName = pkg;
        info.versionCode = 10000;
        info.versionName = "10.0.0";
        info.firstInstallTime = System.currentTimeMillis() - 86400000;
        info.lastUpdateTime = System.currentTimeMillis() - 86400000;
        
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.packageName = pkg;
        appInfo.enabled = true;
        appInfo.sourceDir = "/data/app/" + pkg + "-1/base.apk";
        appInfo.dataDir = "/data/data/" + pkg;
        info.applicationInfo = appInfo;
        
        return info;
    }
    
    private void log(String msg) {
        Log.i(TAG, msg);
    }
    
    private void logFormatted(String requestApp, String targetPkg, String status, String type, String details) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(getTimestamp()).append("】\n");
        sb.append("  请求应用：").append(requestApp).append("\n");
        sb.append("  检测目标：").append(targetPkg).append("\n");
        sb.append("  安装状态：").append(status).append("\n");
        sb.append("  返回类型：").append(type);
        if (details != null) {
            sb.append("\n  详细信息：").append(details);
        }
        Log.i(TAG, sb.toString());
    }
    
    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return sdf.format(new Date());
    }
}
