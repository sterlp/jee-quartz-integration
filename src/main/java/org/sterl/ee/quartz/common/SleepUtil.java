package org.sterl.ee.quartz.common;

public class SleepUtil {

    public static final InterruptedException sleep(long timeInMs) {
        InterruptedException result = null;
        if (timeInMs > 0) {
            try {
                Thread.sleep(timeInMs);
            } catch (InterruptedException ex) {
                result = ex;
            }
        }
        return result;
    }
}
