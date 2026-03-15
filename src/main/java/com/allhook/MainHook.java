package com.allhook;

import android.app.ActivityManager;
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

        // PackageManager 检测
        hookGetPackageInfo(lpparam);
        hookGetApplicationInfo(lpparam);
        hookGetInstalledPackages(lpparam);
        hookGetInstalledApplications(lpparam);
        
        // Intent 检测
        hookQueryIntentActivities(lpparam);
        hookResolveActivity(lpparam);
        
        // 文件检测
        hookFile(lpparam);
        
        // ActivityManager 检测
        hookActivityManager(lpparam);
    }

    // ========== PackageManager: getPackageInfo ==========
    private void hookGetPackageInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                PackageInfo info = (PackageInfo) param.getResult();
                
                if (lpparam.packageName.equals(pkg)) {
                    return;
                }
                
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

    // ========== PackageManager: getApplicationInfo ==========
    private void hookGetApplicationInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                ApplicationInfo info = (ApplicationInfo) param.getResult();
                
                if (lpparam.packageName.equals(pkg)) {
                    return;
                }
                
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

    // ========== PackageManager: getInstalledPackages ==========
    private void hookGetInstalledPackages(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                @SuppressWarnings("unchecked")
                List<PackageInfo> result = (List<PackageInfo>) param.getResult();
                if (result == null) return;
                
                log("[" + lpparam.packageName + "] getInstalledPackages() → " + result.size() + " 个应用");
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

    // ========== PackageManager: getInstalledApplications ==========
    private void hookGetInstalledApplications(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                @SuppressWarnings("unchecked")
                List<ApplicationInfo> result = (List<ApplicationInfo>) param.getResult();
                if (result == null) return;
                
                log("[" + lpparam.packageName + "] getInstalledApplications() → " + result.size() + " 个应用");
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

    // ========== Intent: queryIntentActivities ==========
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

    // ========== Intent: resolveActivity ==========
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

    // ========== File 检测 ==========
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

    // ========== ActivityManager 检测 ==========
    private void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                @SuppressWarnings("unchecked")
                List<ActivityManager.RunningAppProcessInfo> result = 
                    (List<ActivityManager.RunningAppProcessInfo>) param.getResult();
                
                if (result == null) return;
                
                log("[" + lpparam.packageName + "] getRunningAppProcesses() → " + result.size() + " 个进程");
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                ActivityManager.class,
                "getRunningAppProcesses",
                hook
            );
        } catch (Throwable ignored) {}

        XC_MethodHook taskHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                @SuppressWarnings("unchecked")
                List<ActivityManager.RunningTaskInfo> result = 
                    (List<ActivityManager.RunningTaskInfo>) param.getResult();
                
                if (result == null) return;
                
                log("[" + lpparam.packageName + "] getRunningTasks() → " + result.size() + " 个任务");
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                ActivityManager.class,
                "getRunningTasks",
                int.class,
                taskHook
            );
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
