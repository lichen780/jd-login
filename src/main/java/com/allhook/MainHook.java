package com.allhook;

import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        hookPackageManager(lpparam);
    }

    private void hookPackageManager(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                PackageInfo info = (PackageInfo) param.getResult();
                
                // 如果是检测当前应用自身，返回真实信息
                if (lpparam.packageName.equals(pkg)) {
                    return;
                }
                
                // 如果原方法返回 null（应用未安装），伪造一个已安装的信息
                if (info == null) {
                    info = new PackageInfo();
                    info.packageName = pkg;
                    info.versionCode = 10000;
                    info.versionName = "10.0.0";
                    info.firstInstallTime = System.currentTimeMillis() - 86400000; // 1 天前
                    info.lastUpdateTime = System.currentTimeMillis() - 86400000;
                    
                    // 创建 ApplicationInfo
                    ApplicationInfo appInfo = new ApplicationInfo();
                    appInfo.packageName = pkg;
                    appInfo.enabled = true;
                    info.applicationInfo = appInfo;
                    
                    param.setResult(info);
                }
            }
        };

        // Hook 2 参数版本
        XposedHelpers.findAndHookMethod(
            "android.app.ApplicationPackageManager",
            lpparam.classLoader,
            "getPackageInfo",
            String.class,
            int.class,
            hook
        );

        // Hook 3 参数版本 (Android 11+)
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
}
