package com.allhook;

import android.content.pm.PackageInfo;
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
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                PackageInfo info = new PackageInfo();
                info.packageName = pkg;
                info.versionCode = 1;
                info.versionName = "1.0.0";
                param.setResult(info);
            }
        };

        XposedHelpers.findAndHookMethod(
            "android.app.ApplicationPackageManager",
            lpparam.classLoader,
            "getPackageInfo",
            String.class,
            int.class,
            hook
        );
    }
}
