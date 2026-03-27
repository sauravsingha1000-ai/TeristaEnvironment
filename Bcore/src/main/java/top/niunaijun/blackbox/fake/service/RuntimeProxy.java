package top.niunaijun.blackbox.fake.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream; // ✅ FIXED (missing import)
import java.lang.Process;
import java.lang.Runtime;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class RuntimeProxy extends ClassInvocationStub {

    public static final String TAG = "RuntimeProxy";

    @Override
    protected Object getWho() {
        return Runtime.getRuntime();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // no-op
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // =========================================================
    // 🔥 CORE: Hook Runtime.exec()
    // =========================================================
    @ProxyMethod("exec")
    public static class Exec extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {

            try {
                if (args != null && args.length > 0) {

                    String cmd;

                    if (args[0] instanceof String[]) {
                        String[] cmds = (String[]) args[0];
                        cmd = String.join(" ", cmds);
                    } else {
                        cmd = String.valueOf(args[0]);
                    }

                    Slog.d(TAG, "Runtime.exec intercepted: " + cmd);

                    // =====================================================
                    // 🚀 FAKE ROOT: intercept "su"
                    // =====================================================
                    if (cmd.contains("su")) {
                        Slog.d(TAG, "Fake SU command executed");

                        return new FakeProcess(
                                "uid=0(root) gid=0(root)\n"
                        );
                    }

                    // Optional: handle id command
                    if (cmd.contains("id")) {
                        return new FakeProcess(
                                "uid=0(root) gid=0(root)\n"
                        );
                    }

                    // Optional: handle which su
                    if (cmd.contains("which su")) {
                        return new FakeProcess("/system/xbin/su\n");
                    }

                    // =====================================================
                    // 🚀 EXTRA: handle getprop (root checks)
                    // =====================================================
                    if (cmd.contains("getprop")) {
                        return new FakeProcess("");
                    }
                }

            } catch (Throwable e) {
                Slog.e(TAG, "Runtime exec hook error", e);
            }

            return method.invoke(who, args);
        }
    }

    // =========================================================
    // 🔥 Fake Process (simulate root shell)
    // =========================================================
    public static class FakeProcess extends Process {

        private final InputStream inputStream;

        public FakeProcess(String output) {
            this.inputStream = new ByteArrayInputStream(output.getBytes());
        }

        @Override
        public OutputStream getOutputStream() {
            return new OutputStream() {
                @Override
                public void write(int b) {
                    // ignore
                }
            };
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
            // no-op
        }
    }
}
