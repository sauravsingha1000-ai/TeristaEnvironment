package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;

public class SystemPropertiesProxy extends ClassInvocationStub {

    @Override
    protected Object getWho() {
        try {
            return Class.forName("android.os.SystemProperties");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // no-op
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("get")
    public static class Get extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {

            String key = (String) args[0];

            if (key.equals("ro.product.model")) return "Pixel 5";
            if (key.equals("ro.product.device")) return "redfin";
            if (key.equals("ro.product.brand")) return "google";
            if (key.equals("ro.product.manufacturer")) return "Google";
            if (key.equals("ro.build.fingerprint"))
                return "google/redfin/redfin:13/TP1A.220624.014/1234567:user/release-keys";
            if (key.equals("ro.debuggable")) return "1";
            if (key.equals("ro.secure")) return "0";

            return method.invoke(who, args);
        }
    }
}
