package top.niunaijun.blackbox.fake.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.lang.reflect.Method;
import java.util.List;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class ProcessBuilderProxy extends ClassInvocationStub {

    public static final String TAG = "ProcessBuilderProxy";

    @Override
    protected Object getWho() {
        return ProcessBuilder.class;
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
    // 🔥 Hook ProcessBuilder.start()
    // =========================================================
    @ProxyMethod("start")
    public static class Start extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                ProcessBuilder builder = (ProcessBuilder) who;
                List<String> command = builder.command();

                String cmd = String.join(" ", command);
                Slog.d(TAG, "ProcessBuilder.start intercepted: " + cmd);

                // ✅ FAKE ROOT
                if (cmd.contains("su")) {
                    Slog.d(TAG, "Fake SU via ProcessBuilder");
                    return new FakeProcess("uid=0(root) gid=0(root)\n");
                }

                if (cmd.contains("id")) {
                    return new FakeProcess("uid=0(root) gid=0(root)\n");
                }

                if (cmd.contains("which su")) {
                    return new FakeProcess("/system/xbin/su\n");
                }

            } catch (Throwable e) {
                Slog.e(TAG, "ProcessBuilder hook error", e);
            }

            return method.invoke(who, args);
        }
    }

    // =========================================================
    // 🔥 Fake Process
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
