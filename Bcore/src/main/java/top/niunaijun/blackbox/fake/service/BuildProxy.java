package top.niunaijun.blackbox.fake.service;

import android.os.Build;

import java.lang.reflect.Field;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;

public class BuildProxy extends ClassInvocationStub {

    @Override
    protected Object getWho() {
        return Build.class;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        try {
            setField("MODEL", "Pixel 5");
            setField("DEVICE", "redfin");
            setField("BRAND", "google");
            setField("MANUFACTURER", "Google");
            setField("PRODUCT", "redfin");
            setField("FINGERPRINT", "google/redfin/redfin:13/TP1A.220624.014/1234567:user/release-keys");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setField(String name, String value) throws Exception {
        Field field = Build.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
