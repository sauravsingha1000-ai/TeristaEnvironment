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
    protected void inject(Object baseInvocation, Object proxyInvocation) {}

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("get")
    public static class Get extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {

            String key = (String) args[0];

            if ("ro.debuggable".equals(key)) return "1";
            if ("ro.secure".equals(key)) return "0";
            if ("ro.build.tags".equals(key)) return "release-keys";

            if ("ro.product.model".equals(key)) return "Pixel 5";
            if ("ro.product.device".equals(key)) return "redfin";
            if ("ro.product.brand".equals(key)) return "google";

            return method.invoke(who, args);
        }
    }
}
