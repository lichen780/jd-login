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

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"deprecation", "unchecked"})
public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "AllHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.i(TAG, "模块已加载到应用：" + lpparam.packageName);

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
                
                if (info != null) {
                    Log.d(TAG, "getPackageInfo - 包名：" + pkg + " | 标志：" + flags + " | 结果：已安装 (真实)");
                    Log.d(TAG, "  └─ versionCode: " + info.versionCode + ", versionName: " + info.versionName);
                    return;
                }
                
                Log.w(TAG, "getPackageInfo - 包名：" + pkg + " | 标志：" + flags + " | 结果：未安装 → 伪造中...");
                PackageInfo fakeInfo = createFakePackageInfo(pkg);
                param.setResult(fakeInfo);
                Log.i(TAG, "  └─ 伪造成功：versionCode=" + fakeInfo.versionCode + ", versionName=" + fakeInfo.versionName);
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
            Log.i(TAG, "Hook 成功：getPackageInfo(String, int)");
        } catch (Throwable e) {
            Log.e(TAG, "Hook 失败：getPackageInfo(String, int)", e);
        }

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
            Log.i(TAG, "Hook 成功：getPackageInfo(String, int, long)");
        } catch (Throwable e) {
            Log.e(TAG, "Hook 失败：getPackageInfo(String, int, long)", e);
        }
    }

    // Hook getApplicationInfo
    private void hookGetApplicationInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                int flags = (int) param.args[1];
                ApplicationInfo info = (ApplicationInfo) param.getResult();
                
                if (info != null) {
                    Log.d(TAG, "getApplicationInfo - 包名：" + pkg + " | 结果：已安装 (真实)");
                    return;
                }
                
                Log.w(TAG, "getApplicationInfo - 包名：" + pkg + " | 结果：未安装 → 伪造中...");
                ApplicationInfo fakeInfo = new ApplicationInfo();
                fakeInfo.packageName = pkg;
                fakeInfo.enabled = true;
                fakeInfo.sourceDir = "/data/app/" + pkg + "-1/base.apk";
                fakeInfo.dataDir = "/data/data/" + pkg;
                param.setResult(fakeInfo);
                Log.i(TAG, "  └─ 伪造成功");
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
            Log.i(TAG, "Hook 成功：getApplicationInfo(String, int)");
        } catch (Throwable e) {
            Log.e(TAG, "Hook 失败：getApplicationInfo(String, int)", e);
        }
    }

    // Hook queryIntentActivities (通过 Intent 检测)
    private void hookQueryIntentActivities(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                int flags = (int) param.args[1];
                List<ResolveInfo> result = (List<ResolveInfo>) param.getResult();
                
                String intentAction = intent != null ? intent.getAction() : "null";
                String intentPackage = intent != null ? intent.getPackage() : "null";
                
                if (result == null || result.isEmpty()) {
                    Log.w(TAG, "queryIntentActivities - Action: " + intentAction + " | Package: " + intentPackage + " | 结果：空 → 伪造中...");
                    
                    List<ResolveInfo> fakeList = new ArrayList<>();
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = new android.content.pm.ActivityInfo();
                    resolveInfo.activityInfo.packageName = "com.fake.package";
                    resolveInfo.activityInfo.name = "FakeActivity";
                    resolveInfo.activityInfo.enabled = true;
                    fakeList.add(resolveInfo);
                    param.setResult(fakeList);
                    Log.i(TAG, "  └─ 伪造成功");
                } else {
                    Log.d(TAG, "queryIntentActivities - Action: " + intentAction + " | 结果：有匹配项 (数量：" + result.size() + ")");
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
            Log.i(TAG, "Hook 成功：queryIntentActivities(Intent, int)");
        } catch (Throwable e) {
            Log.e(TAG, "Hook 失败：queryIntentActivities(Intent, int)", e);
        }
    }

    // Hook resolveActivity
    private void hookResolveActivity(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                int flags = (int) param.args[1];
                ResolveInfo result = (ResolveInfo) param.getResult();
                
                String intentAction = intent != null ? intent.getAction() : "null";
                String intentPackage = intent != null ? intent.getPackage() : "null";
                
                if (result == null) {
                    Log.w(TAG, "resolveActivity - Action: " + intentAction + " | Package: " + intentPackage + " | 结果：null → 伪造中...");
                    
                    ResolveInfo fakeInfo = new ResolveInfo();
                    fakeInfo.activityInfo = new android.content.pm.ActivityInfo();
                    fakeInfo.activityInfo.packageName = "com.fake.package";
                    fakeInfo.activityInfo.name = "FakeActivity";
                    fakeInfo.activityInfo.enabled = true;
                    param.setResult(fakeInfo);
                    Log.i(TAG, "  └─ 伪造成功");
                } else {
                    Log.d(TAG, "resolveActivity - Action: " + intentAction + " | 结果：" + result.activityInfo.packageName);
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
            Log.i(TAG, "Hook 成功：resolveActivity(Intent, int)");
        } catch (Throwable e) {
            Log.e(TAG, "Hook 失败：resolveActivity(Intent, int)", e);
        }
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
}
