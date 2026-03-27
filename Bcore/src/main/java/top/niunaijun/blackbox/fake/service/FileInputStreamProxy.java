package top.niunaijun.blackbox.fake.service;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class FileInputStreamProxy extends ClassInvocationStub {

    public static final String TAG = "FileInputStreamProxy";

    @Override
    protected Object getWho() {
        return java.io.FileInputStream.class;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // no-op
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // 🔥 Hook constructor
    @ProxyMethod("<init>")
    public static class Constructor extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 0 && args[0] instanceof File) {

                    File file = (File) args[0];
                    String path = file.getAbsolutePath();

                    // 🔥 /proc/self/status
                    if (path.contains("/proc/self/status")) {
                        Slog.d(TAG, "Intercept /proc/self/status");

                        String fake =
                                "Name:\tapp_process\n" +
                                "State:\tR (running)\n" +
                                "Tgid:\t1234\n" +
                                "Pid:\t1234\n" +
                                "PPid:\t1\n" +
                                "Uid:\t0\t0\t0\t0\n" +
                                "Gid:\t0\t0\t0\t0\n";

                        File temp = File.createTempFile("fake_status", null);
                        FileOutputStream fos = new FileOutputStream(temp);
                        fos.write(fake.getBytes());
                        fos.close();

                        args[0] = temp;
                    }

                    // 🔥 /proc/self/cgroup
                    if (path.contains("/proc/self/cgroup")) {
                        Slog.d(TAG, "Intercept /proc/self/cgroup");

                        File temp = File.createTempFile("fake_cgroup", null);
                        FileOutputStream fos = new FileOutputStream(temp);
                        fos.write("0::/\n".getBytes());
                        fos.close();

                        args[0] = temp;
                    }
                }

                return method.invoke(who, args);

            } catch (Throwable e) {
                Slog.w(TAG, "FileInputStream hook error", e);
                return method.invoke(who, args);
            }
        }
    }
}
