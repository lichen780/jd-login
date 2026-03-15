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
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"deprecation", "unchecked"})
public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "AllHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.isFirstApplication) {
            log("=== 模块加载：" + lpparam.packageName + " ===");
        }

        hookGetPackageInfo(lpparam);
        hookGetApplicationInfo(lpparam);
        hookQueryIntentActivities(lpparam);
        hookResolveActivity(lpparam);
        hookFile(lpparam);
    }

    // ========== 核心 Hook：getPackageInfo ==========
    private void hookGetPackageInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                PackageInfo info = (PackageInfo) param.getResult();
                
                // 检测自身时，返回真实信息
                if (lpparam.packageName.equals(pkg)) {
                    return;
                }
                
                // 检测其他应用时，如果原方法返回 null，则伪造
                if (info == null) {
                    PackageInfo fakeInfo = createFakePackageInfo(pkg);
                    param.setResult(fakeInfo);
                    log("[" + lpparam.packageName + "] getPackageInfo(" + pkg + ") → 伪装成功");
                }
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

    // ========== 核心 Hook：getApplicationInfo ==========
    private void hookGetApplicationInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                ApplicationInfo info = (ApplicationInfo) param.getResult();
                
                // 检测自身时，返回真实信息
                if (lpparam.packageName.equals(pkg)) {
                    return;
                }
                
                // 检测其他应用时，如果原方法返回 null，则伪造
                if (info == null) {
                    ApplicationInfo fakeInfo = createFakeAppInfo(pkg);
                    param.setResult(fakeInfo);
                    log("[" + lpparam.packageName + "] getApplicationInfo(" + pkg + ") → 伪装成功");
                }
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

    // ========== Hook：queryIntentActivities ==========
    private void hookQueryIntentActivities(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                @SuppressWarnings("unchecked")
                List<ResolveInfo> result = (List<ResolveInfo>) param.getResult();
                
                if (result == null || result.isEmpty()) {
                    List<ResolveInfo> fakeList = new ArrayList<>();
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = new android.content.pm.ActivityInfo();
                    resolveInfo.activityInfo.packageName = "com.fake.package";
                    resolveInfo.activityInfo.name = "FakeActivity";
                    resolveInfo.activityInfo.enabled = true;
                    fakeList.add(resolveInfo);
                    param.setResult(fakeList);
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

    // ========== Hook：resolveActivity ==========
    private void hookResolveActivity(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ResolveInfo result = (ResolveInfo) param.getResult();
                
                if (result == null) {
                    ResolveInfo fakeInfo = new ResolveInfo();
                    fakeInfo.activityInfo = new android.content.pm.ActivityInfo();
                    fakeInfo.activityInfo.packageName = "com.fake.package";
                    fakeInfo.activityInfo.name = "FakeActivity";
                    fakeInfo.activityInfo.enabled = true;
                    param.setResult(fakeInfo);
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

    // ========== Hook：File 检测 ==========
    private void hookFile(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook existsHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();

                if (path.startsWith("/data/app/")) {
                    param.setResult(true);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(File.class, "exists", existsHook);
        } catch (Throwable ignored) {}

        XC_MethodHook isFileHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();

                if (path.startsWith("/data/app/")) {
                    param.setResult(true);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(File.class, "isFile", isFileHook);
        } catch (Throwable ignored) {}

        XC_MethodHook isDirHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();

                if (path.startsWith("/data/data/")) {
                    param.setResult(true);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(File.class, "isDirectory", isDirHook);
        } catch (Throwable ignored) {}

        XC_MethodHook canReadHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();

                if (path.startsWith("/data/app/") || path.startsWith("/data/data/")) {
                    param.setResult(true);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(File.class, "canRead", canReadHook);
        } catch (Throwable ignored) {}
    }

    // ========== 工具方法 ==========
    private PackageInfo createFakePackageInfo(String pkg) {
        PackageInfo info = new PackageInfo();
        info.packageName = pkg;
        info.versionCode = 10000;
        info.versionName = "10.0.0";
        info.firstInstallTime = System.currentTimeMillis() - 86400000;
        info.lastUpdateTime = System.currentTimeMillis() - 86400000;

        ApplicationInfo appInfo = createFakeAppInfo(pkg);
        info.applicationInfo = appInfo;

        return info;
    }

    private ApplicationInfo createFakeAppInfo(String pkg) {
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = pkg;
        info.enabled = true;
        info.sourceDir = "/data/app/" + pkg + "-1/base.apk";
        info.dataDir = "/data/data/" + pkg;
        return info;
    }

    private void log(String msg) {
        Log.i(TAG, msg);
    }
}
