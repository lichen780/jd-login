package com.allhook;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"deprecation", "unchecked"})
public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        hookGetPackageInfo(lpparam);
        hookGetApplicationInfo(lpparam);
        hookGetInstalledPackages(lpparam);
        hookGetInstalledApplications(lpparam);
        hookQueryIntentActivities(lpparam);
        hookResolveActivity(lpparam);
        hookQueryBroadcastReceivers(lpparam);
        hookGetSignatures(lpparam);
        hookSigningInfo(lpparam);
        hookFile(lpparam);
        hookContentResolver(lpparam);
        hookActivityManager(lpparam);
        hookRuntime(lpparam);
        hookProcessBuilder(lpparam);
        hookCreatePackageContext(lpparam);
    }

    private void hookGetPackageInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                if (lpparam.packageName.equals(pkg)) return;
                if (param.getResult() == null) {
                    param.setResult(createFakePackageInfo(pkg));
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getPackageInfo", String.class, int.class, hook);
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getPackageInfo", String.class, int.class, long.class, hook);
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getPackageInfoAsUser", String.class, int.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookGetApplicationInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String pkg = (String) param.args[0];
                if (lpparam.packageName.equals(pkg)) return;
                if (param.getResult() == null) {
                    param.setResult(createFakeAppInfo(pkg));
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getApplicationInfo", String.class, int.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookGetInstalledPackages(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // 不自动注入，只记录
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledPackages", int.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookGetInstalledApplications(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // 不自动注入，只记录
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledApplications", int.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookQueryIntentActivities(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List<ResolveInfo> result = (List<ResolveInfo>) param.getResult();
                if (result == null || result.isEmpty()) {
                    List<ResolveInfo> fakeList = new ArrayList<>();
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = new android.content.pm.ActivityInfo();
                    resolveInfo.activityInfo.packageName = "com.tencent.mm";
                    resolveInfo.activityInfo.name = "FakeActivity";
                    resolveInfo.activityInfo.enabled = true;
                    fakeList.add(resolveInfo);
                    param.setResult(fakeList);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "queryIntentActivities", Intent.class, int.class, hook);
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
                    fakeInfo.activityInfo.packageName = "com.tencent.mm";
                    fakeInfo.activityInfo.name = "FakeActivity";
                    fakeInfo.activityInfo.enabled = true;
                    param.setResult(fakeInfo);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.PackageManager", lpparam.classLoader, "resolveActivity", Intent.class, int.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookQueryBroadcastReceivers(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                List<ResolveInfo> result = (List<ResolveInfo>) param.getResult();
                if (result == null || result.isEmpty()) {
                    List<ResolveInfo> fakeList = new ArrayList<>();
                    ResolveInfo resolveInfo = new ResolveInfo();
                    resolveInfo.activityInfo = new android.content.pm.ActivityInfo();
                    resolveInfo.activityInfo.packageName = "com.tencent.mm";
                    resolveInfo.activityInfo.name = "FakeReceiver";
                    resolveInfo.activityInfo.enabled = true;
                    fakeList.add(resolveInfo);
                    param.setResult(fakeList);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "queryBroadcastReceivers", Intent.class, int.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookGetSignatures(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Signature[] result = (Signature[]) param.getResult();
                if (result == null || result.length == 0) {
                    param.setResult(new Signature[]{new Signature(new byte[64])});
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getSignatures", String.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookSigningInfo(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (Build.VERSION.SDK_INT < 28) return;
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.getResult() == null) {
                    param.setResult(new android.content.pm.SigningInfo());
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getSigningInfo", String.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookFile(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        hookFileMethod(lpparam, "exists");
        hookFileMethod(lpparam, "isFile");
        hookFileMethod(lpparam, "isDirectory");
        hookFileMethod(lpparam, "canRead");
        hookFileMethod(lpparam, "canWrite");
        hookFileMethodLength(lpparam);
        hookFileMethodListFiles(lpparam);
    }

    private void hookFileMethod(XC_LoadPackage.LoadPackageParam lpparam, String methodName) {
        XC_MethodHook hook = new XC_MethodHook() {
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
            XposedHelpers.findAndHookMethod(File.class, methodName, hook);
        } catch (Throwable ignored) {}
    }

    private void hookFileMethodLength(XC_LoadPackage.LoadPackageParam lpparam) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();
                if (path.startsWith("/data/app/") || path.startsWith("/data/data/")) {
                    param.setResult(1000000L);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(File.class, "length", hook);
        } catch (Throwable ignored) {}
    }

    private void hookFileMethodListFiles(XC_LoadPackage.LoadPackageParam lpparam) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.thisObject;
                String path = file.getAbsolutePath();
                if (path.startsWith("/data/data/")) {
                    param.setResult(new File[]{new File("/data/data/com.tencent.mm")});
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(File.class, "listFiles", hook);
        } catch (Throwable ignored) {}
    }

    private void hookContentResolver(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook queryHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.getResult() == null) {
                    Uri uri = (Uri) param.args[0];
                    if (uri != null && uri.getAuthority() != null) {
                        MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "value"});
                        cursor.addRow(new Object[]{1, "fake"});
                        param.setResult(cursor);
                    }
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.content.ContentResolver", lpparam.classLoader, "query", Uri.class, String[].class, String.class, String[].class, String.class, queryHook);
        } catch (Throwable ignored) {}

        XC_MethodHook callHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.getResult() == null) {
                    android.os.Bundle bundle = new android.os.Bundle();
                    bundle.putString("result", "ok");
                    param.setResult(bundle);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod("android.content.ContentResolver", lpparam.classLoader, "call", Uri.class, String.class, String.class, android.os.Bundle.class, callHook);
        } catch (Throwable ignored) {}
    }

    private void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // 不自动注入进程
            }
        };
        try {
            XposedHelpers.findAndHookMethod(ActivityManager.class, "getRunningAppProcesses", hook);
        } catch (Throwable ignored) {}
    }

    private void hookRuntime(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object cmd = param.args[0];
                String command = cmd instanceof String ? (String) cmd : String.join(" ", (String[]) cmd);
                if (command.contains("pm list packages") || command.contains("dumpsys package") || command.contains("ls /data/data/")) {
                    param.setResult(new FakeProcess());
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String.class, hook);
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String[].class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookProcessBuilder(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                List<String> cmd = (List<String>) param.args[0];
                if (cmd != null) {
                    for (String c : cmd) {
                        if (c.contains("pm list packages") || c.contains("dumpsys package") || c.contains("ls /data/data/")) {
                            param.setResult(new FakeProcess());
                            break;
                        }
                    }
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(ProcessBuilder.class, "start", hook);
        } catch (Throwable ignored) {}
    }

    private void hookCreatePackageContext(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.getResult() == null) {
                    param.setResult(lpparam.classLoader);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(Context.class, "createPackageContext", String.class, int.class, hook);
        } catch (Throwable ignored) {}
    }

    private PackageInfo createFakePackageInfo(String pkg) {
        PackageInfo info = new PackageInfo();
        info.packageName = pkg;
        info.versionCode = 10000;
        info.versionName = "10.0.0";
        info.firstInstallTime = System.currentTimeMillis() - 86400000;
        info.lastUpdateTime = System.currentTimeMillis() - 86400000;
        info.applicationInfo = createFakeAppInfo(pkg);
        try {
            info.signatures = new Signature[]{new Signature(new byte[64])};
        } catch (Throwable ignored) {}
        return info;
    }

    private ApplicationInfo createFakeAppInfo(String pkg) {
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = pkg;
        info.enabled = true;
        info.sourceDir = "/data/app/" + pkg + "-1/base.apk";
        info.dataDir = "/data/data/" + pkg;
        info.publicSourceDir = info.sourceDir;
        info.nativeLibraryDir = "/data/app/" + pkg + "-1/lib";
        return info;
    }

    static class FakeProcess extends Process {
        @Override public OutputStream getOutputStream() { return new ByteArrayOutputStream(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public int waitFor() throws InterruptedException { return 0; }
        @Override public int exitValue() { return 0; }
        @Override public void destroy() {}
    }
}
