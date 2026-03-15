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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings({"deprecation", "unchecked"})
public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "AllHook";
    private static final Set<String> activePackages = new HashSet<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只在 Xposed 中勾选的应用才激活 Hook
        if (lpparam.isFirstApplication) {
            activePackages.add(lpparam.packageName);
            log("模块激活：" + lpparam.packageName);
        }
        
        hookGetPackageInfo(lpparam);
        hookGetApplicationInfo(lpparam);
        hookQueryIntentActivities(lpparam);
        hookResolveActivity(lpparam);
    }

    private void hookGetPackageInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                PackageInfo info = (PackageInfo) param.getResult();
                
                if (info != null) {
                    return;
                }
                
                PackageInfo fakeInfo = createFakePackageInfo(pkg);
                param.setResult(fakeInfo);
                logSimple(lpparam.packageName, pkg, "伪装成功", "versionCode=" + fakeInfo.versionCode + ", versionName=" + fakeInfo.versionName);
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

    private void hookGetApplicationInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                ApplicationInfo info = (ApplicationInfo) param.getResult();
                
                if (info != null) {
                    return;
                }
                
                ApplicationInfo fakeInfo = new ApplicationInfo();
                fakeInfo.packageName = pkg;
                fakeInfo.enabled = true;
                fakeInfo.sourceDir = "/data/app/" + pkg + "-1/base.apk";
                fakeInfo.dataDir = "/data/data/" + pkg;
                param.setResult(fakeInfo);
                logSimple(lpparam.packageName, pkg, "伪装成功 (AppInfo)", "sourceDir=" + fakeInfo.sourceDir);
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

    private void hookQueryIntentActivities(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                List<ResolveInfo> result = (List<ResolveInfo>) param.getResult();
                
                String intentPackage = intent != null ? intent.getPackage() : "null";
                
                if (result == null || result.isEmpty()) {
                    List<ResolveInfo> fakeList = new ArrayList<>();
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = new android.content.pm.ActivityInfo();
                    resolveInfo.activityInfo.packageName = "com.fake.package";
                    resolveInfo.activityInfo.name = "FakeActivity";
                    resolveInfo.activityInfo.enabled = true;
                    fakeList.add(resolveInfo);
                    param.setResult(fakeList);
                    logSimple(lpparam.packageName, intentPackage, "伪装成功 (Intent)", "packageName=com.fake.package");
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

    private void hookResolveActivity(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                ResolveInfo result = (ResolveInfo) param.getResult();
                
                String intentPackage = intent != null ? intent.getPackage() : "null";
                
                if (result == null) {
                    ResolveInfo fakeInfo = new ResolveInfo();
                    fakeInfo.activityInfo = new android.content.pm.ActivityInfo();
                    fakeInfo.activityInfo.packageName = "com.fake.package";
                    fakeInfo.activityInfo.name = "FakeActivity";
                    fakeInfo.activityInfo.enabled = true;
                    param.setResult(fakeInfo);
                    logSimple(lpparam.packageName, intentPackage, "伪装成功 (Activity)", "packageName=com.fake.package");
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
    
    private void logSimple(String requestApp, String targetPkg, String status, String details) {
        // 只记录已激活（Xposed 勾选）的应用
        if (!activePackages.contains(requestApp)) {
            return;
        }
        
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(new Date());
        Log.i(TAG, timestamp + " | 检测目标：" + targetPkg + " | 请求应用：" + requestApp + " | " + status);
        if (details != null) {
            Log.i(TAG, "         └─ " + details);
        }
    }
    
    private void log(String msg) {
        Log.i(TAG, msg);
    }
}
