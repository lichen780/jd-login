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
        
        // 其他检测
        hookPackageManagerFlags(lpparam);
        hookPackageParser(lpparam);
    }

    // ========== PackageManager 检测 ==========
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
                log("[" + lpparam.packageName + "] getPackageInfo(" + pkg + ") → 伪装成功");
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

                ApplicationInfo fakeInfo = createFakeAppInfo(pkg);
                param.setResult(fakeInfo);
                log("[" + lpparam.packageName + "] getApplicationInfo(" + pkg + ") → 伪装成功");
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

    private void hookGetInstalledPackages(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                @SuppressWarnings("unchecked")
                List<PackageInfo> result = (List<PackageInfo>) param.getResult();
                if (result == null) return;
                
                log("[" + lpparam.packageName + "] getInstalledPackages() → 返回 " + result.size() + " 个应用");
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

    private void hookGetInstalledApplications(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                @SuppressWarnings("unchecked")
                List<ApplicationInfo> result = (List<ApplicationInfo>) param.getResult();
                if (result == null) return;
                
                log("[" + lpparam.packageName + "] getInstalledApplications() → 返回 " + result.size() + " 个应用");
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

    // ========== Intent 检测 ==========
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
                    log("[" + lpparam.packageName + "] queryIntentActivities() → 伪装成功");
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
                ResolveInfo result = (ResolveInfo) param.getResult();
                
                if (result == null) {
                    ResolveInfo fakeInfo = new ResolveInfo();
                    fakeInfo.activityInfo = new android.content.pm.ActivityInfo();
                    fakeInfo.activityInfo.packageName = "com.fake.package";
                    fakeInfo.activityInfo.name = "FakeActivity";
                    fakeInfo.activityInfo.enabled = true;
                    param.setResult(fakeInfo);
                    log("[" + lpparam.packageName + "] resolveActivity() → 伪装成功");
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

    // ========== 文件检测 ==========
    private void hookFile(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Hook File.exists()
        XC_MethodHook existsHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();

                if (path.startsWith("/data/app/")) {
                    String pkg = extractPackageName(path);
                    if (pkg != null) {
                        log("[" + lpparam.packageName + "] File.exists(" + pkg + ") → 返回存在");
                        param.setResult(true);
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
                    param.setResult(true);
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

                if (path.startsWith("/data/data/")) {
                    String pkg = extractPackageNameFromDataDir(path);
                    if (pkg != null) {
                        log("[" + lpparam.packageName + "] File.isDirectory(" + pkg + ") → 返回存在");
                        param.setResult(true);
                    }
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(File.class, "isDirectory", isDirHook);
        } catch (Throwable ignored) {}

        // Hook File.canRead()
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

        // Hook File.length()
        XC_MethodHook lengthHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();

                if (path.startsWith("/data/app/")) {
                    param.setResult(1000000L); // 返回 1MB
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(File.class, "length", lengthHook);
        } catch (Throwable ignored) {}
    }

    // ========== 其他检测 ==========
    private void hookPackageManagerFlags(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook enabledSettingHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int result = (param.getResult() != null) ? (int) param.getResult() : 0;
                
                if (result == 1 || result == 3) {
                    param.setResult(2);
                    log("[" + lpparam.packageName + "] getApplicationEnabledSetting() → 伪装为启用");
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getApplicationEnabledSetting",
                String.class,
                enabledSettingHook
            );
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getComponentEnabledSetting",
                "android.content.ComponentName",
                enabledSettingHook
            );
        } catch (Throwable ignored) {}
    }

    // Hook PackageParser - 防止反射检测
    private void hookPackageParser(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.getResult() == null) {
                    log("[" + lpparam.packageName + "] PackageParser → 拦截异常");
                    param.setResult(new PackageInfo());
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(
                "android.content.pm.PackageParser",
                lpparam.classLoader,
                "parsePackage",
                File.class,
                int.class,
                hook
            );
        } catch (Throwable ignored) {}
    }

    // ========== 工具方法 ==========
    private String extractPackageName(String path) {
        if (path.contains("/data/app/")) {
            String[] parts = path.replace("/data/app/", "").split("/");
            if (parts.length > 0) {
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

    private String extractPackageNameFromDataDir(String path) {
        if (path.contains("/data/data/")) {
            String[] parts = path.replace("/data/data/", "").split("/");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return null;
    }

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
