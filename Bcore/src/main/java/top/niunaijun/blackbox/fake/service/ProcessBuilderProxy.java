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
    protected void inject(Object baseInvocation, Object proxyInvocation) {}

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("start")
    public static class Start extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                ProcessBuilder builder = (ProcessBuilder) who;
                List<String> command = builder.command();

                String cmd = String.join(" ", command);
                Slog.d(TAG, "ProcessBuilder: " + cmd);

                if (cmd.contains("which su")) {
                    return new FakeProcess("/system/xbin/su\n");
                }

                if (cmd.equals("su") || cmd.startsWith("su ")) {
                    return new FakeProcess("uid=0(root) gid=0(root)\n");
                }

                if (cmd.contains("id")) {
                    return new FakeProcess("uid=0(root) gid=0(root)\n");
                }

                if (cmd.contains("getprop")) {
                    return new FakeProcess("");
                }

            } catch (Throwable e) {
                Slog.e(TAG, "ProcessBuilder error", e);
            }

            return method.invoke(who, args);
        }
    }

    public static class FakeProcess extends Process {

        private final InputStream inputStream;

        public FakeProcess(String output) {
            this.inputStream = new ByteArrayInputStream(output.getBytes());
        }

        @Override
        public OutputStream getOutputStream() {
            return new OutputStream() {
                @Override
                public void write(int b) {}
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
        public void destroy() {}
    }
}
