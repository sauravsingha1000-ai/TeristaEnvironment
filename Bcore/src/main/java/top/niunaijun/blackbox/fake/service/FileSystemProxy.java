package top.niunaijun.blackbox.fake.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class FileSystemProxy extends ClassInvocationStub {
    public static final String TAG = "FileSystemProxy";

    public FileSystemProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // No direct injection needed
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // ==============================
    // EXISTING HOOKS (SAFE)
    // ==============================

    @ProxyMethod("mkdirs")
    public static class Mkdirs extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                File file = (File) who;
                String path = file.getAbsolutePath();

                if (path.contains("Helium Crashpad") || path.contains("HeliumCrashReporter")) {
                    Slog.d(TAG, "mkdirs bypass: " + path);
                    return true;
                }

                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "mkdirs failed", e);
                return true;
            }
        }
    }

    @ProxyMethod("mkdir")
    public static class Mkdir extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                File file = (File) who;
                String path = file.getAbsolutePath();

                if (path.contains("Helium Crashpad") || path.contains("HeliumCrashReporter")) {
                    Slog.d(TAG, "mkdir bypass: " + path);
                    return true;
                }

                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "mkdir failed", e);
                return true;
            }
        }
    }

    @ProxyMethod("isDirectory")
    public static class IsDirectory extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                File file = (File) who;
                String path = file.getAbsolutePath();

                if (path.contains("Helium Crashpad") || path.contains("HeliumCrashReporter")) {
                    return true;
                }

                return method.invoke(who, args);
            } catch (Exception e) {
                return false;
            }
        }
    }

    // ==============================
    // 🔥 ROOT FILE DETECTION FIX
    // ==============================

    @ProxyMethod("exists")
    public static class Exists extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                File file = (File) who;
                String path = file.getAbsolutePath();

                if (path.equals("/system/xbin/su") ||
                        path.equals("/system/bin/su") ||
                        path.equals("/system/sbin/su")) {

                    Slog.d(TAG, "FakeRoot: su exists -> " + path);
                    return true;
                }

                return method.invoke(who, args);
            } catch (Exception e) {
                return false;
            }
        }
    }
}
