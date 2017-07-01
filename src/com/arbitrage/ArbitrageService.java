package com.arbitrage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by junyuanlau on 1/7/17.
 */
public class ArbitrageService {
    private static final Logger logger = LoggerFactory.getLogger(ArbitrageService.class);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws Exception {
        scheduler.scheduleAtFixedRate(new TaskRunnable(), 0, 60, TimeUnit.SECONDS);
    }

    public static class TaskRunnable implements Runnable {

        public void run() {
            logger.debug("Run task");
            CouponPullerTask task = new CouponPullerTask();
            try {
                task.handleData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void main(String args[]) {
            (new Thread(new TaskRunnable())).start();
        }

    }
}
