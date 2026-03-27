package top.niunaijun.blackbox.fake.service;

import java.io.File;
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
        return File.class;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // no-op
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("mkdirs")
    public static class Mkdirs extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                File file = (File) who;
                String path = file.getAbsolutePath();

                if (path.contains("Helium Crashpad") || path.contains("HeliumCrashReporter")) {
                    Slog.d(TAG, "FileSystem: mkdirs called for Helium crash path: " + path + ", returning true");
                    return true;
                }

                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "FileSystem: mkdirs failed, returning true", e);
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
                    Slog.d(TAG, "FileSystem: mkdir called for Helium crash path: " + path + ", returning true");
                    return true;
                }

                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "FileSystem: mkdir failed, returning true", e);
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
                    Slog.d(TAG, "FileSystem: isDirectory called for Helium crash path: " + path + ", returning true");
                    return true;
                }

                // ✅ Fake root directories
                if (path.equals("/system/xbin") ||
                        path.equals("/system/bin") ||
                        path.equals("/su") ||
                        path.equals("/vendor/bin")) {

                    Slog.d(TAG, "FakeRoot: directory exists -> " + path);
                    return true;
                }

                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "FileSystem: isDirectory failed, returning false", e);
                return false;
            }
        }
    }

    // ✅ ADDED: Fake root su detection
    @ProxyMethod("exists")
    public static class Exists extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                File file = (File) who;
                String path = file.getAbsolutePath();

                // ✅ Fake root paths (extended list)
                if (path.equals("/system/xbin/su") ||
                        path.equals("/system/bin/su") ||
                        path.equals("/system/sbin/su") ||
                        path.equals("/vendor/bin/su") ||
                        path.equals("/su/bin/su")) {

                    Slog.d(TAG, "FakeRoot: su exists -> " + path);
                    return true;
                }

                // ✅ Fake busybox (some apps check this too)
                if (path.equals("/system/xbin/busybox") ||
                        path.equals("/system/bin/busybox")) {

                    Slog.d(TAG, "FakeRoot: busybox exists -> " + path);
                    return true;
                }

                // ✅ Hide real root artifacts
                if (path.contains("magisk") ||
                        path.contains("superuser") ||
                        path.contains("busybox")) {

                    Slog.d(TAG, "FakeRoot: hiding real root artifact -> " + path);
                    return false;
                }

                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "FileSystem: exists error", e);
                return false;
            }
        }
    }

    // ✅ EXTRA: canExecute() (used by some root checks)
    @ProxyMethod("canExecute")
    public static class CanExecute extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                File file = (File) who;
                String path = file.getAbsolutePath();

                if (path.contains("/su") || path.contains("busybox")) {
                    Slog.d(TAG, "FakeRoot: canExecute -> " + path);
                    return true;
                }

                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "FileSystem: canExecute error", e);
                return false;
            }
        }
    }
            }
