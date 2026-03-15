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

import java.io.File;
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
        if (lpparam.isFirstApplication) {
            activePackages.add(lpparam.packageName);
            log("模块激活：" + lpparam.packageName);
        }

        hookGetPackageInfo(lpparam);
        hookGetApplicationInfo(lpparam);
        hookQueryIntentActivities(lpparam);
        hookResolveActivity(lpparam);
        hookGetInstalledPackages(lpparam);
        hookGetInstalledApplications(lpparam);
        hookFile(lpparam);
    }

    private void hookGetPackageInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                PackageInfo info = (PackageInfo) param.getResult();
                
                if (info != null) {
                    // 真实已安装，不记录日志
                    return;
                }
                
                // 未安装，伪造并记录
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
                    // 真实已安装，不记录日志
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
                    logSimple(lpparam.packageName, intentPackage, "伪装成功 (Intent)", null);
                }
                // 有匹配项，不记录日志
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
                    logSimple(lpparam.packageName, intentPackage, "伪装成功 (Activity)", null);
                }
                // 已找到，不记录日志
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

    // Hook File 类 - 检测 /data/app/ 目录
    private void hookFile(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Hook File.exists()
        XC_MethodHook existsHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();
                
                // 检查是否是 /data/app/ 目录下的 APK 路径
                if (path.startsWith("/data/app/")) {
                    // 提取包名
                    String pkg = extractPackageName(path);
                    if (pkg != null) {
                        logSimple(lpparam.packageName, pkg, "文件检测 (exists)", "path=" + path);
                        param.setResult(true); // 伪造文件存在
                    }
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(File.class, "exists", existsHook);
        } catch (Throwable ignored) {}

        // Hook File.isFile()
        XC_MethodHook isFileHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();
                
                if (path.startsWith("/data/app/")) {
                    String pkg = extractPackageName(path);
                    if (pkg != null) {
                        param.setResult(true); // 伪造是文件
                    }
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(File.class, "isFile", isFileHook);
        } catch (Throwable ignored) {}

        // Hook File.isDirectory()
        XC_MethodHook isDirHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();
                
                if (path.startsWith("/data/app/")) {
                    param.setResult(false); // 不是目录
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(File.class, "isDirectory", isDirHook);
        } catch (Throwable ignored) {}
    }

    // 从路径提取包名
    private String extractPackageName(String path) {
        // 路径格式：/data/app/com.example.app-1/base.apk 或 /data/app/com.example.app-xxx/base.apk
        if (path.contains("/data/app/")) {
            String[] parts = path.replace("/data/app/", "").split("/");
            if (parts.length > 0) {
                // 移除 -xxx 后缀
                String pkg = parts[0];
                int dashIndex = pkg.indexOf('-');
                if (dashIndex > 0) {
                    pkg = pkg.substring(0, dashIndex);
                }
                return pkg;
            }
        }
        return null;
    }

    // Hook getInstalledPackages - 批量获取已安装应用列表
    private void hookGetInstalledPackages(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                @SuppressWarnings("unchecked")
                List<PackageInfo> result = (List<PackageInfo>) param.getResult();
                
                if (result == null) {
                    return;
                }
                
                // 不自动注入，只记录
                log(lpparam.packageName + " | getInstalledPackages | 返回 " + result.size() + " 个应用");
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getInstalledPackages",
                int.class,
                hook
            );
        } catch (Throwable ignored) {}
    }

    // Hook getInstalledApplications - 获取已安装应用列表
    private void hookGetInstalledApplications(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                @SuppressWarnings("unchecked")
                List<ApplicationInfo> result = (List<ApplicationInfo>) param.getResult();
                
                if (result == null) {
                    return;
                }
                
                log(lpparam.packageName + " | getInstalledApplications | 返回 " + result.size() + " 个应用");
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getInstalledApplications",
                int.class,
                hook
            );
        } catch (Throwable ignored) {}
    }

    private ApplicationInfo createFakeAppInfo(String pkg) {
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = pkg;
        info.enabled = true;
        info.sourceDir = "/data/app/" + pkg + "-1/base.apk";
        info.dataDir = "/data/data/" + pkg;
        return info;
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
        String log = timestamp + " | 检测目标：" + targetPkg + " | 请求应用：" + requestApp + " | " + status;
        Log.i(TAG, log);
        if (details != null) {
            Log.i(TAG, "         └─ " + details);
        }
    }
    
    private void log(String msg) {
        Log.i(TAG, msg);
    }
}
