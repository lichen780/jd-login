package com.allhook;

import android.content.pm.PackageInfo;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try {
            // hook 系统检测安装状态
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getPackageInfo",
                String.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String pkg = (String) param.args[0];
                        PackageInfo info = new PackageInfo();
                        info.packageName = pkg;
                        param.setResult(info);
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                "android.content.pm.PackageManager",
                lpparam.classLoader,
                "getPackageInfo",
                String.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String pkg = (String) param.args[0];
                        PackageInfo info = new PackageInfo();
                        info.packageName = pkg;
                        param.setResult(info);
                    }
                }
            );
        } catch (Throwable ignored) {}
    }
}
